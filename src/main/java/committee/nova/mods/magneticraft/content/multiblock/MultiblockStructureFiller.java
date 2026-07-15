package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Server-side exact structure placement used by operator tooling. */
public final class MultiblockStructureFiller {
    private MultiblockStructureFiller() {
    }

    public static Result fill(ServerLevel level, AdvancedMultiblockBlockEntity controller) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(controller);
        if (controller.getLevel() != level) {
            throw new IllegalArgumentException("Multiblock controller is not in the supplied level");
        }

        List<Placement> placements = new ArrayList<>();
        for (MultiblockCell cell : controller.definition().requiredCells()) {
            if (cell.rule() == MultiblockRule.CONTROLLER) {
                continue;
            }
            BlockPos position = MultiblockTransform.worldPosition(
                    controller.getBlockPos(),
                    cell.offset(),
                    controller.definition().center(),
                    controller.facing(),
                    controller.mirrored()
            );
            if (level.isOutsideBuildHeight(position)
                    || !level.getWorldBorder().isWithinBounds(position)
                    || !level.getChunkSource().hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                return Result.unavailable(position);
            }
            placements.add(new Placement(position, cell.rule().previewState(controller.facing())));
        }

        int changedBlocks = 0;
        for (Placement placement : placements) {
            if (level.getBlockState(placement.position()).equals(placement.state())) {
                continue;
            }
            if (level.setBlock(placement.position(), placement.state(), Block.UPDATE_ALL)) {
                changedBlocks++;
            }
        }
        return Result.success(changedBlocks);
    }

    private record Placement(BlockPos position, BlockState state) {
    }

    public record Result(int changedBlocks, @Nullable BlockPos unavailablePosition) {
        public boolean success() {
            return unavailablePosition == null;
        }

        private static Result success(int changedBlocks) {
            return new Result(changedBlocks, null);
        }

        private static Result unavailable(BlockPos position) {
            return new Result(0, position.immutable());
        }
    }
}
