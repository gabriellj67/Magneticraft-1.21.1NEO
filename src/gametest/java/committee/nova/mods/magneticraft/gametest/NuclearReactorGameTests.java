package committee.nova.mods.magneticraft.gametest;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorColumnBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorStructure;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Loaded-world proof for manual formation and periodic structural invalidation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearReactorGameTests {
    private static final String TEMPLATE = "base_content";

    private NuclearReactorGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void variableReactorFormsThenUnformsAfterActuatorDamage(GameTestHelper helper) {
        int width = 7;
        int length = 7;
        int height = 7;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = columns(width, length);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    NuclearReactorStructure.ExpectedPart expected = NuclearReactorStructure.expectedPart(
                            width, length, height, x, y, z, columns, facing);
                    BlockPos position = NuclearReactorStructure.worldPosition(
                            controllerPosition, facing, width, x, y, z);
                    helper.getLevel().setBlock(position, state(expected), Block.UPDATE_ALL);
                }
            }
        }
        helper.assertTrue(helper.getLevel().getBlockEntity(controllerPosition)
                        instanceof NuclearReactorControllerBlockEntity,
                "Reactor controller block entity was not created");
        NuclearReactorControllerBlockEntity controller =
                (NuclearReactorControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        ServerPlayer operator = operator(helper, controllerPosition);
        helper.assertTrue(controller.tryForm(operator), "Complete variable reactor did not form");
        helper.assertTrue(controller.snapshot().orElseThrow().columns().size() == 9,
                "Reactor snapshot did not retain all logical columns");
        helper.assertTrue(controller.snapshot().orElseThrow().ports().size() == 4,
                "Reactor snapshot did not retain all public ports");
        helper.assertTrue(controller.estimate().powerDensityJoulesPerTick() > 0.0D,
                "Formed reactor did not calculate a static layout preview");

        ReactorColumnCoordinate control = new ReactorColumnCoordinate(1, 0);
        BlockPos actuator = NuclearReactorStructure.worldPosition(
                controllerPosition, facing, width, control.x() + 2, height - 1, control.z() + 2);
        helper.getLevel().destroyBlock(actuator, false);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!controller.formed(), "Damaged reactor remained formed after periodic validation");
            helper.assertTrue(!helper.getLevel().getBlockState(controllerPosition)
                            .getValue(NuclearReactorControllerBlock.FORMED),
                    "Controller block state remained formed after structure damage");
            helper.succeed();
        });
    }

    private static Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns(int width, int length) {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> result = new LinkedHashMap<>();
        NuclearReactorColumnType[] first = {
                NuclearReactorColumnType.FUEL_STANDARD,
                NuclearReactorColumnType.CONTROL_ROD_A,
                NuclearReactorColumnType.COOLANT_CHANNEL,
                NuclearReactorColumnType.INSTRUMENTATION
        };
        int index = 0;
        for (int z = 0; z < length - 4; z++) {
            for (int x = 0; x < width - 4; x++) {
                result.put(new ReactorColumnCoordinate(x, z), index < first.length
                        ? first[index]
                        : index % 2 == 0 ? NuclearReactorColumnType.FUEL_STANDARD
                        : NuclearReactorColumnType.REFLECTOR);
                index++;
            }
        }
        return result;
    }

    private static BlockState state(NuclearReactorStructure.ExpectedPart expected) {
        return switch (expected.kind()) {
            case CONTROLLER -> ModNuclearBlocks.REACTOR_CONTROLLER.get().defaultBlockState()
                    .setValue(NuclearReactorControllerBlock.FACING, expected.facing());
            case CONTAINMENT_CASING -> ModNuclearBlocks.REACTOR_CONTAINMENT_CASING.get().defaultBlockState();
            case PRESSURE_VESSEL -> ModNuclearBlocks.REACTOR_PRESSURE_VESSEL.get().defaultBlockState();
            case CONTROL_ROD_ACTUATOR -> ModNuclearBlocks.REACTOR_CONTROL_ROD_ACTUATOR.get().defaultBlockState();
            case COOLANT_PORT -> ModNuclearBlocks.REACTOR_MAIN_COOLANT_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case ELECTRICAL_PORT -> ModNuclearBlocks.REACTOR_ELECTRICAL_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case INSTRUMENTATION_PORT -> ModNuclearBlocks.REACTOR_INSTRUMENTATION_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case COLUMN_BASE -> ModNuclearBlocks.reactorColumn(expected.columnType()).get().defaultBlockState();
            case COLUMN_SEGMENT -> ModNuclearBlocks.REACTOR_COLUMN_SEGMENT.get().defaultBlockState();
        };
    }

    private static ServerPlayer operator(GameTestHelper helper, BlockPos controller) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "reactor-operator-test"));
        player.setPos(controller.getX() + 0.5D, controller.getY(), controller.getZ() + 2.5D);
        return player;
    }
}
