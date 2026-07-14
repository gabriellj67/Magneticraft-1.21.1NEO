package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void thermopileUsesAnInternalEightyKilojouleBuffer(GameTestHelper helper) {
        SingleBlockMachineBlockEntity thermopile = placeMachine(helper, SingleBlockMachineDefinition.THERMOPILE);
        EnergyStorageModule energy = requireEnergy(helper, thermopile);
        helper.assertTrue(energy.getMaxEnergyStored() == 80_000, "Thermopile buffer capacity is not 80 kJ");
        helper.assertFalse(
                thermopile.getCapability(ForgeCapabilities.ENERGY, Direction.UP).isPresent(),
                "Thermopile leaked its internal buffer as Forge Energy"
        );
        thermopile.electricity().node().setVoltage(125.0D);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(energy.getEnergyStored() > 0, "120 V bridge did not charge the thermopile buffer");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void infiniteSourceMaintainsItsNativeOneHundredTwentyFiveVoltContract(GameTestHelper helper) {
        SingleBlockMachineBlockEntity source = placeMachine(helper, SingleBlockMachineDefinition.INFINITE_ENERGY);
        source.electricity().node().setVoltage(0.0D);

        helper.runAfterDelay(2, () -> {
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
        EnergyStorageModule energy = requireEnergy(helper, transformer);
        energy.setEnergyStored(1_000);
        transformer.electricity().node().setEnergyJoules(0.0D);

        helper.runAfterDelay(2, () -> {
            int storedFe = energy.getEnergyStored();
            double storedJoules = transformer.electricity().node().energyJoules();
            helper.assertTrue(storedFe < 1_000 && storedJoules > 0.0D, "RF transformer did not transfer energy");
            helper.assertTrue(
                    Math.abs(storedFe + storedJoules - 1_000.0D) < 0.000001D,
                    "RF transformer violated the 1 FE = 1 J conservation contract"
            );
            helper.assertTrue(storedJoules <= 200.0D, "RF transformer exceeded its bounded two-tick transfer rate");
            helper.succeed();
        });
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
        EnergyStorageModule electricEnergy = requireEnergy(helper, electric);
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

        electric.electricity().node().setVoltage(125.0D);
        double suppliedJoules = electric.electricity().node().energyJoules();
        double heatedJoules = electric.heat().node().internalEnergyJoules();
        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(),
                electric.getBlockPos(),
                electric.getBlockState(),
                electric
        );
        helper.assertTrue(
                electricEnergy.getEnergyStored() == 120,
                "Electric-heater ticker did not bridge 200 J and consume exactly 80 J"
        );
        helper.assertTrue(
                Math.abs(suppliedJoules - electric.electricity().node().energyJoules() - 200.0D) < 0.000001D,
                "Electric-heater ticker bypassed or mis-accounted its electrical bridge"
        );
        helper.assertTrue(
                Math.abs(electric.heat().node().internalEnergyJoules() - heatedJoules - 80.0D) < 0.000001D,
                "Electric-heater ticker did not convert 80 J into heat"
        );
        helper.assertTrue(electric.working(), "Electric-heater ticker did not mark successful conversion as working");

        helper.setBlock(CENTER, Blocks.AIR);
        SingleBlockMachineBlockEntity partial = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_HEATER);
        EnergyStorageModule partialEnergy = requireEnergy(helper, partial);
        partial.electricity().node().setVoltage(63.5D);
        partial.heat().node().setTemperature(400.0D);
        double partialSupply = partial.electricity().node().energyJoules();
        double partialHeat = partial.heat().node().internalEnergyJoules();
        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(),
                partial.getBlockPos(),
                partial.getBlockState(),
                partial
        );
        int bridgedPartial = partialEnergy.getEnergyStored();
        helper.assertTrue(
                bridgedPartial > 0 && bridgedPartial < 80,
                "Electric-heater bridge did not produce a 1..79 J boundary fixture"
        );
        helper.assertTrue(
                Math.abs(partialSupply - partial.electricity().node().energyJoules() - bridgedPartial) < 0.000001D,
                "Electric heater consumed part of its bridged 1..79 J buffer"
        );
        helper.assertTrue(
                partial.heat().node().internalEnergyJoules() <= partialHeat - 10.0D,
                "Electric heater did not dissipate heat with a partial 1..79 J buffer"
        );
        helper.assertFalse(partial.working(), "Electric heater reported working with less than 80 J");

        SingleBlockMachineBlockEntity rf = placeMachineAt(
                helper,
                CENTER.above(),
                SingleBlockMachineDefinition.RF_HEATER
        );
        EnergyStorageModule rfEnergy = requireEnergy(helper, rf);
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
    public static void engineBufferIsBidirectionalButCannotBackfeedElectricalNetwork(GameTestHelper helper) {
        SingleBlockMachineBlockEntity engine = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_ENGINE);
        EnergyStorageModule energy = requireEnergy(helper, engine);
        for (Direction direction : Direction.values()) {
            var capability = engine.getCapability(ForgeCapabilities.ENERGY, direction)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(capability.canReceive(), "Electric engine cannot receive FE on " + direction);
            helper.assertTrue(capability.canExtract(), "Electric engine cannot extract FE on " + direction);
        }
        helper.assertTrue(
                engine.getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                        .orElseThrow(AssertionError::new)
                        .receiveEnergy(Integer.MAX_VALUE, true) == 80_000,
                "Electric engine buffer retained a 1,000 FE external rate cap"
        );
        energy.setEnergyStored(1_000);
        engine.electricity().node().setVoltage(0.0D);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(energy.getEnergyStored() == 1_000, "Electric engine backfed FE into the electrical network");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void engineConvertsAtFullRateAtExactlySixtyVoltsAndMarksWorking(GameTestHelper helper) {
        helper.setBlock(CENTER.relative(Direction.SOUTH), Blocks.AIR);
        SingleBlockMachineBlockEntity engine = placeMachine(helper, SingleBlockMachineDefinition.ELECTRIC_ENGINE);
        EnergyStorageModule energy = requireEnergy(helper, engine);
        engine.electricity().node().setVoltage(60.0D);
        double before = engine.electricity().node().energyJoules();

        SingleBlockMachineBlockEntity.serverTick(
                helper.getLevel(),
                engine.getBlockPos(),
                engine.getBlockState(),
                engine
        );

        helper.assertTrue(energy.getEnergyStored() == 1_000, "Electric engine was not full-speed at exactly 60 V");
        helper.assertTrue(engine.working(), "Electric engine conversion did not set its working state");
        helper.assertTrue(
                Math.abs(before - engine.electricity().node().energyJoules() - energy.getEnergyStored()) < 0.000001D,
                "Electric engine violated the 1 J = 1 FE conservation contract"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void engineActivelyOutputsOnlyThroughItsDirectionalFace(GameTestHelper helper) {
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
        requireEnergy(helper, engine).setEnergyStored(1_000);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    requireEnergy(helper, outputReceiver).getEnergyStored() > 0,
                    "Electric engine did not actively output through its configured face"
            );
            helper.assertTrue(
                    requireEnergy(helper, sideReceiver).getEnergyStored() == 0,
                    "Electric engine actively output through a non-output face"
            );
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

    private static EnergyStorageModule requireEnergy(
            GameTestHelper helper,
            SingleBlockMachineBlockEntity machine
    ) {
        helper.assertTrue(machine.energy() != null, machine.definition().id() + " has no energy buffer");
        return machine.energy();
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
