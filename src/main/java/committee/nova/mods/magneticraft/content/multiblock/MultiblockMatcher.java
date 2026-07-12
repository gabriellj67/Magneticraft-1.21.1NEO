package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only structure validation that never forces a chunk load.
 */
public final class MultiblockMatcher {
    private MultiblockMatcher() {
    }

    public static MultiblockValidationResult validate(
            Level level,
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockDefinition definition,
            Block controllerBlock
    ) {
        for (MultiblockCell cell : definition.requiredCells()) {
            BlockPos worldPosition = MultiblockTransform.worldPosition(
                    controller,
                    cell.offset(),
                    definition.center(),
                    facing,
                    mirrored
            );
            if (!level.hasChunkAt(worldPosition)) {
                return MultiblockValidationResult.unloaded(worldPosition, cell.rule());
            }
            if (!cell.rule().matches(level.getBlockState(worldPosition), controllerBlock, facing)) {
                return MultiblockValidationResult.mismatch(worldPosition, cell.rule());
            }
        }
        return MultiblockValidationResult.validResult();
    }

    public static List<BlockPos> memberPositions(
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockDefinition definition
    ) {
        List<BlockPos> positions = new ArrayList<>(definition.memberCells().size());
        for (MultiblockCell cell : definition.memberCells()) {
            positions.add(MultiblockTransform.worldPosition(
                    controller,
                    cell.offset(),
                    definition.center(),
                    facing,
                    mirrored
            ));
        }
        return List.copyOf(positions);
    }
}
