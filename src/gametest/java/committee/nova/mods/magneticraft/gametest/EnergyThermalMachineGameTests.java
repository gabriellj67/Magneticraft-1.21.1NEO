package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.item.OilProspectorItem;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositSavedData;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/** Released 1.12 energy and thermal-machine behavior contracts. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyThermalMachineGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    private EnergyThermalMachineGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void batteryKeepsFeInternalAndSeparatesAutomationFromMenuFiltering(GameTestHelper helper) {
        helper.setBlock(CENTER, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = requireBlockEntity(helper, CENTER, BatteryBlockEntity.class);

        helper.assertFalse(
                battery.getCapability(ForgeCapabilities.ENERGY, null).isPresent(),
                "Battery exposed an unsided Forge Energy capability"
        );
        for (Direction direction : Direction.values()) {
            helper.assertFalse(
                    battery.getCapability(ForgeCapabilities.ENERGY, direction).isPresent(),
                    "Battery exposed Forge Energy on " + direction
            );
        }

        IItemHandler automation = battery.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        ItemStack remainder = automation.insertItem(0, new ItemStack(Items.DIRT), false);
        helper.assertTrue(remainder.isEmpty(), "Raw automation could not insert a non-energy item");

        Player player = helper.makeMockSurvivalPlayer();
        BatteryMenu menu = new BatteryMenu(1, player.getInventory(), battery);
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "Charge slot accepted a non-energy item");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT)), "Discharge slot accepted a non-energy item");
        ItemStack portableBattery = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        helper.assertTrue(menu.getSlot(0).mayPlace(portableBattery), "Charge slot rejected a chargeable cell");
        helper.assertTrue(menu.getSlot(1).mayPlace(portableBattery), "Discharge slot rejected an extractable cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tieredBatteriesBindCapacityAndNativeTransferRate(GameTestHelper helper) {
        BlockPos lowPosition = CENTER;
        BlockPos mediumPosition = CENTER.east(2);
        BlockPos highPosition = CENTER.west(1);
        helper.setBlock(lowPosition, ModMachineBlocks.BATTERY.get());
        helper.setBlock(mediumPosition, ModMachineBlocks.BATTERY.get());
        helper.setBlock(highPosition, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity low = requireBlockEntity(helper, lowPosition, BatteryBlockEntity.class);
        BatteryBlockEntity medium = requireBlockEntity(helper, mediumPosition, BatteryBlockEntity.class);
        BatteryBlockEntity high = requireBlockEntity(helper, highPosition, BatteryBlockEntity.class);

        medium.electricity().applyTierFromPlacementData(VoltageTierIds.MEDIUM);
        high.electricity().applyTierFromPlacementData(VoltageTierIds.HIGH);
        helper.assertTrue(medium.electricity().tierId().equals(VoltageTierIds.MEDIUM),
                "Medium-voltage battery rejected its tier payload");
        helper.assertTrue(high.electricity().tierId().equals(VoltageTierIds.HIGH),
                "High-voltage battery rejected its tier payload");

        assertBatteryProfile(helper, low, 1_000_000, 640);
        assertBatteryProfile(helper, medium, 4_000_000, 2_560);
        assertBatteryProfile(helper, high, 16_000_000, 10_240);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void thermopileUsesItsDataDrivenGeneratorBuffer(GameTestHelper helper) {
        SingleBlockMachineBlockEntity thermopile = placeMachine(helper, SingleBlockMachineDefinition.THERMOPILE);
        ElectricalPowerModule energy = requirePower(helper, thermopile);
        helper.assertTrue(energy.ratedCapacityWholeJoules() == 4_000,
                "Thermopile node capacity did not bind its data profile");
        helper.assertFalse(
                thermopile.getCapability(ForgeCapabilities.ENERGY, Direction.UP).isPresent(),
                "Thermopile leaked its internal buffer as Forge Energy"
        );
        thermopile.electricity().node().setVoltage(125.0D);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    Math.abs(energy.storedJoules() - thermopile.electricity().node().energyJoules()) < 1.0E-6D,
                    "Thermopile exposed a second energy balance"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void infiniteSourceMaintainsItsNativeOneHundredTwentyFiveVoltContract(GameTestHelper helper) {
        SingleBlockMachineBlockEntity source = placeMachine(helper, SingleBlockMachineDefinition.INFINITE_ENERGY);
        source.electricity().node().setVoltage(0.0D);

        helper.runAfterDelay(105, () -> {
            helper.assertTrue(
                    Math.abs(source.electricity().node().voltage() - 125.0D) < 0.000001D,
                    "Infinite source did not restore its fixed 125 V output"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void rfTransformerConvertsForgeEnergyToNativeJoulesOneToOne(GameTestHelper helper) {
        SingleBlockMachineBlockEntity transformer = placeMachine(helper, SingleBlockMachineDefinition.RF_TRANSFORMER);
        ElectricalPowerModule energy = requirePower(helper, transformer);
        transformer.electricity().node().setEnergyJoules(0.0D);
        var forgeEnergy = transformer.getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(forgeEnergy.receiveEnergy(1_000, true) == 100,
                "RF transformer simulation ignored its 100 J/t profile rate");
        helper.assertTrue(energy.storedWholeJoules() == 0, "RF transformer simulation mutated node energy");
        helper.assertTrue(forgeEnergy.receiveEnergy(1_000, false) == 100,
                "RF transformer did not accept its bounded FE input");
        helper.assertTrue(energy.storedWholeJoules() == 100,
                "RF transformer did not convert accepted FE directly to J");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void boilerExposesBothTanksAndActivelyExportsWithoutRateCap(GameTestHelper helper) {
        SingleBlockMachineBlockEntity boiler = placeMachine(helper, SingleBlockMachineDefinition.STEAM_BOILER);
        SideLimitedFluidReceiverBlockEntity receiver = placeFluidReceiver(
                helper, CENTER.above(), Direction.DOWN
        );
        helper.assertTrue(
                receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent(),
                "Boiler receiver did not expose its bottom input"
        );
        helper.assertFalse(
                receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "Boiler receiver unexpectedly exposed its top input"
        );

        for (Direction direction : Direction.values()) {
            IFluidHandler handler = boiler.getCapability(ForgeCapabilities.FLUID_HANDLER, direction)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(handler.getTanks() == 2, "Boiler did not expose both tanks on " + direction);
        }
        IFluidHandler handler = boiler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(
                handler.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1_000),
                        IFluidHandler.FluidAction.EXECUTE) == 1_000,
                "Boiler water input rejected a full tank"
        );
        FluidStack steam = new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 5_000);
        boiler.secondaryTank().tank().fill(steam, IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    receiver.tank().getFluidAmount() == 5_000,
                    "Boiler did not export through the target's DOWN capability"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void boilerCombinedFluidCapabilityInvalidatesAndRevives(GameTestHelper helper) {
        SingleBlockMachineBlockEntity boiler = placeMachine(helper, SingleBlockMachineDefinition.STEAM_BOILER);
        LazyOptional<IFluidHandler> oldCapability = boiler.getCapability(
                ForgeCapabilities.FLUID_HANDLER,
                Direction.NORTH
        );
        IFluidHandler oldHandler = oldCapability.orElseThrow(AssertionError::new);
        helper.assertTrue(oldHandler.getTanks() == 2, "Initial boiler capability did not combine both tanks");

        boiler.invalidateCaps();
        helper.assertFalse(oldCapability.isPresent(), "Invalidated boiler LazyOptional remained live");
        helper.assertFalse(
                boiler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(),
                "Invalidated boiler still exposed its combined fluid handler"
        );

        boiler.reviveCaps();
        IFluidHandler revivedHandler = boiler.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(revivedHandler != oldHandler, "Boiler revived the stale combined fluid handler instance");
        for (Direction direction : Direction.values()) {
            IFluidHandler sided = boiler.getCapability(ForgeCapabilities.FLUID_HANDLER, direction)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(sided.getTanks() == 2, "Revived boiler lost a tank on " + direction);
        }

        helper.assertTrue(
                revivedHandler.fill(
                        new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 250),
                        IFluidHandler.FluidAction.EXECUTE
                ) == 250,
                "Revived boiler did not route fills to its water tank"
        );
        boiler.secondaryTank().tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 500),
                IFluidHandler.FluidAction.EXECUTE
        );
        helper.assertTrue(
                revivedHandler.getFluidInTank(0).getFluid() == net.minecraft.world.level.material.Fluids.WATER
                        && revivedHandler.getFluidInTank(1).getFluid()
                        == ModFluids.get(FluidDefinition.STEAM).source().get(),
                "Revived boiler swapped or hid its water and steam tanks"
        );
        helper.assertTrue(
                revivedHandler.drain(
                        new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100),
                        IFluidHandler.FluidAction.EXECUTE
                ).isEmpty(),
                "Boiler composite handler allowed draining its water input"
        );
        helper.assertTrue(
                revivedHandler.drain(100, IFluidHandler.FluidAction.EXECUTE)
                        .getFluid() == ModFluids.get(FluidDefinition.STEAM).source().get(),
                "Boiler composite handler did not drain its steam output"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void heatersUseReleasedBridgeAndPartialEnergyContract(GameTestHelper helper) {
        SingleBlockMachineBlockEntity electric = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_HEATER);
        ElectricalPowerModule electricEnergy = requirePower(helper, electric);
        helper.assertFalse(
                electric.getCapability(ForgeCapabilities.ENERGY, Direction.UP).isPresent(),
                "Electric heater exposed its bridge buffer as Forge Energy"
        );
        for (Direction direction : Direction.values()) {
            helper.assertTrue(
                    electric.electricalReading(direction).isPresent(),
                    "Electric heater lost its electrical-network port on " + direction
            );
            helper.assertTrue(
                    electric.thermalReading(direction).isPresent()
                            == (direction.getAxis() == Direction.Axis.Y),
                    "Electric heater heat port does not match its vertical contract on " + direction
            );
        }

        electricEnergy.setStoredJoules(4_000);
        double heatedJoules = electric.heat().node().internalEnergyJoules();
        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(),
                electric.getBlockPos(),
                electric.getBlockState(),
                electric
        );
        helper.assertTrue(
                electricEnergy.storedWholeJoules() == 3_920,
                "Electric-heater recipe did not consume its fixed 80 J from the node"
        );
        helper.assertTrue(
                Math.abs(electric.heat().node().internalEnergyJoules() - heatedJoules - 80.0D) < 0.000001D,
                "Electric-heater ticker did not convert 80 J into heat"
        );
        helper.assertTrue(electric.working(), "Electric-heater ticker did not mark successful conversion as working");

        helper.setBlock(CENTER, Blocks.AIR);
        SingleBlockMachineBlockEntity partial = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_HEATER);
        ElectricalPowerModule partialEnergy = requirePower(helper, partial);
        partialEnergy.setStoredJoules(79);
        double partialHeat = partial.heat().node().internalEnergyJoules();
        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(),
                partial.getBlockPos(),
                partial.getBlockState(),
                partial
        );
        helper.assertTrue(
                partialEnergy.storedWholeJoules() == 79,
                "Electric heater consumed an incomplete or undervoltage 79 J node"
        );
        helper.assertTrue(
                Math.abs(partial.heat().node().internalEnergyJoules() - partialHeat) < 0.000001D,
                "Electric heater produced heat from an incomplete cache"
        );
        helper.assertFalse(partial.working(), "Electric heater reported working with less than 80 J");

        SingleBlockMachineBlockEntity rf = placeMachineAt(
                helper,
                CENTER.above(),
                SingleBlockMachineDefinition.RF_HEATER
        );
        EnergyStorageModule rfEnergy = requireForgeEnergy(helper, rf);
        helper.assertTrue(rfEnergy.getMaxEnergyStored() == 80_000, "FE heater buffer capacity is not 80 kJ");
        for (Direction direction : Direction.values()) {
            var capability = rf.getCapability(ForgeCapabilities.ENERGY, direction)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(capability.canReceive(), "FE heater cannot receive on " + direction);
            helper.assertTrue(capability.canExtract(), "FE heater cannot extract on " + direction);
            helper.assertTrue(
                    rf.thermalReading(direction).isPresent()
                            == (direction.getAxis() == Direction.Axis.Y),
                    "FE heater heat port does not match its vertical contract on " + direction
            );
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void engineExposesOnlyNativeElectricity(GameTestHelper helper) {
        SingleBlockMachineBlockEntity engine = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_ENGINE);
        ElectricalPowerModule energy = requirePower(helper, engine);
        helper.assertTrue(energy.electricalControllerBound(), "Electric engine did not bind its native J controller");
        helper.assertFalse(
                engine.getCapability(ForgeCapabilities.ENERGY, null).isPresent(),
                "Electric engine exposed an unsided Forge Energy capability"
        );
        for (Direction direction : Direction.values()) {
            helper.assertFalse(
                    engine.getCapability(ForgeCapabilities.ENERGY, direction).isPresent(),
                    "Electric engine exposed Forge Energy on " + direction
            );
            helper.assertTrue(
                    engine.supportsNetworkConnection(NetworkDomain.ELECTRICITY, direction),
                    "Electric engine did not expose native electricity on " + direction
            );
        }
        energy.setStoredJoules(1_000);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(energy.storedWholeJoules() == 1_000,
                    "Electric engine changed its native J buffer without a native load");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void engineConvertsJToNativeKineticsWithoutForgeEnergy(GameTestHelper helper) {
        helper.setBlock(
                CENTER,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.ELECTRIC_ENGINE).get().defaultBlockState()
                        .setValue(SingleBlockMachineBlock.FACING, Direction.NORTH)
                        .setValue(SingleBlockMachineBlock.MASTER, true)
        );
        SingleBlockMachineBlockEntity engine = requireBlockEntity(
                helper, CENTER, SingleBlockMachineBlockEntity.class
        );
        SingleBlockMachineBlockEntity outputReceiver = placeMachineAt(
                helper, CENTER.south(), SingleBlockMachineDefinition.RF_HEATER
        );
        SingleBlockMachineBlockEntity sideReceiver = placeMachineAt(
                helper, CENTER.east(), SingleBlockMachineDefinition.RF_HEATER
        );
        ElectricalPowerModule energy = requirePower(helper, engine);
        energy.setStoredJoules(32_000);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    requireForgeEnergy(helper, outputReceiver).getEnergyStored() == 0,
                    "Electric engine directly output FE through its configured face"
            );
            helper.assertTrue(
                    requireForgeEnergy(helper, sideReceiver).getEnergyStored() == 0,
                    "Electric engine directly output FE through a side face"
            );
            int consumedJoules = 32_000 - energy.storedWholeJoules();
            helper.assertTrue(consumedJoules > 0 && consumedJoules <= 200,
                    "Electric engine consumed an invalid amount of native J");
            helper.assertTrue(engine.kinetic() != null && engine.kinetic().node().energyJoules() > 0.0D,
                    "Electric engine did not produce native rotary energy");
            helper.assertTrue(engine.kinetic().node().energyJoules() <= consumedJoules * 0.9D,
                    "Electric engine exceeded its 90% J-to-kinetic efficiency");
            helper.assertTrue(engine.working(), "Electric engine did not report native kinetic conversion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void gasifierActivelyExportsItsWholeBufferWhileIdle(GameTestHelper helper) {
        SingleBlockMachineBlockEntity gasifier = placeMachine(helper, SingleBlockMachineDefinition.GASIFICATION_UNIT);
        SideLimitedFluidReceiverBlockEntity receiver = placeFluidReceiver(
                helper, CENTER.above(), Direction.UP
        );
        helper.assertTrue(
                receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "Gasifier receiver did not expose its top input"
        );
        helper.assertFalse(
                receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent(),
                "Gasifier receiver unexpectedly exposed its bottom input"
        );
        FluidStack gas = new FluidStack(ModFluids.get(FluidDefinition.WOOD_GAS).source().get(), 4_000);
        gasifier.primaryTank().tank().fill(gas, IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    receiver.tank().getFluidAmount() == 4_000,
                    "Idle gasifier did not export through the target's UP capability"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void brickFurnacePreservesProgressAcrossRecipeChangesAndUsesWorkingGrace(GameTestHelper helper) {
        SingleBlockMachineBlockEntity furnace = placeMachine(helper, SingleBlockMachineDefinition.BRICK_FURNACE);
        furnace.heat().node().setTemperature(500.0D);
        furnace.inventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        int[] progress = new int[1];

        helper.runAfterDelay(4, () -> {
            progress[0] = furnace.progress();
            helper.assertTrue(progress[0] > 0, "Brick furnace did not start processing");
            furnace.inventory().setStackInSlot(0, new ItemStack(Items.RAW_GOLD));
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(furnace.progress() > progress[0], "Recipe change reset brick-furnace progress");
            furnace.inventory().setStackInSlot(0, ItemStack.EMPTY);
        });
        helper.runAfterDelay(18, () -> helper.assertTrue(
                furnace.getBlockState().getValue(SingleBlockMachineBlock.LIT),
                "Brick furnace lost its approximately 20-tick working grace"
        ));
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(
                    furnace.getBlockState().getValue(SingleBlockMachineBlock.LIT),
                    "Brick furnace stayed working after the grace period"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void internalCombustionEnginePreservesSplitAndBlocksAllConsumptionWhenFull(
            GameTestHelper helper
    ) {
        helper.setBlock(
                CENTER,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.INTERNAL_COMBUSTION_ENGINE).get()
                        .defaultBlockState()
                        .setValue(SingleBlockMachineBlock.FACING, Direction.NORTH)
        );
        SingleBlockMachineBlockEntity engine = requireBlockEntity(
                helper, CENTER, SingleBlockMachineBlockEntity.class
        );
        ElectricalPowerModule energy = requirePower(helper, engine);
        engine.heat().node().setTemperature(293.15D);
        energy.setStoredJoules(0.0D);
        engine.primaryTank().tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.DIESEL).source().get(), 2),
                IFluidHandler.FluidAction.EXECUTE
        );
        double initialHeat = engine.heat().node().internalEnergyJoules();

        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(), engine.getBlockPos(), engine.getBlockState(), engine
        );

        double electricalGain = energy.storedJoules();
        double thermalGain = engine.heat().node().internalEnergyJoules() - initialHeat;
        helper.assertTrue(Math.abs(electricalGain - 120.0D) < 1.0E-6D,
                "Internal combustion engine exceeded or missed its 120 J/t output");
        helper.assertTrue(Math.abs(electricalGain / (electricalGain + thermalGain) - 0.70D) < 1.0E-6D,
                "Internal combustion engine broke its 70/30 energy split");
        helper.assertTrue(engine.primaryTank().tank().getFluidAmount() == 1,
                "Internal combustion engine did not stage exactly one fuel mB");
        helper.assertFalse(engine.getCapability(ForgeCapabilities.ENERGY, Direction.SOUTH).isPresent(),
                "Internal combustion engine exposed a Forge Energy conversion path");

        energy.setStoredJoules(energy.ratedCapacityJoules());
        int blockedFuel = engine.primaryTank().tank().getFluidAmount();
        double blockedHeat = engine.heat().node().internalEnergyJoules();
        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(), engine.getBlockPos(), engine.getBlockState(), engine
        );
        helper.assertTrue(engine.primaryTank().tank().getFluidAmount() == blockedFuel,
                "Full electrical output consumed another fuel mB");
        helper.assertTrue(Math.abs(engine.heat().node().internalEnergyJoules() - blockedHeat) < 1.0E-6D,
                "Full electrical output produced unbuffered heat");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void geothermalPumpConsumesOneLoadedLavaSourceOnDemand(GameTestHelper helper) {
        SingleBlockMachineBlockEntity pump = placeMachine(
                helper, SingleBlockMachineDefinition.GEOTHERMAL_PUMP
        );
        Player owner = helper.makeMockSurvivalPlayer();
        pump.setOwner(owner.getUUID());
        helper.setBlock(CENTER.below(), Blocks.LAVA);
        double initialHeat = pump.heat().node().internalEnergyJoules();

        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(), pump.getBlockPos(), pump.getBlockState(), pump
        );

        helper.assertTrue(helper.getBlockState(CENTER.below()).is(Blocks.OBSIDIAN),
                "Geothermal pump did not replace its registered loaded lava source with obsidian");
        helper.assertTrue(Math.abs(pump.heat().node().internalEnergyJoules() - initialHeat - 120.0D) < 1.0E-6D,
                "Geothermal pump did not respect its 120 J/t heat output");
        owner.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void oilProspectorChargesEveryAttemptExceptInsufficientEnergy(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack prospector = new ItemStack(ModMachineItems.OIL_PROSPECTOR.get());
        var storage = prospector.getCapability(ForgeCapabilities.ENERGY)
                .orElseThrow(AssertionError::new);
        storage.receiveEnergy(1_000, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, prospector);
        BlockPos absolute = helper.absolutePos(CENTER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);

        InteractionResult emptySurvey = prospector.useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
        );
        helper.assertTrue(emptySurvey.consumesAction() && storage.getEnergyStored() == 500,
                "Oil prospector did not charge 500 J for a completed no-result scan");

        OilDepositSavedData.get(helper.getLevel()).register(
                absolute,
                absolute,
                OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS
        );
        InteractionResult knownSurvey = prospector.useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
        );
        helper.assertTrue(knownSurvey.consumesAction() && storage.getEnergyStored() == 0,
                "Oil prospector did not charge 500 J for a successful scan");
        prospector.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(storage.getEnergyStored() == 0,
                "Oil prospector changed its balance after rejecting an underpowered scan");
        helper.assertTrue(((OilProspectorItem) prospector.getItem()).capacity() == 25_000,
                "Oil prospector did not expose its 25 kJ portable energy capacity");
        player.discard();
        helper.succeed();
    }

    private static SingleBlockMachineBlockEntity placeMachine(
            GameTestHelper helper,
            SingleBlockMachineDefinition definition
    ) {
        return placeMachineAt(helper, CENTER, definition);
    }

    private static SingleBlockMachineBlockEntity placeMachineAt(
            GameTestHelper helper,
            BlockPos position,
            SingleBlockMachineDefinition definition
    ) {
        helper.setBlock(position, ModMachineBlocks.machine(definition).get());
        return requireBlockEntity(helper, position, SingleBlockMachineBlockEntity.class);
    }

    private static ElectricalPowerModule requirePower(
            GameTestHelper helper,
            SingleBlockMachineBlockEntity machine
    ) {
        helper.assertTrue(machine.energy() != null, machine.definition().id() + " has no native joule module");
        return machine.energy();
    }

    private static EnergyStorageModule requireForgeEnergy(
            GameTestHelper helper,
            SingleBlockMachineBlockEntity machine
    ) {
        helper.assertTrue(machine.forgeEnergy() != null, machine.definition().id() + " has no FE storage");
        return machine.forgeEnergy();
    }

    private static SideLimitedFluidReceiverBlockEntity placeFluidReceiver(
            GameTestHelper helper,
            BlockPos position,
            Direction inputSide
    ) {
        helper.setBlock(position, Blocks.BARREL);
        BlockPos absolutePosition = helper.absolutePos(position);
        SideLimitedFluidReceiverBlockEntity receiver = new SideLimitedFluidReceiverBlockEntity(
                absolutePosition,
                helper.getLevel().getBlockState(absolutePosition),
                inputSide
        );
        helper.getLevel().setBlockEntity(receiver);
        return receiver;
    }

    private static <T extends BlockEntity> T requireBlockEntity(
            GameTestHelper helper,
            BlockPos position,
            Class<T> type
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(type.isInstance(blockEntity), "Missing block entity " + type.getSimpleName());
        return type.cast(blockEntity);
    }

    private static void assertBatteryProfile(
            GameTestHelper helper,
            BatteryBlockEntity battery,
            int expectedCapacity,
            int expectedTransfer
    ) {
        helper.assertTrue(battery.energy().ratedCapacityWholeJoules() == expectedCapacity,
                "Battery capacity did not match its voltage tier");
        helper.assertTrue(battery.energy().maximumTransferJoulesPerTick() == expectedTransfer,
                "Battery native transfer rate did not match its voltage tier");
    }

    private static final class SideLimitedFluidReceiverBlockEntity extends BarrelBlockEntity {
        private final Direction inputSide;
        private final FluidTank tank = new FluidTank(16_000);
        private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);

        private SideLimitedFluidReceiverBlockEntity(
                BlockPos position,
                BlockState state,
                Direction inputSide
        ) {
            super(position, state);
            this.inputSide = inputSide;
        }

        private FluidTank tank() {
            return tank;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeCapabilities.FLUID_HANDLER && side == inputSide) {
                return fluidCapability.cast();
            }
            return super.getCapability(capability, side);
        }

        @Override
        public void invalidateCaps() {
            fluidCapability.invalidate();
            super.invalidateCaps();
        }

        @Override
        public void reviveCaps() {
            super.reviveCaps();
            fluidCapability = LazyOptional.of(() -> tank);
        }
    }
}
