package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

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
        return validate(
                controller,
                facing,
                mirrored,
                definition,
                position -> level.getChunkSource().hasChunk(
                        position.getX() >> 4,
                        position.getZ() >> 4
                ),
                (cell, position) -> {
                    if (level.getBlockState(position).is(ModAdvancedBlocks.MULTIBLOCK_GAP.get())
                            && level instanceof ServerLevel serverLevel) {
                        return controller.equals(
                                MultiblockMembershipService.controllerAt(serverLevel, position)
                        );
                    }
                    return cell.rule().matches(
                            level.getBlockState(position),
                            controllerBlock,
                            facing
                    );
                }
        );
    }

    static MultiblockValidationResult validate(
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockDefinition definition,
            Predicate<BlockPos> chunkLoaded,
            BiPredicate<MultiblockCell, BlockPos> cellMatches
    ) {
        for (MultiblockCell cell : definition.requiredCells()) {
            BlockPos worldPosition = MultiblockTransform.worldPosition(
                    controller,
                    cell.offset(),
                    definition.center(),
                    facing,
                    mirrored
            );
            if (!chunkLoaded.test(worldPosition)) {
                return MultiblockValidationResult.unloaded(worldPosition, cell.rule());
            }
            if (!cellMatches.test(cell, worldPosition)) {
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
