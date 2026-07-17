package committee.nova.mods.magneticraft.gametest;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalFacilityType;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortRole;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalStructureValidator;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Loaded-world checks for rotated structure claims, exact fluid faces and damage invalidation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearThermalGameTests {
    private static final String TEMPLATE = "base_content";

    private NuclearThermalGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void steamGeneratorFormsClaimsExactPortsAndUnformsAfterCoreDamage(GameTestHelper helper) {
        int width = 5;
        int length = 5;
        int height = 5;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        buildSteamGenerator(helper, controllerPosition, facing, width, length, height);
        NuclearThermalControllerBlockEntity controller =
                (NuclearThermalControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        helper.assertTrue(controller.tryForm(operator(helper, controllerPosition)),
                "Complete variable steam generator did not form");
        helper.assertTrue(controller.snapshot().orElseThrow().ports().size() == 5,
                "Steam generator snapshot lost one or more exact ports");

        BlockPos hotPosition = controller.snapshot().orElseThrow().ports()
                .get(NuclearThermalPortRole.HOT_COOLANT_INPUT);
        Direction hotSide = helper.getLevel().getBlockState(hotPosition).getValue(NuclearThermalPortBlock.FACING);
        IFluidHandler hot = helper.getLevel().getBlockEntity(hotPosition)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, hotSide).orElseThrow(AssertionError::new);
        helper.assertTrue(hot.fill(new FluidStack(
                ModFluids.get(FluidDefinition.HOT_REACTOR_COOLANT).source().get(), 1_000),
                IFluidHandler.FluidAction.EXECUTE) == 1_000, "Hot primary inlet rejected reactor coolant");
        helper.assertFalse(helper.getLevel().getBlockEntity(hotPosition)
                        .getCapability(ForgeCapabilities.FLUID_HANDLER, hotSide.getOpposite()).isPresent(),
                "Thermal port leaked its capability onto an unplanned face");

        BlockPos coldPosition = controller.snapshot().orElseThrow().ports()
                .get(NuclearThermalPortRole.COLD_COOLANT_OUTPUT);
        Direction coldSide = helper.getLevel().getBlockState(coldPosition).getValue(NuclearThermalPortBlock.FACING);
        IFluidHandler cold = helper.getLevel().getBlockEntity(coldPosition)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, coldSide).orElseThrow(AssertionError::new);
        helper.assertTrue(cold.fill(new FluidStack(
                ModFluids.get(FluidDefinition.COLD_REACTOR_COOLANT).source().get(), 100),
                IFluidHandler.FluidAction.EXECUTE) == 0, "Cold primary output accepted external input");

        BlockPos exchanger = NuclearThermalStructureValidator.worldPosition(
                controllerPosition, facing, width, width / 2, 2, 2);
        helper.getLevel().destroyBlock(exchanger, false);
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(controller.formed(), "Damaged steam generator remained formed");
            helper.assertFalse(helper.getLevel().getBlockState(controllerPosition)
                            .getValue(NuclearThermalControllerBlock.FORMED),
                    "Damaged steam generator retained its formed block state");
            helper.succeed();
        });
    }

    private static void buildSteamGenerator(
            GameTestHelper helper, BlockPos controller, Direction facing, int width, int length, int height
    ) {
        NuclearThermalFacilityType type = NuclearThermalFacilityType.STEAM_GENERATOR;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = NuclearThermalStructureValidator.worldPosition(
                            controller, facing, width, x, y, z);
                    NuclearThermalPortRole role = NuclearThermalStructureValidator.portAt(
                            type, width, length, height, x, y, z);
                    BlockState state;
                    if (x == width / 2 && y == 1 && z == 0) {
                        state = ModNuclearBlocks.thermalController(type).get().defaultBlockState()
                                .setValue(NuclearThermalControllerBlock.FACING, facing);
                    } else if (role != null) {
                        state = ModNuclearBlocks.NUCLEAR_THERMAL_PORT.get().defaultBlockState()
                                .setValue(NuclearThermalPortBlock.FACING, outward(facing, width, length, x, z));
                    } else if (x == 0 || x == width - 1 || y == 0 || y == height - 1
                            || z == 0 || z == length - 1) {
                        state = ModNuclearBlocks.FACILITY_CASING.get().defaultBlockState();
                    } else if (x == width / 2) {
                        state = ModNuclearBlocks.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    helper.getLevel().setBlock(position, state, Block.UPDATE_ALL);
                }
            }
        }
    }

    private static Direction outward(Direction facing, int width, int length, int x, int z) {
        if (z == 0) return facing;
        if (z == length - 1) return facing.getOpposite();
        if (x == 0) return facing.getCounterClockWise();
        if (x == width - 1) return facing.getClockWise();
        throw new IllegalArgumentException();
    }

    private static ServerPlayer operator(GameTestHelper helper, BlockPos controller) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "thermal-operator-test"));
        player.setPos(controller.getX() + 0.5D, controller.getY(), controller.getZ() + 2.5D);
        return player;
    }
}
