package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SmallTankBlockItem;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlock;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.FluidPipeModule;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Loaded-world contracts dedicated to the 0.4 heat and fluid slice. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeatFluidGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos FIRST = new BlockPos(0, 1, 1);
    private static final BlockPos MIDDLE = new BlockPos(1, 1, 1);
    private static final BlockPos LAST = new BlockPos(2, 1, 1);

    private HeatFluidGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pipeShapesContactDamageAndInsulationMatchLegacy(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.HEAT_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(LAST, ModNetworkBlocks.INSULATED_HEAT_PIPE.get());
        HeatPipeBlockEntity raw = require(helper, FIRST, HeatPipeBlockEntity.class);
        IronPipeBlockEntity fluid = require(helper, MIDDLE, IronPipeBlockEntity.class);
        HeatPipeBlockEntity insulated = require(helper, LAST, HeatPipeBlockEntity.class);

        var rawBounds = raw.getBlockState().getShape(
                helper.getLevel(), raw.getBlockPos(), CollisionContext.empty()
        ).bounds();
        var insulatedBounds = insulated.getBlockState().getShape(
                helper.getLevel(), insulated.getBlockPos(), CollisionContext.empty()
        ).bounds();
        var fluidBounds = fluid.getBlockState().getShape(
                helper.getLevel(), fluid.getBlockPos(), CollisionContext.empty()
        ).bounds();
        helper.assertTrue(close(rawBounds.minX, 4.0D / 16.0D), "Raw heat pipe did not keep its 4px inset");
        helper.assertTrue(close(rawBounds.maxX, 12.0D / 16.0D), "Raw heat pipe width changed");
        helper.assertTrue(close(insulatedBounds.minX, 3.0D / 16.0D), "Insulated pipe did not keep its 3px inset");
        helper.assertTrue(close(insulatedBounds.maxX, 13.0D / 16.0D), "Insulated pipe width changed");
        helper.assertTrue(close(fluidBounds.minX, 4.0D / 16.0D), "Iron fluid pipe did not keep its 4px inset");
        helper.assertTrue(close(fluidBounds.maxX, 12.0D / 16.0D), "Iron fluid pipe width changed");

        raw.heat().node().setTemperature(354.15D);
        insulated.heat().node().setTemperature(354.15D);
        Player rawContact = helper.makeMockSurvivalPlayer();
        Player insulatedContact = helper.makeMockSurvivalPlayer();
        ModNetworkBlocks.HEAT_PIPE.get().entityInside(
                raw.getBlockState(), helper.getLevel(), raw.getBlockPos(), rawContact
        );
        ModNetworkBlocks.INSULATED_HEAT_PIPE.get().entityInside(
                insulated.getBlockState(), helper.getLevel(), insulated.getBlockPos(), insulatedContact
        );
        helper.assertTrue(close(rawContact.getHealth(), 18.0D), "Raw pipe did not deal the legacy 2 damage above 80 C");
        helper.assertTrue(close(insulatedContact.getHealth(), 20.0D), "Insulated pipe dealt contact damage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void heatSinkHasSixThinShapesAndBlocksOnlyItsRearSide(GameTestHelper helper) {
        HeatSinkBlock sinkBlock = (HeatSinkBlock) ModNetworkBlocks.HEAT_SINK.get();
        for (Direction facing : Direction.values()) {
            var state = sinkBlock.defaultBlockState().setValue(HeatSinkBlock.FACING, facing);
            helper.setBlock(MIDDLE, state);
            var bounds = state.getShape(
                    helper.getLevel(), helper.absolutePos(MIDDLE), CollisionContext.empty()
            ).bounds();
            double thickness = switch (facing.getAxis()) {
                case X -> bounds.getXsize();
                case Y -> bounds.getYsize();
                case Z -> bounds.getZsize();
            };
            helper.assertTrue(close(thickness, 5.0D / 16.0D), "Heat sink shape is not 5/16 thick for " + facing);
        }

        helper.setBlock(FIRST, ModNetworkBlocks.HEAT_PIPE.get());
        helper.setBlock(MIDDLE, sinkBlock.defaultBlockState().setValue(HeatSinkBlock.FACING, Direction.EAST));
        helper.setBlock(LAST, ModNetworkBlocks.HEAT_PIPE.get());
        PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
        helper.runAfterDelay(2, () -> {
            BlockPos sink = helper.absolutePos(MIDDLE);
            helper.assertTrue(
                    manager.component(NetworkDomain.HEAT, sink).size() == 2,
                    "Heat sink did not connect through its front side"
            );
            helper.assertTrue(
                    manager.component(NetworkDomain.HEAT, helper.absolutePos(FIRST)).size() == 1,
                    "Heat sink connected through its blocked rear side"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void heatSinkDissipatesOneTemperatureDeltaPerTick(GameTestHelper helper) {
        helper.setBlock(MIDDLE, ModNetworkBlocks.HEAT_SINK.get());
        HeatSinkBlockEntity sink = require(helper, MIDDLE, HeatSinkBlockEntity.class);
        sink.heat().node().setTemperature(400.0D);
        double before = sink.heat().node().internalEnergyJoules();

        helper.runAfterDelay(1, () -> {
            double removed = before - sink.heat().node().internalEnergyJoules();
            helper.assertTrue(
                    Math.abs(removed - (400.0D - 298.15D)) < 1.0E-6D,
                    "Heat sink did not remove exactly one temperature delta: " + removed
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void ironPipeCapabilityAggregatesLoadedMembersAndTracksSplits(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(LAST, ModNetworkBlocks.IRON_PIPE.get());
        IronPipeBlockEntity first = require(helper, FIRST, IronPipeBlockEntity.class);
        IronPipeBlockEntity middle = require(helper, MIDDLE, IronPipeBlockEntity.class);
        IronPipeBlockEntity last = require(helper, LAST, IronPipeBlockEntity.class);

        helper.runAfterDelay(1, () -> {
            IFluidHandler component = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(component.getTanks() == 3, "Passive pipe side did not expose all loaded members");
            for (int tank = 0; tank < component.getTanks(); tank++) {
                helper.assertTrue(component.getTankCapacity(tank) == 160, "A pipe member lost its 160mB capacity");
            }
            FluidStack water = new FluidStack(Fluids.WATER, 480);
            helper.assertTrue(component.fill(water, IFluidHandler.FluidAction.SIMULATE) == 480, "Aggregate fill simulation failed");
            helper.assertTrue(total(first, middle, last) == 0, "Aggregate simulation mutated a member");
            helper.assertTrue(component.fill(water, IFluidHandler.FluidAction.EXECUTE) == 480, "Aggregate fill failed");
            helper.assertTrue(total(first, middle, last) == 480, "Aggregate fill lost fluid");
            helper.assertTrue(component.drain(200, IFluidHandler.FluidAction.SIMULATE).getAmount() == 200,
                    "Aggregate drain simulation failed");
            helper.assertTrue(total(first, middle, last) == 480, "Aggregate drain simulation mutated a member");

            middle.setRemoved();
            helper.assertTrue(component.getTanks() == 1, "Cached capability retained an unloaded member");
            IFluidHandler lastComponent = last.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.EAST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(lastComponent.getTanks() == 1, "Unload did not split the loaded graph");
            middle.clearRemoved();
            middle.onLoad();
            helper.assertTrue(component.getTanks() == 3, "Reloaded member did not rejoin the aggregate view");
            helper.assertTrue(total(first, middle, last) == 480, "Unload/reload changed authoritative fluid");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void passivePipeInputPullsAtLegacyRate(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(FIRST.above(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        IronPipeBlockEntity pipe = require(helper, FIRST, IronPipeBlockEntity.class);
        SingleBlockMachineBlockEntity source = require(helper, FIRST.above(), SingleBlockMachineBlockEntity.class);
        source.primaryTank().tank().fill(
                new FluidStack(Fluids.WATER, 320),
                IFluidHandler.FluidAction.EXECUTE
        );

        helper.assertTrue(pipe.pipe().sideMode(Direction.UP) == FluidPipeModule.SideMode.PASSIVE,
                "New pipe side was not passive");
        helper.assertTrue(pipe.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "Passive side did not expose its aggregate capability");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(pipe.pipe().node().amount() == 160,
                    "Passive side did not pull at the 160mB/t legacy rate");
            helper.assertTrue(source.primaryTank().tank().getFluidAmount() == 160,
                    "Passive side pulled the wrong amount from its source");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void activePipeOutputIsRateLimitedAndNeverExposesCapability(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(LAST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(FIRST.above(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        IronPipeBlockEntity first = require(helper, FIRST, IronPipeBlockEntity.class);
        IFluidHandler input = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        IFluidHandler cachedActiveSide = first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        input.fill(new FluidStack(Fluids.WATER, 320), IFluidHandler.FluidAction.EXECUTE);
        first.pipe().setSideMode(Direction.UP, FluidPipeModule.SideMode.ACTIVE);

        helper.assertFalse(first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                "Active side exposed a capability");
        helper.assertTrue(cachedActiveSide.drain(160, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "Cached passive view bypassed an active side");
        helper.runAfterDelay(1, () -> {
            SingleBlockMachineBlockEntity target = require(helper, FIRST.above(), SingleBlockMachineBlockEntity.class);
            helper.assertTrue(target.primaryTank().tank().getFluidAmount() == 160,
                    "Active side violated the 160mB/t legacy rate");
            first.pipe().setSideMode(Direction.UP, FluidPipeModule.SideMode.DISABLED);
            helper.assertFalse(first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(),
                    "Disabled side exposed a capability");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void activePipeOutputCommitsOnlyTheAcceptedAmount(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(MIDDLE, ModNetworkBlocks.IRON_PIPE.get());
        helper.setBlock(FIRST.above(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        IronPipeBlockEntity first = require(helper, FIRST, IronPipeBlockEntity.class);
        IronPipeBlockEntity middle = require(helper, MIDDLE, IronPipeBlockEntity.class);
        SingleBlockMachineBlockEntity target = require(helper, FIRST.above(), SingleBlockMachineBlockEntity.class);
        first.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new)
                .fill(new FluidStack(Fluids.WATER, 160), IFluidHandler.FluidAction.EXECUTE);
        target.primaryTank().tank().fill(
                new FluidStack(Fluids.WATER, 31_900),
                IFluidHandler.FluidAction.EXECUTE
        );
        first.pipe().setSideMode(Direction.UP, FluidPipeModule.SideMode.ACTIVE);

        helper.runAfterDelay(1, () -> {
            helper.assertTrue(target.primaryTank().tank().getFluidAmount() == 32_000,
                    "Active pipe did not fill the target's exact remaining capacity");
            helper.assertTrue(total(first, middle) == 60,
                    "Active pipe removed more than the target accepted");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void smallTankExportsAllFluidAndDescribesPortableContents(GameTestHelper helper) {
        helper.setBlock(MIDDLE, ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        helper.setBlock(MIDDLE.below(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        SingleBlockMachineBlockEntity source = require(helper, MIDDLE, SingleBlockMachineBlockEntity.class);
        source.primaryTank().tank().fill(
                new FluidStack(Fluids.WATER, 12_345),
                IFluidHandler.FluidAction.EXECUTE
        );
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModNetworkItems.WRENCH.get()));
        BlockPos absolute = helper.absolutePos(MIDDLE);
        source.interact(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );

        ItemStack portable = new ItemStack(ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        source.saveTankToItem(portable);
        helper.assertTrue(portable.getItem() instanceof SmallTankBlockItem, "Small tank did not use its portable item type");
        helper.assertTrue(portable.getTagElement("BlockEntityTag").getBoolean("tank_export_enabled"),
                "Small tank item lost its exporter state");
        List<Component> tooltip = new ArrayList<>();
        portable.getItem().appendHoverText(portable, null, tooltip, TooltipFlag.NORMAL);
        helper.assertTrue(tooltip.stream().map(Component::getString).anyMatch(line -> line.contains("12345")),
                "Small tank tooltip did not show the stored amount");

        helper.runAfterDelay(1, () -> {
            SingleBlockMachineBlockEntity target = require(helper, MIDDLE.below(), SingleBlockMachineBlockEntity.class);
            helper.assertTrue(source.primaryTank().tank().isEmpty(), "Small tank retained fluid after full export");
            helper.assertTrue(target.primaryTank().tank().getFluidAmount() == 12_345,
                    "Small tank exporter did not move the full accepted amount in one tick");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void smallTankExporterBackpressuresAFullTarget(GameTestHelper helper) {
        helper.setBlock(MIDDLE, ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        helper.setBlock(MIDDLE.below(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        SingleBlockMachineBlockEntity source = require(helper, MIDDLE, SingleBlockMachineBlockEntity.class);
        SingleBlockMachineBlockEntity target = require(helper, MIDDLE.below(), SingleBlockMachineBlockEntity.class);
        source.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        target.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 32_000), IFluidHandler.FluidAction.EXECUTE);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModNetworkItems.WRENCH.get()));
        BlockPos absolute = helper.absolutePos(MIDDLE);
        source.interact(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );

        helper.runAfterDelay(1, () -> {
            helper.assertTrue(source.primaryTank().tank().getFluidAmount() == 500,
                    "Full target did not backpressure the small tank exporter");
            helper.assertTrue(target.primaryTank().tank().getFluidAmount() == 32_000,
                    "Backpressure changed the full target");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void smallTankExporterCommitsOnlyPartialAcceptance(GameTestHelper helper) {
        helper.setBlock(MIDDLE, ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        helper.setBlock(MIDDLE.below(), ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        SingleBlockMachineBlockEntity source = require(helper, MIDDLE, SingleBlockMachineBlockEntity.class);
        SingleBlockMachineBlockEntity target = require(helper, MIDDLE.below(), SingleBlockMachineBlockEntity.class);
        source.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
        target.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 31_700), IFluidHandler.FluidAction.EXECUTE);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModNetworkItems.WRENCH.get()));
        BlockPos absolute = helper.absolutePos(MIDDLE);
        source.interact(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(source.primaryTank().tank().getFluidAmount() == 200,
                    "Partial acceptance removed too much source fluid");
            helper.assertTrue(target.primaryTank().tank().getFluidAmount() == 32_000,
                    "Partial acceptance inserted the wrong amount");
            helper.succeed();
        });
    }

    private static int total(IronPipeBlockEntity... pipes) {
        int total = 0;
        for (IronPipeBlockEntity pipe : pipes) {
            total += pipe.pipe().node().amount();
        }
        return total;
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1.0E-6D;
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
