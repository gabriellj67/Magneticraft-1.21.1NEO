package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Loaded-world regression coverage for models that must not hide adjacent block faces. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModelOcclusionGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos CENTER = new BlockPos(1, 2, 1);

    private ModelOcclusionGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void partialAndTransparentModelsKeepAdjacentFacesVisible(GameTestHelper helper) {
        List<Block> models = List.of(
                ModMachineBlocks.CRUSHING_TABLE.get(),
                ModMachineBlocks.BATTERY.get(),
                ModMachineBlocks.PERMANENT_MAGNET.get(),
                ModNetworkBlocks.PRESSURE_TANK.get(),
                ModComputerContent.COMPUTER.get(),
                ModComputerContent.MINING_ROBOT.get(),
                machine(SingleBlockMachineDefinition.SLUICE_BOX),
                machine(SingleBlockMachineDefinition.SMALL_TANK),
                machine(SingleBlockMachineDefinition.FEEDING_TROUGH),
                machine(SingleBlockMachineDefinition.INSERTER),
                machine(SingleBlockMachineDefinition.COMBUSTION_CHAMBER),
                machine(SingleBlockMachineDefinition.STEAM_BOILER),
                machine(SingleBlockMachineDefinition.GASIFICATION_UNIT),
                machine(SingleBlockMachineDefinition.ELECTRIC_ENGINE),
                machine(SingleBlockMachineDefinition.INTERNAL_COMBUSTION_ENGINE)
        );

        helper.setBlock(CENTER, Blocks.STONE);
        BlockPos absoluteCenter = helper.absolutePos(CENTER);
        for (Block model : models) {
            for (Direction direction : Direction.values()) {
                BlockPos relativeNeighbor = CENTER.relative(direction);
                helper.setBlock(relativeNeighbor, model);
                BlockPos absoluteNeighbor = helper.absolutePos(relativeNeighbor);
                var neighborState = helper.getLevel().getBlockState(absoluteNeighbor);
                String modelId = model.getDescriptionId();

                helper.assertTrue(!neighborState.canOcclude(), modelId + " still participates in face occlusion");
                helper.assertTrue(
                        Block.shouldRenderFace(
                                Blocks.STONE.defaultBlockState(),
                                helper.getLevel(),
                                absoluteCenter,
                                direction,
                                absoluteNeighbor
                        ),
                        modelId + " hides the adjacent stone face toward " + direction.getName()
                );
            }
        }
        helper.succeed();
    }

    private static Block machine(SingleBlockMachineDefinition definition) {
        return ModMachineBlocks.machine(definition).get();
    }
}
