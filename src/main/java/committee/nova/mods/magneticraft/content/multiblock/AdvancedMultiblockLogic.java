package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.Optional;

/**
 * Server-only behavior strategies for the sixteen advanced controllers.
 */
final class AdvancedMultiblockLogic {
    private static final int STEAM_PER_OPERATION = 10;
    private static final double STEAM_ENERGY_PER_OPERATION = 20.0D;
    private static final int PUMPJACK_RATE = 20;
    private static final int PUMPJACK_COST = 40;
    private static final int PUMPJACK_SEARCH_RADIUS = 3;
    private static final int PUMPJACK_SEARCH_BUDGET = 256;
    private static final int PUMPJACK_SEARCH_RETRY_TICKS = 100;
    private static final int COMBUSTION_WORK_PER_TICK = 40;
    private static final double SOLID_FUEL_POWER = 10.0D;
    private static final double COMBUSTION_MAX_TEMPERATURE = 2_273.15D;
    private static final int REFINERY_STEAM_PER_TICK = 20;

    private final AdvancedMultiblockBlockEntity machine;
    private BlockPos cachedOilDeposit;
    private BlockPos oilSearchOrigin;
    private int oilSearchColumn;
    private int oilSearchY = Integer.MIN_VALUE;
    private long oilSearchRetryAt;

    AdvancedMultiblockLogic(AdvancedMultiblockBlockEntity machine) {
        this.machine = machine;
    }

    void onFormed(ServerLevel level) {
        if (machine.definition() == MultiblockDefinition.SOLAR_TOWER) {
            bindNearbyMirrors(level);
        } else if (machine.definition() == MultiblockDefinition.SOLAR_MIRROR
                && !validSolarTower(level, machine.solarTowerPosition())) {
            machine.setSolarTowerPosition(findNearbyTower(level));
        }
    }

    void onUnformed() {
        if (machine.definition() == MultiblockDefinition.SOLAR_MIRROR) {
            machine.setSolarTowerPosition(null);
        }
    }

    void tick(ServerLevel level) {
        machine.setWorking(false);
        switch (machine.definition()) {
            case SOLAR_PANEL -> tickSolarPanel(level);
            case SOLAR_TOWER -> tickSolarTower();
            case STEAM_ENGINE -> tickSteamGenerator(120, 240.0D);
            case STEAM_TURBINE -> tickSteamGenerator(600, 1_200.0D);
            case GRINDER, SIEVE, HYDRAULIC_PRESS -> tickAdvancedProcessing(level);
            case BIG_ELECTRIC_FURNACE -> tickBigElectricFurnace(level);
            case BIG_COMBUSTION_CHAMBER -> tickBigCombustion(level);
            case BIG_STEAM_BOILER -> tickBigSteamBoiler();
            case OIL_HEATER -> tickOilHeater();
            case REFINERY -> tickRefinery();
            case PUMPJACK -> tickPumpjack(level);
            case SOLAR_MIRROR -> tickSolarMirror(level);
            default -> {
            }
        }
    }

    private void tickSolarPanel(ServerLevel level) {
        if (!level.isDay() || level.isThundering() || machine.electricity() == null) {
            return;
        }
        int visible = 0;
        int panels = 0;
        for (MultiblockCell cell : machine.definition().cells()) {
            if (cell.rule() != MultiblockRule.ELECTRIC && cell.rule() != MultiblockRule.CONTROLLER) {
                continue;
            }
            panels++;
            BlockPos position = MultiblockTransform.worldPosition(
                    machine.getBlockPos(),
                    cell.offset(),
                    machine.definition().center(),
                    machine.facing(),
                    machine.mirrored()
            );
            if (level.canSeeSky(position.above())) {
                visible++;
            }
        }
        if (visible > 0 && panels > 0) {
            double generated = 100.0D * visible / panels;
            if (machine.electricity().node().addEnergy(generated, false) > 0.0D) {
                machine.setWorking(true);
            }
        }
    }

    private void tickSolarTower() {
        if (machine.heat() == null || machine.heat().node().temperatureKelvin() <= 3_773.15D) {
            return;
        }
        double excessTemperature = machine.heat().node().temperatureKelvin() - 293.15D;
        machine.heat().node().removeHeat(excessTemperature * 0.25D, false);
        machine.markChanged();
    }

    private void tickSteamGenerator(int steamRate, double power) {
        if (machine.electricity() == null || machine.tank(0) == null) {
            return;
        }
        FluidStack steam = machine.tank(0).tank().getFluid();
        if (steam.getAmount() < STEAM_PER_OPERATION
                || steam.getFluid() != ModFluids.get(FluidDefinition.STEAM).source().get()) {
            return;
        }
        int availableOperations = Math.min(
                steam.getAmount() / STEAM_PER_OPERATION,
                steamRate / STEAM_PER_OPERATION
        );
        double requestedEnergy = Math.min(power, availableOperations * STEAM_ENERGY_PER_OPERATION);
        double acceptedEnergy = machine.electricity().node().addEnergy(requestedEnergy, true);
        int operations = Math.min(
                availableOperations,
                (int) Math.floor(acceptedEnergy / STEAM_ENERGY_PER_OPERATION)
        );
        if (operations <= 0) {
            return;
        }
        machine.tank(0).tank().drain(
                operations * STEAM_PER_OPERATION,
                IFluidHandler.FluidAction.EXECUTE
        );
        machine.electricity().node().addEnergy(
                operations * STEAM_ENERGY_PER_OPERATION,
                false
        );
        machine.setWorking(true);
    }

    private void tickAdvancedProcessing(ServerLevel level) {
        if (machine.inventory() == null || machine.energy() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        if (input.isEmpty()) {
            machine.resetProgress();
            return;
        }
        Optional<AdvancedProcessingRecipe> recipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.ADVANCED_PROCESSING_TYPE.get())
                .stream()
                .filter(candidate -> candidate.machine() == machine.definition())
                .filter(candidate -> candidate.input().test(input)
                        && input.getCount() >= candidate.inputCount())
                .filter(candidate -> candidate.pressMode() == null
                        || candidate.pressMode() == machine.hydraulicPressMode())
                .findFirst();
        if (recipe.isEmpty() || !outputsFit(recipe.get().results())) {
            machine.resetProgress();
            return;
        }
        advanceItemProcess(
                level,
                recipe.get().getId(),
                recipe.get().duration(),
                recipe.get().energyPerTick(),
                recipe.get().inputCount(),
                recipe.get().chanceResults()
        );
    }

    private void tickBigElectricFurnace(ServerLevel level) {
        if (machine.inventory() == null || machine.energy() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        if (input.isEmpty()) {
            machine.resetProgress();
            return;
        }
        SimpleContainer container = new SimpleContainer(input);
        Optional<? extends AbstractCookingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, container, level);
        if (recipe.isEmpty()) {
            machine.resetProgress();
            return;
        }
        ItemStack result = recipe.get().assemble(container, level.registryAccess());
        if (result.isEmpty() || !outputsFit(List.of(result))) {
            machine.resetProgress();
            return;
        }
        advanceItemProcess(
                level,
                recipe.get().getId(),
                Math.max(1, recipe.get().getCookingTime() / 10),
                200,
                1,
                List.of(new AdvancedProcessingRecipe.ChanceResult(result, 1.0F))
        );
    }

    private void advanceItemProcess(
            ServerLevel level,
            ResourceLocation recipeId,
            int duration,
            int cost,
            int inputCount,
            List<AdvancedProcessingRecipe.ChanceResult> outputs
    ) {
        if (!machine.recipeMatches(recipeId, duration)) {
            machine.startRecipe(recipeId, duration);
        }
        if (machine.energy().extractEnergy(cost, true) < cost) {
            return;
        }
        machine.energy().extractEnergy(cost, false);
        machine.advanceProgress();
        machine.setWorking(true);
        if (machine.progress() < duration) {
            return;
        }
        machine.inventory().extractInternal(0, inputCount, false);
        IItemHandlerModifiable handler = machine.inventory().menuHandler();
        for (int index = 0; index < outputs.size(); index++) {
            AdvancedProcessingRecipe.ChanceResult output = outputs.get(index);
            if (level.random.nextFloat() > output.chance()) {
                continue;
            }
            ItemStack remainder = handler.insertItem(index + 1, output.stack().copy(), false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("Simulated advanced-machine output no longer fits: " + recipeId);
            }
        }
        machine.resetProgress();
    }

    private boolean outputsFit(List<ItemStack> outputs) {
        if (machine.inventory() == null || outputs.size() + 1 > machine.inventory().slots()) {
            return false;
        }
        IItemHandlerModifiable handler = machine.inventory().menuHandler();
        for (int index = 0; index < outputs.size(); index++) {
            if (!handler.insertItem(index + 1, outputs.get(index).copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void tickBigCombustion(ServerLevel level) {
        if (machine.heat() == null || machine.tank(0) == null || machine.inventory() == null) {
            return;
        }
        if (machine.burnTicks() > 0) {
            if (machine.heat().node().temperatureKelvin() < COMBUSTION_MAX_TEMPERATURE) {
                int work = machine.consumeBurnWork(COMBUSTION_WORK_PER_TICK);
                machine.heat().node().addHeat(work * machine.burnPower(), false);
                machine.setWorking(work > 0);
            }
            return;
        }
        ItemStack solidFuel = machine.inventory().getStackInSlot(0);
        int burnTime = ForgeHooks.getBurnTime(solidFuel, RecipeType.SMELTING);
        if (burnTime > 0) {
            machine.inventory().extractInternal(0, 1, false);
            machine.startBurn(burnTime, SOLID_FUEL_POWER);
            return;
        }
        FluidStack fuel = machine.tank(0).tank().getFluid();
        Optional<FluidFuelRecipe> fluidRecipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get())
                .stream()
                .filter(candidate -> !fuel.isEmpty() && candidate.fluid() == fuel.getFluid())
                .findFirst();
        if (fluidRecipe.isPresent() && fuel.getAmount() > 0) {
            int amount = Math.min(100, fuel.getAmount());
            int burnWork = Math.round(amount * fluidRecipe.get().durationTicks() / 1_000.0F);
            if (burnWork > 0) {
                machine.tank(0).tank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
                machine.startBurn(burnWork, fluidRecipe.get().powerPerTick());
            }
            return;
        }
        double excessHeat = Math.max(
                0.0D,
                (machine.heat().node().temperatureKelvin() - 293.15D)
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        if (excessHeat > 0.0D) {
            machine.heat().node().removeHeat(Math.min(10.0D, excessHeat), false);
            machine.markChanged();
        }
    }

    private void tickBigSteamBoiler() {
        if (machine.heat() == null || machine.tank(0) == null || machine.tank(1) == null
                || machine.heat().node().temperatureKelvin() < 373.15D) {
            return;
        }
        FluidStack water = machine.tank(0).tank().getFluid();
        if (water.isEmpty() || !water.getFluid().defaultFluidState().is(FluidTags.WATER)) {
            return;
        }
        double excessHeat = Math.max(
                0.0D,
                (machine.heat().node().temperatureKelvin() - 373.15D)
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        int waterAmount = Math.min(60, water.getAmount());
        waterAmount = Math.min(waterAmount, (int) Math.floor(excessHeat / 20.0D));
        FluidStack steam = new FluidStack(
                ModFluids.get(FluidDefinition.STEAM).source().get(), waterAmount * 10
        );
        int acceptedSteam = machine.tank(1).tank().fill(steam, IFluidHandler.FluidAction.SIMULATE);
        int acceptedWater = acceptedSteam / 10;
        int producedSteam = acceptedWater * 10;
        double heatCost = producedSteam * 2.0D;
        if (acceptedWater <= 0 || machine.heat().node().removeHeat(heatCost, true) < heatCost) {
            return;
        }
        machine.tank(0).tank().drain(acceptedWater, IFluidHandler.FluidAction.EXECUTE);
        machine.tank(1).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), producedSteam),
                IFluidHandler.FluidAction.EXECUTE
        );
        machine.heat().node().removeHeat(heatCost, false);
        machine.setWorking(true);
    }

    private void tickOilHeater() {
        if (machine.heat() == null || machine.tank(0) == null || machine.tank(1) == null) {
            return;
        }
        FluidStack input = machine.tank(0).tank().getFluid();
        OilHeaterBatch batch = oilHeaterBatch(input);
        if (batch == null
                || input.getAmount() < batch.inputAmount()
                || machine.heat().node().temperatureKelvin() < batch.minimumTemperature()
                || machine.tank(1).tank().fill(batch.output(), IFluidHandler.FluidAction.SIMULATE)
                < batch.output().getAmount()) {
            machine.resetProgress();
            return;
        }
        int totalWork = batch.duration() * 120;
        if (!machine.recipeMatches(batch.id(), totalWork)) {
            machine.startRecipe(batch.id(), totalWork);
        }
        int work = (int) Math.floor(Math.min(
                1.0D,
                Math.max(0.0D, machine.heat().node().temperatureKelvin() - batch.minimumTemperature())
        ) * 120.0D);
        double excessHeat = Math.max(
                0.0D,
                (machine.heat().node().temperatureKelvin() - batch.minimumTemperature())
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        if (work <= 0 || excessHeat < work) {
            return;
        }
        machine.heat().node().removeHeat(work, false);
        machine.advanceProgress(work);
        machine.setWorking(true);
        if (machine.progress() >= totalWork) {
            machine.tank(0).tank().drain(batch.inputAmount(), IFluidHandler.FluidAction.EXECUTE);
            machine.tank(1).tank().fill(batch.output(), IFluidHandler.FluidAction.EXECUTE);
            machine.resetProgress();
        }
    }

    private void tickRefinery() {
        if (machine.tank(0) == null || machine.tank(1) == null) {
            return;
        }
        FluidStack input = machine.tank(0).tank().getFluid();
        FluidStack processSteam = machine.tank(1).tank().getFluid();
        RefineryBatch batch = refineryBatch(input);
        int steamWork = Math.min(
                REFINERY_STEAM_PER_TICK,
                (int) Math.floor((double) REFINERY_STEAM_PER_TICK
                        * processSteam.getAmount()
                        / machine.tank(1).tank().getCapacity())
        );
        if (batch == null
                || input.getAmount() < batch.inputAmount()
                || steamWork <= 0
                || processSteam.getFluid() != ModFluids.get(FluidDefinition.STEAM).source().get()
                || !refineryOutputsFit(batch.outputs())) {
            machine.resetProgress();
            return;
        }
        int totalWork = batch.duration() * REFINERY_STEAM_PER_TICK;
        if (!machine.recipeMatches(batch.id(), totalWork)) {
            machine.startRecipe(batch.id(), totalWork);
        }
        machine.tank(1).tank().drain(steamWork, IFluidHandler.FluidAction.EXECUTE);
        machine.advanceProgress(steamWork);
        machine.setWorking(true);
        if (machine.progress() >= totalWork) {
            machine.tank(0).tank().drain(batch.inputAmount(), IFluidHandler.FluidAction.EXECUTE);
            for (int index = 0; index < batch.outputs().size(); index++) {
                FluidStack output = batch.outputs().get(index);
                if (!output.isEmpty()) {
                    machine.tank(index + 2).tank().fill(output, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            machine.resetProgress();
        }
    }

    private boolean refineryOutputsFit(List<FluidStack> outputs) {
        for (int index = 0; index < outputs.size(); index++) {
            FluidStack output = outputs.get(index);
            if (!output.isEmpty()
                    && machine.tank(index + 2).tank().fill(output, IFluidHandler.FluidAction.SIMULATE)
                    < output.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private OilHeaterBatch oilHeaterBatch(FluidStack input) {
        if (input.getFluid().defaultFluidState().is(FluidTags.WATER)) {
            return new OilHeaterBatch(
                    Magneticraft.id("oil_heater/water_to_steam"),
                    1,
                    new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 10),
                    1,
                    373.15D
            );
        }
        if (input.getFluid() == ModFluids.get(FluidDefinition.OIL).source().get()) {
            return new OilHeaterBatch(
                    Magneticraft.id("oil_heater/crude_oil_to_heated_crude_oil"),
                    10,
                    new FluidStack(ModFluids.get(FluidDefinition.HOT_CRUDE).source().get(), 100),
                    2,
                    623.15D
            );
        }
        return null;
    }

    private RefineryBatch refineryBatch(FluidStack input) {
        if (input.getFluid() == ModFluids.get(FluidDefinition.STEAM).source().get()) {
            return refineryBatch("steam_to_water", 10, 2,
                    new FluidStack(Fluids.WATER, 1), FluidStack.EMPTY, FluidStack.EMPTY);
        }
        if (input.getFluid() == ModFluids.get(FluidDefinition.HOT_CRUDE).source().get()) {
            return refineryBatch("heated_crude_oil_fractionation", 100, 1,
                    fluid(FluidDefinition.HEAVY_OIL, 4),
                    fluid(FluidDefinition.LIGHT_OIL, 3),
                    fluid(FluidDefinition.LPG, 3));
        }
        if (input.getFluid() == ModFluids.get(FluidDefinition.HEAVY_OIL).source().get()) {
            return refineryBatch("heavy_oil_fractionation", 10, 1,
                    fluid(FluidDefinition.OIL_RESIDUE, 4),
                    fluid(FluidDefinition.FUEL, 5),
                    fluid(FluidDefinition.LUBRICANT, 1));
        }
        if (input.getFluid() == ModFluids.get(FluidDefinition.LIGHT_OIL).source().get()) {
            return refineryBatch("light_oil_fractionation", 10, 1,
                    fluid(FluidDefinition.DIESEL, 5),
                    fluid(FluidDefinition.KEROSENE, 2),
                    fluid(FluidDefinition.GASOLINE, 3));
        }
        if (input.getFluid() == ModFluids.get(FluidDefinition.LPG).source().get()) {
            return refineryBatch("lpg_fractionation", 10, 1,
                    fluid(FluidDefinition.PLASTIC, 5),
                    fluid(FluidDefinition.NAPHTHA, 2),
                    fluid(FluidDefinition.NATURAL_GAS, 3));
        }
        return null;
    }

    private RefineryBatch refineryBatch(
            String id,
            int inputAmount,
            int duration,
            FluidStack output0,
            FluidStack output1,
            FluidStack output2
    ) {
        return new RefineryBatch(
                Magneticraft.id("refinery/" + id),
                inputAmount,
                List.of(output0, output1, output2),
                duration
        );
    }

    private FluidStack fluid(FluidDefinition definition, int amount) {
        return new FluidStack(ModFluids.get(definition).source().get(), amount);
    }

    private void tickPumpjack(ServerLevel level) {
        if (machine.energy() == null || machine.tank(0) == null
                || machine.energy().extractEnergy(PUMPJACK_COST, true) < PUMPJACK_COST) {
            return;
        }
        FluidStack oil = new FluidStack(ModFluids.get(FluidDefinition.OIL).source().get(), PUMPJACK_RATE);
        int accepted = machine.tank(0).tank().fill(oil, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        OilDepositBlockEntity deposit = findOilDeposit(level);
        if (deposit == null) {
            return;
        }
        int drained = deposit.drain(accepted, true);
        if (drained <= 0) {
            return;
        }
        machine.energy().extractEnergy(PUMPJACK_COST, false);
        deposit.drain(drained, false);
        machine.tank(0).tank().fill(new FluidStack(oil, drained), IFluidHandler.FluidAction.EXECUTE);
        machine.setWorking(true);
    }

    private OilDepositBlockEntity findOilDeposit(ServerLevel level) {
        if (cachedOilDeposit != null) {
            if (!level.hasChunk(cachedOilDeposit.getX() >> 4, cachedOilDeposit.getZ() >> 4)) {
                return null;
            }
            if (level.getBlockEntity(cachedOilDeposit) instanceof OilDepositBlockEntity deposit
                    && deposit.remaining() > 0) {
                return deposit;
            }
            cachedOilDeposit = null;
            resetOilSearch();
        }

        BlockPos origin = machine.getBlockPos().relative(machine.facing(), 5);
        if (!origin.equals(oilSearchOrigin)) {
            oilSearchOrigin = origin;
            resetOilSearch();
        }
        int diameter = PUMPJACK_SEARCH_RADIUS * 2 + 1;
        int columnCount = diameter * diameter;
        if (oilSearchColumn >= columnCount) {
            if (level.getGameTime() < oilSearchRetryAt) {
                return null;
            }
            resetOilSearch();
        }

        int budget = PUMPJACK_SEARCH_BUDGET;
        while (budget-- > 0 && oilSearchColumn < columnCount) {
            int offsetX = oilSearchColumn % diameter - PUMPJACK_SEARCH_RADIUS;
            int offsetZ = oilSearchColumn / diameter - PUMPJACK_SEARCH_RADIUS;
            int x = origin.getX() + offsetX;
            int z = origin.getZ() + offsetZ;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                advanceOilSearchColumn(origin.getY());
                continue;
            }
            if (oilSearchY == Integer.MIN_VALUE) {
                oilSearchY = origin.getY();
            }
            if (oilSearchY < level.getMinBuildHeight()) {
                advanceOilSearchColumn(origin.getY());
                continue;
            }
            BlockPos position = new BlockPos(x, oilSearchY--, z);
            if (level.getBlockEntity(position) instanceof OilDepositBlockEntity deposit && deposit.remaining() > 0) {
                cachedOilDeposit = position;
                return deposit;
            }
        }
        if (oilSearchColumn >= columnCount) {
            oilSearchRetryAt = level.getGameTime() + PUMPJACK_SEARCH_RETRY_TICKS;
        }
        return null;
    }

    private void advanceOilSearchColumn(int startY) {
        oilSearchColumn++;
        oilSearchY = startY;
    }

    private void resetOilSearch() {
        oilSearchColumn = 0;
        oilSearchY = Integer.MIN_VALUE;
        oilSearchRetryAt = 0L;
    }

    private void tickSolarMirror(ServerLevel level) {
        if (!level.isDay() || level.isThundering() || level.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos origin = machine.getBlockPos();
        if (!level.canSeeSky(origin.above(3))) {
            return;
        }
        BlockPos target = machine.solarTowerPosition();
        if (target == null || !level.hasChunk(target.getX() >> 4, target.getZ() >> 4)) {
            return;
        }
        if (!(level.getBlockEntity(target) instanceof AdvancedMultiblockBlockEntity tower)
                || !tower.operational()
                || tower.definition() != MultiblockDefinition.SOLAR_TOWER
                || tower.heat() == null
                || Math.abs(target.getX() - origin.getX()) > 25
                || target.getY() - origin.getY() < 0
                || target.getY() - origin.getY() > 20
                || Math.abs(target.getZ() - origin.getZ()) > 25) {
            machine.setSolarTowerPosition(null);
            return;
        }
        tower.heat().node().addHeat(16.0D * 20.0D, false);
        tower.markChanged();
        machine.setWorking(true);
    }

    private void bindNearbyMirrors(ServerLevel level) {
        BlockPos tower = machine.getBlockPos();
        for (int y = 0; y <= 20; y++) {
            for (int x = -25; x <= 25; x++) {
                for (int z = -25; z <= 25; z++) {
                    BlockPos candidate = tower.offset(x, -y, z);
                    if (!level.hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) {
                        continue;
                    }
                    if (level.getBlockEntity(candidate) instanceof AdvancedMultiblockBlockEntity mirror
                            && mirror.operational()
                            && mirror.definition() == MultiblockDefinition.SOLAR_MIRROR) {
                        mirror.setSolarTowerPosition(tower);
                    }
                }
            }
        }
    }

    private BlockPos findNearbyTower(ServerLevel level) {
        BlockPos mirror = machine.getBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int y = 0; y <= 20; y++) {
            for (int x = -25; x <= 25; x++) {
                for (int z = -25; z <= 25; z++) {
                    BlockPos candidate = mirror.offset(x, y, z);
                    if (!level.hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)
                            || !validSolarTower(level, candidate)) {
                        continue;
                    }
                    double distance = candidate.distSqr(mirror);
                    if (distance < nearestDistance) {
                        nearest = candidate.immutable();
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean validSolarTower(ServerLevel level, BlockPos position) {
        return position != null
                && level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
                && level.getBlockEntity(position) instanceof AdvancedMultiblockBlockEntity tower
                && tower.operational()
                && tower.definition() == MultiblockDefinition.SOLAR_TOWER;
    }

    private record OilHeaterBatch(
            ResourceLocation id,
            int inputAmount,
            FluidStack output,
            int duration,
            double minimumTemperature
    ) {
    }

    private record RefineryBatch(
            ResourceLocation id,
            int inputAmount,
            List<FluidStack> outputs,
            int duration
    ) {
    }
}
