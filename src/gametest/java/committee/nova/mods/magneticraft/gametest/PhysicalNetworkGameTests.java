package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlock;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlock;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ConveyorRoute;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

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
    public static void conduitArmsFollowPipeAndMachineConnections(GameTestHelper helper) {
        Block[] conduits = {
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                ModNetworkBlocks.HEAT_PIPE.get(),
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                ModNetworkBlocks.IRON_PIPE.get(),
                ModNetworkBlocks.PNEUMATIC_TUBE.get(),
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get()
        };
        for (Block conduit : conduits) {
            helper.setBlock(MIDDLE, Blocks.AIR);
            helper.setBlock(LAST, Blocks.AIR);
            helper.setBlock(MIDDLE, conduit);
            refreshConduit(helper, MIDDLE);
            assertConnections(helper, MIDDLE);

            helper.setBlock(LAST, conduit);
            refreshConduit(helper, LAST);
            assertConnections(helper, MIDDLE, Direction.EAST);
            assertConnections(helper, LAST, Direction.WEST);

            helper.setBlock(LAST, Blocks.AIR);
            refreshConduit(helper, LAST);
            assertConnections(helper, MIDDLE);
        }

        helper.setBlock(MIDDLE, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(
                LAST,
                ModMachineBlocks.BATTERY.get().defaultBlockState().setValue(BatteryBlock.FACING, Direction.EAST)
        );
        refreshConduit(helper, MIDDLE);
        assertConnections(helper, MIDDLE, Direction.EAST);

        helper.setBlock(MIDDLE, ModNetworkBlocks.HEAT_PIPE.get());
        helper.setBlock(LAST, ModNetworkBlocks.HEAT_SINK.get());
        refreshConduit(helper, MIDDLE);
        assertConnections(helper, MIDDLE, Direction.EAST);

        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(LAST, ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        refreshConduit(helper, MIDDLE);
        assertConnections(helper, MIDDLE, Direction.EAST);

        helper.setBlock(MIDDLE, ModNetworkBlocks.PNEUMATIC_TUBE.get());
        helper.setBlock(LAST, Blocks.CHEST);
        refreshConduit(helper, MIDDLE);
        assertConnections(helper, MIDDLE, Direction.EAST);

        helper.setBlock(MIDDLE, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(LAST, Blocks.STONE);
        refreshConduit(helper, MIDDLE);
        assertConnections(helper, MIDDLE);
        helper.succeed();
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
            helper.assertTrue(
                    first.pipe().node().amount() + second.pipe().node().amount() == 160,
                    "Fluid component did not preserve its aggregate amount"
            );
            helper.assertTrue(input.drain(40, IFluidHandler.FluidAction.SIMULATE).getAmount() == 40, "Passive side simulation failed");
            helper.assertTrue(
                    first.pipe().node().amount() + second.pipe().node().amount() == 160,
                    "Passive drain simulation mutated the component"
            );
            helper.assertTrue(input.drain(40, IFluidHandler.FluidAction.EXECUTE).getAmount() == 40, "Passive side did not drain fluid");
            helper.assertTrue(
                    first.pipe().node().amount() + second.pipe().node().amount() == 120,
                    "Passive side drained the wrong aggregate amount"
            );

            first.pipe().setSideMode(Direction.WEST, FluidPipeModule.SideMode.ACTIVE);
            helper.assertFalse(first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).isPresent(), "Active side exposed fluid capability");
            helper.assertTrue(input.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Cached capability bypassed active side");

            first.pipe().setSideMode(Direction.WEST, FluidPipeModule.SideMode.DISABLED);
            helper.assertFalse(first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST).isPresent(), "Disabled side exposed fluid capability");
            helper.assertTrue(input.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "Cached capability bypassed disabled side");

            int localAmount = first.pipe().node().amount();
            CompoundTag saved = first.saveWithoutMetadata();
            IronPipeBlockEntity restored = new IronPipeBlockEntity(first.getBlockPos(), first.getBlockState());
            restored.load(saved);
            helper.assertTrue(restored.pipe().node().amount() == localAmount, "Pipe fluid did not survive NBT reload");
            helper.assertTrue(restored.pipe().sideMode(Direction.WEST) == FluidPipeModule.SideMode.DISABLED, "Pipe side mode did not persist");

            PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
            BlockPos absolute = helper.absolutePos(FIRST);
            first.setRemoved();
            helper.assertTrue(manager.node(NetworkDomain.FLUID, absolute).isEmpty(), "Unloaded pipe remained registered");
            first.clearRemoved();
            first.onLoad();
            helper.assertTrue(manager.node(NetworkDomain.FLUID, absolute).isPresent(), "Reloaded pipe did not re-register");
            helper.assertTrue(first.pipe().node().amount() == localAmount, "Runtime unload changed authoritative fluid");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 55)
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

        helper.runAfterDelay(36, () -> {
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
            helper.assertTrue(conveyor.belt().parcels().get(0).progress() == 2, "Redstone-disabled conveyor advanced");
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

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void conveyorRightClickStoresRetrievesAndAllowsContinuedPlacement(GameTestHelper helper) {
        helper.setBlock(
                MIDDLE,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        ConveyorBeltBlockEntity conveyor = require(helper, MIDDLE, ConveyorBeltBlockEntity.class);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_INGOT, 5));

        InteractionResult inserted = useConveyor(helper, conveyor, player);
        helper.assertTrue(inserted.consumesAction(), "Right click did not store the held stack");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Stored stack remained in hand");
        helper.assertTrue(conveyor.belt().parcels().size() == 1, "Right click did not create exactly one parcel");
        helper.assertTrue(conveyor.belt().parcels().get(0).stack().getCount() == 5, "Right click stored the wrong count");

        InteractionResult removed = useConveyor(helper, conveyor, player);
        helper.assertTrue(removed.consumesAction(), "Empty-hand right click did not retrieve a parcel");
        helper.assertTrue(conveyor.belt().parcels().isEmpty(), "Retrieved parcel remained on the conveyor");
        helper.assertTrue(player.getInventory().countItem(Items.COPPER_INGOT) == 5, "Retrieved stack was lost or duplicated");

        ItemStack belts = new ItemStack(ModNetworkBlocks.CONVEYOR_BELT.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, belts);
        InteractionResult placement = useConveyor(helper, conveyor, player);
        helper.assertTrue(placement == InteractionResult.PASS, "Held conveyor item did not pass through to block placement");
        helper.assertTrue(belts.getCount() == 3, "Placement pass-through consumed a conveyor item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void conveyorTopCapabilityReadsAndExtractsWithoutSimulationMutation(GameTestHelper helper) {
        helper.setBlock(
                MIDDLE,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        ConveyorBeltBlockEntity conveyor = require(helper, MIDDLE, ConveyorBeltBlockEntity.class);
        var handler = conveyor.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false).isEmpty(),
                "Top capability rejected a valid input");
        helper.assertTrue(handler.getSlots() == 2, "Top capability did not expose its input and parcel slots");
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 5, "Top capability did not expose parcel contents");

        ItemStack simulated = handler.extractItem(1, 2, true);
        helper.assertTrue(simulated.is(Items.COPPER_INGOT) && simulated.getCount() == 2,
                "Top capability simulated the wrong extraction");
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 5, "Simulated extraction mutated the parcel");

        ItemStack extracted = handler.extractItem(1, 2, false);
        helper.assertTrue(extracted.is(Items.COPPER_INGOT) && extracted.getCount() == 2,
                "Top capability executed the wrong extraction");
        helper.assertTrue(handler.getStackInSlot(1).getCount() == 3, "Executed extraction removed the wrong count");
        helper.assertTrue(handler.extractItem(1, 64, false).getCount() == 3, "Final extraction returned the wrong remainder");
        helper.assertTrue(handler.getSlots() == 1 && conveyor.belt().parcels().isEmpty(),
                "Empty parcel remained exposed after extraction");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 35)
    public static void conveyorBackpressureReleasesExactlyOnce(GameTestHelper helper) {
        helper.setBlock(
                FIRST,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(MIDDLE, Blocks.CHEST);
        ConveyorBeltBlockEntity conveyor = require(helper, FIRST, ConveyorBeltBlockEntity.class);
        ChestBlockEntity chest = require(helper, MIDDLE, ChestBlockEntity.class);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        var input = conveyor.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        input.insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);

        helper.runAfterDelay(18, () -> {
            helper.assertTrue(countItem(chest, Items.COPPER_INGOT) == 0, "Full inventory accepted a conveyor parcel");
            helper.assertTrue(conveyor.belt().parcels().size() == 1, "Backpressure deleted or duplicated a parcel");
            var parcel = conveyor.belt().parcels().get(0);
            helper.assertTrue(parcel.progress() == 14 && parcel.locked(), "Blocked parcel did not stop at the open-end limit");
            chest.clearContent();
        });

        helper.runAfterDelay(23, () -> {
            helper.assertTrue(countItem(chest, Items.COPPER_INGOT) == 5, "Released backpressure lost or duplicated items");
            helper.assertTrue(conveyor.belt().parcels().isEmpty(), "Released conveyor retained a duplicate parcel");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 70)
    public static void conveyorAutoCornerUsesLegacyRoutesAndTransfersExactlyOnce(GameTestHelper helper) {
        BlockPos cornerOutput = new BlockPos(1, 1, 2);
        BlockPos chestPosition = new BlockPos(1, 1, 3);
        helper.setBlock(
                FIRST,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(
                MIDDLE,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.SOUTH)
        );
        helper.setBlock(
                cornerOutput,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.SOUTH)
        );
        helper.setBlock(chestPosition, Blocks.CHEST);
        ConveyorBeltBlockEntity source = require(helper, FIRST, ConveyorBeltBlockEntity.class);
        ConveyorBeltBlockEntity corner = require(helper, MIDDLE, ConveyorBeltBlockEntity.class);
        ConveyorBeltBlockEntity output = require(helper, cornerOutput, ConveyorBeltBlockEntity.class);
        var input = source.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        input.insertItem(0, new ItemStack(Items.IRON_INGOT), false);
        input.insertItem(0, new ItemStack(Items.GOLD_INGOT), false);

        helper.runAfterDelay(18, () -> {
            helper.assertTrue(corner.belt().parcels().stream().anyMatch(parcel -> parcel.route() == ConveyorRoute.LEFT_CORNER),
                    "Corner did not map the left lane to LEFT_CORNER");
            helper.assertTrue(corner.belt().parcels().stream().anyMatch(parcel -> parcel.route() == ConveyorRoute.RIGHT_SHORT),
                    "Corner did not map the right lane to RIGHT_SHORT");
        });

        helper.runAfterDelay(58, () -> {
            ChestBlockEntity chest = require(helper, chestPosition, ChestBlockEntity.class);
            helper.assertTrue(countItem(chest, Items.IRON_INGOT) == 1, "Corner lost or duplicated the left-lane item");
            helper.assertTrue(countItem(chest, Items.GOLD_INGOT) == 1, "Corner lost or duplicated the right-lane item");
            helper.assertTrue(source.belt().parcels().isEmpty(), "Corner source retained a duplicate parcel");
            helper.assertTrue(corner.belt().parcels().isEmpty(), "Corner retained a duplicate parcel");
            helper.assertTrue(output.belt().parcels().isEmpty(), "Corner output retained a duplicate parcel");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void conveyorOutputsIntoInventoryBelowAnOpenFront(GameTestHelper helper) {
        BlockPos elevatedConveyor = new BlockPos(0, 2, 1);
        BlockPos openFront = new BlockPos(1, 2, 1);
        helper.setBlock(
                elevatedConveyor,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(openFront, Blocks.AIR);
        helper.setBlock(MIDDLE, Blocks.CHEST);
        ConveyorBeltBlockEntity conveyor = require(helper, elevatedConveyor, ConveyorBeltBlockEntity.class);
        conveyor.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new)
                .insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);

        helper.runAfterDelay(20, () -> {
            ChestBlockEntity chest = require(helper, MIDDLE, ChestBlockEntity.class);
            helper.assertTrue(countItem(chest, Items.COPPER_INGOT) == 5,
                    "Open-front conveyor did not output into the forward-below inventory");
            helper.assertTrue(conveyor.belt().parcels().isEmpty(), "Forward-below output retained a duplicate parcel");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 65)
    public static void conveyorPausesForRemovedTargetAndResumesSafely(GameTestHelper helper) {
        helper.setBlock(
                FIRST,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(
                MIDDLE,
                ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST)
        );
        helper.setBlock(LAST, Blocks.CHEST);
        ConveyorBeltBlockEntity source = require(helper, FIRST, ConveyorBeltBlockEntity.class);
        ConveyorBeltBlockEntity target = require(helper, MIDDLE, ConveyorBeltBlockEntity.class);
        ChestBlockEntity chest = require(helper, LAST, ChestBlockEntity.class);
        target.setRemoved();
        source.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new)
                .insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(source.belt().parcels().size() == 1, "Removed target deleted the source parcel");
            helper.assertTrue(target.belt().parcels().isEmpty(), "Removed target accepted a parcel");
            helper.assertTrue(countItem(chest, Items.COPPER_INGOT) == 0, "Removed target forwarded a parcel");
            helper.setBlock(MIDDLE, Blocks.AIR);
            helper.setBlock(
                    MIDDLE,
                    ModNetworkBlocks.CONVEYOR_BELT.get().defaultBlockState()
                            .setValue(ConveyorBeltBlock.FACING, Direction.EAST)
            );
        });

        helper.runAfterDelay(55, () -> {
            helper.assertTrue(countItem(chest, Items.COPPER_INGOT) == 5, "Reloaded target did not resume exactly once");
            ConveyorBeltBlockEntity reloaded = require(helper, MIDDLE, ConveyorBeltBlockEntity.class);
            helper.assertTrue(source.belt().parcels().isEmpty() && reloaded.belt().parcels().isEmpty(),
                    "Reloaded conveyor chain retained a duplicate parcel");
            helper.succeed();
        });
    }

    private static void assertConnections(
            GameTestHelper helper,
            BlockPos position,
            Direction... connectedDirections
    ) {
        Set<Direction> connected = Set.of(connectedDirections);
        BlockState state = helper.getBlockState(position);
        for (Direction direction : Direction.values()) {
            boolean actual = state.getValue(ConduitBlock.property(direction));
            helper.assertTrue(
                    actual == connected.contains(direction),
                    state.getBlock() + " connection " + direction + " was " + actual + ", expected " + connected
            );
        }
    }

    private static void refreshConduit(GameTestHelper helper, BlockPos position) {
        ConduitBlock.refreshAround(helper.getLevel(), helper.absolutePos(position));
    }

    private static InteractionResult useConveyor(
            GameTestHelper helper,
            ConveyorBeltBlockEntity conveyor,
            Player player
    ) {
        BlockPos position = conveyor.getBlockPos();
        return ModNetworkBlocks.CONVEYOR_BELT.get().use(
                conveyor.getBlockState(),
                helper.getLevel(),
                position,
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false)
        );
    }

    private static int countItem(ChestBlockEntity chest, Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
