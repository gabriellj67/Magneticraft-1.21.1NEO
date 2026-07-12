package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlock;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlock;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.FluidPipeModule;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Loaded-world topology, capability and persistence contracts for Task 4.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhysicalNetworkGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos FIRST = new BlockPos(0, 1, 1);
    private static final BlockPos MIDDLE = new BlockPos(1, 1, 1);
    private static final BlockPos LAST = new BlockPos(2, 1, 1);

    private PhysicalNetworkGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void electricCablePowersExistingFurnaceThroughVoltageNetwork(GameTestHelper helper) {
        helper.setBlock(
                FIRST,
                ModMachineBlocks.BATTERY.get().defaultBlockState().setValue(BatteryBlock.FACING, Direction.WEST)
        );
        helper.setBlock(MIDDLE, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(LAST, ModMachineBlocks.ELECTRIC_FURNACE.get());

        BatteryBlockEntity battery = require(helper, FIRST, BatteryBlockEntity.class);
        ElectricCableBlockEntity cable = require(helper, MIDDLE, ElectricCableBlockEntity.class);
        ElectricFurnaceBlockEntity furnace = require(helper, LAST, ElectricFurnaceBlockEntity.class);
        battery.energy().setEnergyStored(20_000);

        helper.runAfterDelay(30, () -> {
            helper.assertTrue(battery.energy().getEnergyStored() < 20_000, "Battery did not discharge into voltage network");
            helper.assertTrue(cable.electricity().node().voltage() > 0.0D, "Cable never acquired voltage");
            helper.assertTrue(furnace.energy().getEnergyStored() > 0, "Electric furnace did not receive bridged network energy");
            helper.assertTrue(furnace.electricity().node().voltage() <= 125.0D, "Furnace node exceeded tier voltage");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void topologyMergesSplitsAndRejoinsLocally(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(LAST, ModNetworkBlocks.ELECTRIC_CABLE.get());
        PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());

        helper.runAfterDelay(2, () -> {
            BlockPos first = helper.absolutePos(FIRST);
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, first).size() == 3, "Cable line did not merge");
            var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether != null, "Nether level was not available for isolation check");
            helper.assertTrue(PhysicalNetworkService.manager(nether) != manager, "Two dimensions shared a runtime manager");
            helper.assertTrue(
                    PhysicalNetworkService.manager(nether).nodeCount(NetworkDomain.ELECTRICITY) == 0,
                    "Overworld nodes leaked into Nether manager"
            );
            helper.destroyBlock(MIDDLE);
        });
        helper.runAfterDelay(5, () -> {
            BlockPos first = helper.absolutePos(FIRST);
            BlockPos last = helper.absolutePos(LAST);
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, first).size() == 1, "Broken cable did not split first side");
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, last).size() == 1, "Broken cable did not split second side");
            helper.setBlock(MIDDLE, ModNetworkBlocks.ELECTRIC_CABLE.get());
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(
                    manager.component(NetworkDomain.ELECTRICITY, helper.absolutePos(FIRST)).size() == 3,
                    "Restored cable did not rejoin"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void heatAndPressureEdgesConserveTheirAuthoritativeState(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.HEAT_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.HEAT_PIPE.get());
        HeatPipeBlockEntity hot = require(helper, FIRST, HeatPipeBlockEntity.class);
        HeatPipeBlockEntity cold = require(helper, MIDDLE, HeatPipeBlockEntity.class);
        hot.heat().node().setTemperature(700.0D);
        double heatBefore = hot.heat().node().internalEnergyJoules() + cold.heat().node().internalEnergyJoules();

        helper.runAfterDelay(2, () -> {
            double heatAfter = hot.heat().node().internalEnergyJoules() + cold.heat().node().internalEnergyJoules();
            helper.assertTrue(Math.abs(heatAfter - heatBefore) < 1.0E-3D, "Heat network lost internal energy");
            helper.assertTrue(cold.heat().node().temperatureKelvin() > HeatNode.AMBIENT_TEMPERATURE_KELVIN, "Heat did not conduct");
            Player player = helper.makeMockSurvivalPlayer();
            float health = player.getHealth();
            ModNetworkBlocks.HEAT_PIPE.get().entityInside(
                    hot.getBlockState(),
                    helper.getLevel(),
                    hot.getBlockPos(),
                    player
            );
            helper.assertTrue(player.getHealth() < health, "Hot uninsulated pipe did not hurt entity");

            helper.setBlock(LAST, ModNetworkBlocks.INSULATED_HEAT_PIPE.get());
            HeatPipeBlockEntity insulated = require(helper, LAST, HeatPipeBlockEntity.class);
            insulated.heat().node().setTemperature(700.0D);
            health = player.getHealth();
            ModNetworkBlocks.INSULATED_HEAT_PIPE.get().entityInside(
                    insulated.getBlockState(),
                    helper.getLevel(),
                    insulated.getBlockPos(),
                    player
            );
            helper.assertTrue(player.getHealth() == health, "Insulated pipe hurt entity");

            helper.setBlock(FIRST, ModNetworkBlocks.PNEUMATIC_TUBE.get());
            helper.setBlock(MIDDLE, ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get());
            PneumaticTubeBlockEntity source = require(helper, FIRST, PneumaticTubeBlockEntity.class);
            PneumaticTubeBlockEntity target = require(helper, MIDDLE, PneumaticTubeBlockEntity.class);
            source.pressure().node().setPressureKpa(600.0D);
            double gasBefore = source.pressure().node().gasKpaLiters() + target.pressure().node().gasKpaLiters();

            helper.runAfterDelay(3, () -> {
                double gasAfter = source.pressure().node().gasKpaLiters() + target.pressure().node().gasKpaLiters();
                helper.assertTrue(Math.abs(gasAfter - gasBefore) < 1.0E-6D, "Pressure edge lost gas without a leak");
                helper.assertTrue(target.pressure().node().pressureKpa() > 0.0D, "Restriction tube did not transmit pressure");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void fluidPortsSimulatePersistAndSurviveRuntimeUnload(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        IronPipeBlockEntity first = require(helper, FIRST, IronPipeBlockEntity.class);
        IFluidHandler input = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        FluidStack water = new FluidStack(Fluids.WATER, 160);
        helper.assertTrue(input.fill(water, IFluidHandler.FluidAction.SIMULATE) == 160, "Fluid simulation rejected capacity");
        helper.assertTrue(first.pipe().node().amount() == 0, "Fluid simulation mutated pipe");
        input.fill(water, IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(2, () -> {
            IronPipeBlockEntity second = require(helper, MIDDLE, IronPipeBlockEntity.class);
            helper.assertTrue(first.pipe().node().amount() == 80, "First pipe did not balance to 80mB");
            helper.assertTrue(second.pipe().node().amount() == 80, "Second pipe did not balance to 80mB");

            first.pipe().setSideMode(Direction.WEST, FluidPipeModule.SideMode.ACTIVE);
            IFluidHandler output = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(output.drain(40, IFluidHandler.FluidAction.SIMULATE).getAmount() == 40, "Active side simulation failed");
            helper.assertTrue(first.pipe().node().amount() == 80, "Active drain simulation mutated pipe");
            helper.assertTrue(output.drain(40, IFluidHandler.FluidAction.EXECUTE).getAmount() == 40, "Active side did not output fluid");
            helper.assertTrue(first.pipe().node().amount() == 40, "Active side drained the wrong amount");

            first.pipe().setSideMode(Direction.WEST, FluidPipeModule.SideMode.DISABLED);
            helper.assertFalse(first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).isPresent(), "Disabled side exposed fluid capability");
            helper.assertTrue(input.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Cached capability bypassed disabled side");

            CompoundTag saved = first.saveWithoutMetadata();
            IronPipeBlockEntity restored = new IronPipeBlockEntity(first.getBlockPos(), first.getBlockState());
            restored.load(saved);
            helper.assertTrue(restored.pipe().node().amount() == 40, "Pipe fluid did not survive NBT reload");
            helper.assertTrue(restored.pipe().sideMode(Direction.WEST) == FluidPipeModule.SideMode.DISABLED, "Pipe side mode did not persist");

            PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
            BlockPos absolute = helper.absolutePos(FIRST);
            first.setRemoved();
            helper.assertTrue(manager.node(NetworkDomain.FLUID, absolute).isEmpty(), "Unloaded pipe remained registered");
            first.clearRemoved();
            first.onLoad();
            helper.assertTrue(manager.node(NetworkDomain.FLUID, absolute).isPresent(), "Reloaded pipe did not re-register");
            helper.assertTrue(first.pipe().node().amount() == 40, "Runtime unload changed authoritative fluid");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pneumaticTubeRoutesPersistedItemIntoInventory(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.PNEUMATIC_TUBE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get());
        helper.setBlock(LAST, Blocks.CHEST);
        PneumaticTubeBlockEntity tube = require(helper, FIRST, PneumaticTubeBlockEntity.class);
        ItemStack iron = new ItemStack(Items.IRON_INGOT, 7);
        var input = tube.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        ItemStack oversized = new ItemStack(Items.IRON_INGOT, 65);
        helper.assertTrue(
                input.insertItem(0, oversized, true).getCount() == 1,
                "Tube simulation did not respect its slot limit"
        );
        helper.assertTrue(input.insertItem(0, iron, true).isEmpty(), "Tube simulation rejected valid item");
        helper.assertTrue(tube.logistics().itemsSnapshot().isEmpty(), "Tube simulation created an item");
        input.insertItem(0, iron, false);

        CompoundTag saved = tube.saveWithoutMetadata();
        PneumaticTubeBlockEntity restored = new PneumaticTubeBlockEntity(tube.getBlockPos(), tube.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.logistics().itemsSnapshot().size() == 1, "In-flight item did not survive reload");

        helper.runAfterDelay(25, () -> {
            ChestBlockEntity chest = require(helper, LAST, ChestBlockEntity.class);
            helper.assertTrue(chest.getItem(0).is(Items.IRON_INGOT), "Tube routed the wrong item");
            helper.assertTrue(chest.getItem(0).getCount() == 7, "Tube duplicated or lost item count");
            helper.assertTrue(tube.logistics().itemsSnapshot().isEmpty(), "Source tube retained a duplicate payload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pneumaticIntersectionRoundRobinsEqualPriorityOutputs(GameTestHelper helper) {
        helper.setBlock(FIRST, Blocks.CHEST);
        helper.setBlock(MIDDLE, ModNetworkBlocks.PNEUMATIC_TUBE.get());
        helper.setBlock(LAST, Blocks.CHEST);
        PneumaticTubeBlockEntity tube = require(helper, MIDDLE, PneumaticTubeBlockEntity.class);
        var input = tube.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        input.insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        input.insertItem(0, new ItemStack(Items.GOLD_INGOT), false);

        helper.runAfterDelay(20, () -> {
            ChestBlockEntity first = require(helper, FIRST, ChestBlockEntity.class);
            ChestBlockEntity last = require(helper, LAST, ChestBlockEntity.class);
            helper.assertTrue(!first.getItem(0).isEmpty(), "Round robin starved west output");
            helper.assertTrue(!last.getItem(0).isEmpty(), "Round robin starved east output");
            helper.assertTrue(
                    first.getItem(0).getCount() + last.getItem(0).getCount() == 2,
                    "Round robin duplicated or lost payloads"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void conveyorBlocksOnSimulationThenTransfersExactlyOnce(GameTestHelper helper) {
        helper.setBlock(
                FIRST,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(MIDDLE, Blocks.CHEST);
        ConveyorBeltBlockEntity conveyor = require(helper, FIRST, ConveyorBeltBlockEntity.class);
        var input = conveyor.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        ItemStack oversized = new ItemStack(Items.COPPER_INGOT, 65);
        helper.assertTrue(
                input.insertItem(0, oversized, true).getCount() == 1,
                "Conveyor simulation did not respect its slot limit"
        );
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 5);
        helper.assertTrue(input.insertItem(0, copper, true).isEmpty(), "Conveyor simulation rejected item");
        helper.assertTrue(conveyor.belt().parcels().isEmpty(), "Conveyor simulation mutated state");
        input.insertItem(0, copper, false);
        conveyor.belt().cycleRedstoneMode();

        helper.runAfterDelay(5, () -> {
            ChestBlockEntity chest = require(helper, MIDDLE, ChestBlockEntity.class);
            helper.assertTrue(chest.getItem(0).isEmpty(), "Redstone-disabled conveyor moved an item");
            helper.assertTrue(conveyor.belt().parcels().get(0).progress() == 0, "Redstone-disabled conveyor advanced");
            conveyor.belt().cycleRedstoneMode();
        });

        helper.runAfterDelay(28, () -> {
            ChestBlockEntity chest = require(helper, MIDDLE, ChestBlockEntity.class);
            helper.assertTrue(chest.getItem(0).is(Items.COPPER_INGOT), "Conveyor transferred the wrong item");
            helper.assertTrue(chest.getItem(0).getCount() == 5, "Conveyor duplicated or lost item count");
            helper.assertTrue(conveyor.belt().parcels().isEmpty(), "Conveyor retained a duplicate parcel");
            helper.succeed();
        });
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
