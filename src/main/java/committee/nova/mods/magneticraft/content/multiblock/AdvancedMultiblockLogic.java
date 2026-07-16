package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.system.network.heat.HeatNode;

import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.PolymerizerRecipe;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private enum DrillStatus {
        INTACT,
        MISSING,
        UNLOADED
    }

    private static final int STEAM_PER_OPERATION = 10;
    private static final double STEAM_ENERGY_PER_OPERATION = 20.0D;
    private static final String PUMPJACK_TAG = "pumpjack";
    private static final int PUMPJACK_BATCH_MILLIBUCKETS =
            OilDepositBlockEntity.EXTRACTION_STAGE_MILLIBUCKETS;
    private static final int PUMPJACK_COST = 80;
    private static final int PUMPJACK_INITIAL_SEARCH_BUDGET = 81;
    private static final int PUMPJACK_FIELD_SEARCH_BUDGET = 640;
    private static final int PUMPJACK_DRILL_SKIP_BUDGET = 5;
    private static final int PUMPJACK_PIPE_CHECK_INTERVAL = 80;
    private static final int COMBUSTION_WORK_PER_TICK = 40;
    private static final double SOLID_FUEL_POWER = 10.0D;
    private static final double COMBUSTION_MAX_TEMPERATURE = 2_273.15D;
    private static final int REFINERY_STEAM_PER_TICK = 20;

    private final AdvancedMultiblockBlockEntity machine;
    private PumpjackState pumpjackState = new PumpjackState();

    AdvancedMultiblockLogic(AdvancedMultiblockBlockEntity machine) {
        this.machine = machine;
    }

    void savePersistentState(CompoundTag tag) {
        if (machine.definition() == MultiblockDefinition.PUMPJACK) {
            tag.put(PUMPJACK_TAG, pumpjackState.save());
        }
    }

    void loadPersistentState(CompoundTag tag) {
        pumpjackState = machine.definition() == MultiblockDefinition.PUMPJACK
                && tag.contains(PUMPJACK_TAG, Tag.TAG_COMPOUND)
                ? PumpjackState.load(tag.getCompound(PUMPJACK_TAG))
                : new PumpjackState();
    }

    void stripPortableState(CompoundTag tag) {
        tag.remove(PUMPJACK_TAG);
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
            case OIL_HEATER -> tickOilHeater(level);
            case POLYMERIZER -> tickPolymerizer(level);
            case REFINERY -> tickRefinery(level);
            case PUMPJACK -> tickPumpjack(level);
            case SOLAR_MIRROR -> tickSolarMirror(level);
            default -> {
            }
        }
    }

    private void tickSolarPanel(ServerLevel level) {
        if (!level.isDay() || machine.energy() == null) {
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
            if (machine.energy().generateJoules(generated, false) > 0.0D) {
                machine.setWorking(true);
            }
        }
    }

    private void tickSolarTower() {
        if (machine.heat() == null || machine.heat().node().temperatureKelvin() <= 3_773.15D) {
            return;
        }
        double excessTemperature = machine.heat().node().temperatureKelvin()
                - HeatNode.AMBIENT_TEMPERATURE_KELVIN;
        machine.heat().node().removeHeat(excessTemperature * 0.25D, false);
        machine.markChanged();
    }

    private void tickSteamGenerator(int steamRate, double power) {
        if (machine.energy() == null || machine.tank(0) == null) {
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
        double acceptedEnergy = machine.energy().generateJoules(requestedEnergy, true);
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
        machine.energy().generateJoules(operations * STEAM_ENERGY_PER_OPERATION, false);
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
                .getAllRecipesFor(ModRecipeTypes.advancedProcessingType(machine.definition()).get())
                .stream()
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
        if (machine.energy().consumeJoules(cost, true) < cost) {
            return;
        }
        machine.energy().consumeJoules(cost, false);
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
                (machine.heat().node().temperatureKelvin() - HeatNode.AMBIENT_TEMPERATURE_KELVIN)
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

    private void tickOilHeater(ServerLevel level) {
        if (machine.heat() == null || machine.tank(0) == null || machine.tank(1) == null) {
            return;
        }
        FluidStack input = machine.tank(0).tank().getFluid();
        AdvancedProcessingRecipe recipe = fluidRecipe(level, MultiblockDefinition.OIL_HEATER, input);
        if (recipe == null) {
            machine.resetProgress();
            return;
        }
        FluidStack output = recipe.fluidOutputs().get(0).stack();
        if (machine.heat().node().temperatureKelvin() < recipe.minimumTemperatureKelvin()
                || machine.tank(1).tank().fill(output, IFluidHandler.FluidAction.SIMULATE) < output.getAmount()) {
            machine.resetProgress();
            return;
        }
        int totalWork = recipe.duration() * 120;
        if (!machine.recipeMatches(recipe.getId(), totalWork)) {
            machine.startRecipe(recipe.getId(), totalWork);
        }
        int work = (int) Math.floor(Math.min(
                1.0D,
                Math.max(0.0D, machine.heat().node().temperatureKelvin() - recipe.minimumTemperatureKelvin())
        ) * 120.0D);
        double excessHeat = Math.max(
                0.0D,
                (machine.heat().node().temperatureKelvin() - recipe.minimumTemperatureKelvin())
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        if (work <= 0 || excessHeat < work) {
            return;
        }
        machine.heat().node().removeHeat(work, false);
        machine.advanceProgress(work);
        machine.setWorking(true);
        if (machine.progress() >= totalWork) {
            machine.tank(0).tank().drain(recipe.fluidInputAmount(), IFluidHandler.FluidAction.EXECUTE);
            machine.tank(1).tank().fill(output, IFluidHandler.FluidAction.EXECUTE);
            machine.resetProgress();
        }
    }

    private void tickPolymerizer(ServerLevel level) {
        if (machine.heat() == null || machine.tank(0) == null || machine.inventory() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        FluidStack fluid = machine.tank(0).tank().getFluid();
        PolymerizerRecipe recipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.POLYMERIZING_TYPE.get())
                .stream()
                .filter(candidate -> candidate.matches(input, fluid))
                .findFirst()
                .orElse(null);
        if (recipe == null || !outputsFit(List.of(recipe.result()))) {
            return;
        }
        if (machine.heat().node().temperatureKelvin() < recipe.minimumTemperatureKelvin()
                || machine.heat().node().removeHeat(recipe.heatPerTick(), true) < recipe.heatPerTick()) {
            return;
        }
        if (!machine.recipeMatches(recipe.getId(), recipe.duration())) {
            machine.startRecipe(recipe.getId(), recipe.duration());
        }
        machine.heat().node().removeHeat(recipe.heatPerTick(), false);
        machine.advanceProgress();
        machine.setWorking(true);
        if (machine.progress() < recipe.duration()) {
            return;
        }

        machine.tank(0).tank().drain(recipe.fluidInput().getAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (recipe.ingredient().isPresent()) {
            machine.inventory().extractInternal(0, 1, false);
        }
        ItemStack remainder = machine.inventory().menuHandler().insertItem(1, recipe.result(), false);
        if (!remainder.isEmpty()) {
            throw new IllegalStateException("Simulated polymerizer output no longer fits: " + recipe.getId());
        }
        machine.resetProgress();
    }

    private void tickRefinery(ServerLevel level) {
        if (machine.tank(0) == null || machine.tank(1) == null) {
            return;
        }
        FluidStack input = machine.tank(0).tank().getFluid();
        FluidStack processSteam = machine.tank(1).tank().getFluid();
        AdvancedProcessingRecipe recipe = fluidRecipe(level, MultiblockDefinition.REFINERY, input);
        int steamWork = Math.min(
                REFINERY_STEAM_PER_TICK,
                (int) Math.floor((double) REFINERY_STEAM_PER_TICK
                        * processSteam.getAmount()
                        / machine.tank(1).tank().getCapacity())
        );
        if (recipe == null
                || steamWork <= 0
                || processSteam.getFluid() != ModFluids.get(FluidDefinition.STEAM).source().get()
                || !refineryOutputsFit(recipe.fluidOutputs())) {
            machine.resetProgress();
            return;
        }
        int totalWork = recipe.duration() * REFINERY_STEAM_PER_TICK;
        if (!machine.recipeMatches(recipe.getId(), totalWork)) {
            machine.startRecipe(recipe.getId(), totalWork);
        }
        machine.tank(1).tank().drain(steamWork, IFluidHandler.FluidAction.EXECUTE);
        machine.advanceProgress(steamWork);
        machine.setWorking(true);
        if (machine.progress() >= totalWork) {
            machine.tank(0).tank().drain(recipe.fluidInputAmount(), IFluidHandler.FluidAction.EXECUTE);
            for (AdvancedProcessingRecipe.FluidOutput output : recipe.fluidOutputs()) {
                machine.tank(output.tank() + 2).tank().fill(output.stack(), IFluidHandler.FluidAction.EXECUTE);
            }
            machine.resetProgress();
        }
    }

    private boolean refineryOutputsFit(List<AdvancedProcessingRecipe.FluidOutput> outputs) {
        for (AdvancedProcessingRecipe.FluidOutput output : outputs) {
            FluidStack stack = output.stack();
            if (machine.tank(output.tank() + 2).tank().fill(stack, IFluidHandler.FluidAction.SIMULATE)
                    < stack.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private AdvancedProcessingRecipe fluidRecipe(
            ServerLevel level,
            MultiblockDefinition definition,
            FluidStack input
    ) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.advancedProcessingType(definition).get()).stream()
                .filter(AdvancedProcessingRecipe::isFluidProcessing)
                .filter(recipe -> recipe.matchesFluid(input))
                .findFirst()
                .orElse(null);
    }

    private void tickPumpjack(ServerLevel level) {
        if (machine.energy() == null || machine.tank(0) == null) {
            return;
        }
        switch (pumpjackState.phase()) {
            case SEARCHING_OIL -> tickPumpjackOilSearch(level);
            case SEARCHING_DEPOSIT -> tickPumpjackDepositScan(level);
            case DIGGING -> tickPumpjackDigging(level);
            case SEARCHING_SOURCE -> tickPumpjackSourceSearch(level);
            case EXTRACTING -> tickPumpjackExtraction(level);
        }
    }

    private void tickPumpjackOilSearch(ServerLevel level) {
        BlockPos origin = drillHead().below();
        if (origin.getY() < level.getMinBuildHeight()) {
            return;
        }
        PumpjackCursor cursor = PumpjackCursor.searchingOil(origin, level.getMinBuildHeight());
        if (!validatePumpjackCursor(cursor)) {
            return;
        }
        int index = pumpjackState.cursorIndex();
        int scanned = 0;
        while (scanned < PUMPJACK_INITIAL_SEARCH_BUDGET && index < cursor.totalPositions()) {
            BlockPos position = cursor.positionAt(index);
            index++;
            scanned++;
            if (!loaded(level, position)) {
                continue;
            }
            if (level.getBlockEntity(position) instanceof OilDepositBlockEntity) {
                pumpjackState.setDepositOrigin(position);
                pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_DEPOSIT);
                machine.markChanged();
                return;
            }
        }
        if (index != pumpjackState.cursorIndex()) {
            pumpjackState.setCursorIndex(index);
            machine.markChanged();
        }
        if (index >= cursor.totalPositions()) {
            pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_OIL);
            machine.markChanged();
        }
    }

    private void tickPumpjackDepositScan(ServerLevel level) {
        BlockPos depositOrigin = pumpjackState.depositOrigin().orElse(null);
        if (depositOrigin == null || machine.energy().consumeJoules(PUMPJACK_COST, true) < PUMPJACK_COST) {
            return;
        }
        PumpjackCursor cursor = PumpjackCursor.depositScan(
                depositOrigin,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight()
        );
        if (!validatePumpjackCursor(cursor)) {
            return;
        }
        int index = pumpjackState.cursorIndex();
        int depositSize = pumpjackState.depositSize();
        int remainingSources = pumpjackState.depositRemainingSources();
        int scanned = 0;
        while (scanned < PUMPJACK_FIELD_SEARCH_BUDGET && index < cursor.totalPositions()) {
            BlockPos position = cursor.positionAt(index);
            index++;
            scanned++;
            if (!loaded(level, position)) {
                continue;
            }
            if (level.getBlockEntity(position) instanceof OilDepositBlockEntity deposit) {
                depositSize++;
                if (deposit.remaining() >= PUMPJACK_BATCH_MILLIBUCKETS) {
                    remainingSources++;
                }
            }
        }
        if (scanned <= 0) {
            return;
        }
        machine.energy().consumeJoules(PUMPJACK_COST, false);
        machine.setWorking(true);
        pumpjackState.setCursorIndex(index);
        pumpjackState.setDepositCounts(depositSize, remainingSources);
        if (index >= cursor.totalPositions()) {
            pumpjackState.transitionTo(depositSize > 0
                    ? PumpjackState.Phase.DIGGING
                    : PumpjackState.Phase.SEARCHING_OIL);
        }
        machine.markChanged();
    }

    private void tickPumpjackDigging(ServerLevel level) {
        BlockPos depositOrigin = pumpjackState.depositOrigin().orElse(null);
        if (depositOrigin == null || depositOrigin.getY() > drillHead().getY()) {
            resetPumpjack();
            return;
        }
        PumpjackCursor cursor = PumpjackCursor.digging(drillHead(), depositOrigin.getY());
        if (!validatePumpjackCursor(cursor)) {
            return;
        }
        int index = pumpjackState.cursorIndex();
        if (index >= cursor.totalPositions()) {
            pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
            machine.markChanged();
            return;
        }
        BlockPos current = cursor.positionAt(index);
        if (!loaded(level, current)
                || machine.energy().consumeJoules(PUMPJACK_COST, true) < PUMPJACK_COST) {
            return;
        }
        boolean operate = pumpjackState.advancePhaseTick();
        machine.markChanged();
        if (!operate) {
            return;
        }

        int inspected = 0;
        while (inspected < PUMPJACK_DRILL_SKIP_BUDGET && index < cursor.totalPositions()) {
            BlockPos position = cursor.positionAt(index);
            if (!loaded(level, position)) {
                return;
            }
            BlockState state = level.getBlockState(position);
            if (state.is(committee.nova.mods.magneticraft.init.ModAdvancedBlocks.PUMPJACK_DRILL.get())) {
                index++;
                inspected++;
                continue;
            }
            if (state.getDestroySpeed(level, position) < 0.0F) {
                pumpjackState.setCursorIndex(index);
                machine.markChanged();
                return;
            }
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(position) : null;
            List<ItemStack> drops = state.isAir()
                    ? List.of()
                    : Block.getDrops(state, level, position, blockEntity);
            if (!level.setBlock(
                    position,
                    committee.nova.mods.magneticraft.init.ModAdvancedBlocks.PUMPJACK_DRILL.get().defaultBlockState(),
                    Block.UPDATE_ALL
            )) {
                return;
            }
            if (!state.isAir()) {
                level.levelEvent(2001, position, Block.getId(state));
                drops.forEach(stack -> Block.popResource(level, drillHead().above(), stack));
            }
            machine.energy().consumeJoules(PUMPJACK_COST, false);
            machine.setWorking(true);
            index++;
            pumpjackState.setCursorIndex(index);
            if (index >= cursor.totalPositions()) {
                pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
            }
            machine.markChanged();
            return;
        }
        pumpjackState.setCursorIndex(index);
        if (index >= cursor.totalPositions()) {
            pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
        }
        machine.markChanged();
    }

    private void tickPumpjackSourceSearch(ServerLevel level) {
        BlockPos depositOrigin = pumpjackState.depositOrigin().orElse(null);
        if (depositOrigin == null) {
            resetPumpjack();
            return;
        }
        if (pumpjackState.depositRemainingSources() <= 0) {
            pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_DEPOSIT);
            machine.markChanged();
            return;
        }
        if (level.getGameTime() % PUMPJACK_PIPE_CHECK_INTERVAL == 0L) {
            DrillStatus drillStatus = drillStatus(level, depositOrigin.getY());
            if (drillStatus == DrillStatus.UNLOADED) {
                return;
            }
            if (drillStatus == DrillStatus.MISSING) {
                pumpjackState.transitionTo(PumpjackState.Phase.DIGGING);
                machine.markChanged();
                return;
            }
        }
        PumpjackCursor cursor = PumpjackCursor.sourceScan(
                drillHead().below(),
                depositOrigin,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight()
        );
        if (!validatePumpjackCursor(cursor)) {
            return;
        }
        int index = pumpjackState.cursorIndex();
        int scanned = 0;
        while (scanned < PUMPJACK_FIELD_SEARCH_BUDGET && index < cursor.totalPositions()) {
            BlockPos position = cursor.positionAt(index);
            index++;
            scanned++;
            if (!loaded(level, position)) {
                continue;
            }
            if (level.getBlockEntity(position) instanceof OilDepositBlockEntity deposit
                    && deposit.remaining() >= PUMPJACK_BATCH_MILLIBUCKETS) {
                pumpjackState.setTargetSource(position);
                pumpjackState.transitionTo(PumpjackState.Phase.EXTRACTING);
                machine.markChanged();
                return;
            }
        }
        if (index != pumpjackState.cursorIndex()) {
            pumpjackState.setCursorIndex(index);
            machine.markChanged();
        }
        if (index >= cursor.totalPositions()) {
            pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_DEPOSIT);
            machine.markChanged();
        }
    }

    private void tickPumpjackExtraction(ServerLevel level) {
        BlockPos target = pumpjackState.targetSource().orElse(null);
        BlockPos depositOrigin = pumpjackState.depositOrigin().orElse(null);
        if (target == null || depositOrigin == null) {
            resetPumpjack();
            return;
        }
        if (!loaded(level, target)) {
            return;
        }
        if (level.getGameTime() % PUMPJACK_PIPE_CHECK_INTERVAL == 0L) {
            DrillStatus drillStatus = drillStatus(level, depositOrigin.getY());
            if (drillStatus == DrillStatus.UNLOADED) {
                return;
            }
            if (drillStatus == DrillStatus.MISSING) {
                pumpjackState.transitionTo(PumpjackState.Phase.DIGGING);
                machine.markChanged();
                return;
            }
        }
        if (!(level.getBlockEntity(target) instanceof OilDepositBlockEntity deposit)
                || deposit.remaining() < PUMPJACK_BATCH_MILLIBUCKETS) {
            sourceUnavailable();
            return;
        }
        if (machine.energy().consumeJoules(PUMPJACK_COST, true) < PUMPJACK_COST) {
            return;
        }
        machine.energy().consumeJoules(PUMPJACK_COST, false);
        machine.setWorking(true);
        boolean produce = pumpjackState.advancePhaseTick();
        machine.markChanged();
        if (!produce) {
            return;
        }
        FluidStack oil = new FluidStack(
                ModFluids.get(FluidDefinition.OIL).source().get(),
                PUMPJACK_BATCH_MILLIBUCKETS
        );
        if (machine.tank(0).tank().fill(oil, IFluidHandler.FluidAction.SIMULATE)
                != PUMPJACK_BATCH_MILLIBUCKETS) {
            return;
        }
        if (deposit.drain(PUMPJACK_BATCH_MILLIBUCKETS, true) != PUMPJACK_BATCH_MILLIBUCKETS) {
            sourceUnavailable();
            return;
        }
        deposit.drain(PUMPJACK_BATCH_MILLIBUCKETS, false);
        machine.tank(0).tank().fill(oil, IFluidHandler.FluidAction.EXECUTE);
        if (deposit.remaining() < PUMPJACK_BATCH_MILLIBUCKETS) {
            sourceUnavailable();
        }
    }

    private void sourceUnavailable() {
        pumpjackState.setDepositCounts(
                pumpjackState.depositSize(),
                Math.max(0, pumpjackState.depositRemainingSources() - 1)
        );
        pumpjackState.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
        machine.markChanged();
    }

    private boolean validatePumpjackCursor(PumpjackCursor cursor) {
        if (pumpjackState.cursorIndex() <= cursor.totalPositions()) {
            return true;
        }
        resetPumpjack();
        return false;
    }

    private DrillStatus drillStatus(ServerLevel level, int targetY) {
        BlockPos head = drillHead();
        if (targetY > head.getY()) {
            return DrillStatus.MISSING;
        }
        PumpjackCursor cursor = PumpjackCursor.digging(head, targetY);
        for (int index = 0; index < cursor.totalPositions(); index++) {
            BlockPos position = cursor.positionAt(index);
            if (!loaded(level, position)) {
                return DrillStatus.UNLOADED;
            }
            if (!level.getBlockState(position)
                    .is(committee.nova.mods.magneticraft.init.ModAdvancedBlocks.PUMPJACK_DRILL.get())) {
                return DrillStatus.MISSING;
            }
        }
        return DrillStatus.INTACT;
    }

    private BlockPos drillHead() {
        return machine.getBlockPos().relative(machine.facing(), 5);
    }

    private boolean loaded(ServerLevel level, BlockPos position) {
        return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) != null;
    }

    private void resetPumpjack() {
        pumpjackState.reset();
        machine.markChanged();
    }

    private void tickSolarMirror(ServerLevel level) {
        if (!level.isDay() || level.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos origin = machine.getBlockPos();
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

}
