package committee.nova.mods.magneticraft.gametest;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockCell;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockRule;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockStructureFiller;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiblockAdminCommandGameTests {
    private static final String TEMPLATE = "advanced_systems";
    private static final String FILL_COMMAND = "magneticraft multiblock fill";
    private static final BlockPos CONTROLLER = new BlockPos(16, 2, 16);

    private MultiblockAdminCommandGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void fillerCompletesEveryDefinitionAndFacing(GameTestHelper helper) {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                AdvancedMultiblockBlockEntity controller = placeController(helper, definition, facing);

                MultiblockStructureFiller.Result result = MultiblockStructureFiller.fill(
                        helper.getLevel(),
                        controller
                );

                helper.assertTrue(result.success(), definition.id() + " " + facing + " was unavailable");
                helper.assertTrue(result.changedBlocks() > 0,
                        definition.id() + " " + facing + " did not place any blocks");
                helper.assertTrue(controller.validate().valid(),
                        definition.id() + " " + facing + " did not validate after filling");
                assertDefinition(helper, definition, facing);
                clearDefinition(helper, definition, CONTROLLER, facing);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void operatorFillsTargetedController(GameTestHelper helper) {
        AdvancedMultiblockBlockEntity controller = placeController(
                helper,
                MultiblockDefinition.HYDRAULIC_PRESS,
                Direction.EAST
        );
        ServerPlayer player = targetedPlayer(helper, CONTROLLER);

        int result = execute(helper, player, 2);

        helper.assertTrue(result == 1, "Operator fill command did not report success");
        helper.assertTrue(!controller.formed(), "Operator fill command bypassed normal structure formation");
        helper.assertTrue(controller.validate().valid(), "Operator fill command did not complete the structure");
        clearDefinition(helper, controller.definition(), CONTROLLER, controller.facing());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void nonOperatorCannotFillTargetedController(GameTestHelper helper) {
        AdvancedMultiblockBlockEntity controller = placeController(
                helper,
                MultiblockDefinition.HYDRAULIC_PRESS,
                Direction.EAST
        );
        ServerPlayer player = targetedPlayer(helper, CONTROLLER);

        int result = execute(helper, player, 1);

        helper.assertTrue(result == 0, "Non-operator fill command unexpectedly reported success");
        helper.assertTrue(!controller.formed(), "Non-operator fill command formed the structure");
        helper.assertTrue(!controller.validate().valid(), "Non-operator fill command changed structure blocks");
        clearDefinition(helper, controller.definition(), CONTROLLER, controller.facing());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void operatorWithoutControllerTargetDoesNotChangeWorld(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, Blocks.STONE);
        ServerPlayer player = targetedPlayer(helper, CONTROLLER);

        int result = execute(helper, player, 2);

        helper.assertTrue(result == 0, "Fill command accepted a non-controller target");
        helper.assertTrue(helper.getBlockState(CONTROLLER).is(Blocks.STONE),
                "Fill command changed a non-controller target");
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.succeed();
    }

    private static AdvancedMultiblockBlockEntity placeController(
            GameTestHelper helper,
            MultiblockDefinition definition,
            Direction facing
    ) {
        helper.setBlock(
                CONTROLLER,
                ModAdvancedBlocks.controller(definition).get().defaultBlockState()
                        .setValue(AdvancedMultiblockBlock.FACING, facing)
        );
        var blockEntity = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(blockEntity instanceof AdvancedMultiblockBlockEntity, "Missing multiblock controller");
        return (AdvancedMultiblockBlockEntity) blockEntity;
    }

    private static ServerPlayer targetedPlayer(GameTestHelper helper, BlockPos localTarget) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "multiblock-command-test")
        );
        BlockPos target = helper.absolutePos(localTarget);
        BlockPos standing = target.relative(Direction.SOUTH, 8);
        player.setPos(standing.getX() + 0.5D, standing.getY(), standing.getZ() + 0.5D);
        player.setYRot(180.0F);
        player.setYHeadRot(180.0F);
        player.setXRot(8.0F);
        return player;
    }

    private static int execute(GameTestHelper helper, ServerPlayer player, int permissionLevel) {
        return helper.getLevel().getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(permissionLevel).withSuppressedOutput(),
                FILL_COMMAND
        );
    }

    private static void assertDefinition(
            GameTestHelper helper,
            MultiblockDefinition definition,
            Direction facing
    ) {
        for (MultiblockCell cell : definition.requiredCells()) {
            BlockPos position = MultiblockTransform.worldPosition(
                    CONTROLLER,
                    cell.offset(),
                    definition.center(),
                    facing,
                    false
            );
            if (cell.rule() == MultiblockRule.CONTROLLER) {
                helper.assertTrue(helper.getBlockState(position).is(ModAdvancedBlocks.controller(definition).get()),
                        definition.id() + " replaced its controller");
            } else {
                helper.assertTrue(cell.rule().matches(
                                helper.getBlockState(position),
                                ModAdvancedBlocks.controller(definition).get(),
                                facing
                        ), definition.id() + " mismatched " + cell.rule() + " at " + position);
            }
        }
    }

    private static void clearDefinition(
            GameTestHelper helper,
            MultiblockDefinition definition,
            BlockPos controller,
            Direction facing
    ) {
        for (MultiblockCell cell : definition.requiredCells()) {
            BlockPos position = MultiblockTransform.worldPosition(
                    controller,
                    cell.offset(),
                    definition.center(),
                    facing,
                    false
            );
            helper.setBlock(position, Blocks.AIR);
        }
    }
}
