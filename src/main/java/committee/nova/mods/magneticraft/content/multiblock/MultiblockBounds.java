package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Exact world-space bounds shared by structure previews and formed model culling. */
public final class MultiblockBounds {
    private MultiblockBounds() {
    }

    public static AABB worldBounds(
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockDefinition definition
    ) {
        BlockPos min = null;
        BlockPos max = null;
        for (MultiblockCell cell : definition.cells()) {
            BlockPos position = MultiblockTransform.worldPosition(
                    controller, cell.offset(), definition.center(), facing, mirrored
            );
            min = min == null ? position : new BlockPos(
                    Math.min(min.getX(), position.getX()),
                    Math.min(min.getY(), position.getY()),
                    Math.min(min.getZ(), position.getZ())
            );
            max = max == null ? position : new BlockPos(
                    Math.max(max.getX(), position.getX()),
                    Math.max(max.getY(), position.getY()),
                    Math.max(max.getZ(), position.getZ())
            );
        }
        if (min == null || max == null) {
            return new AABB(controller);
        }
        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D
        );
    }

    /** Includes small decorative model overhangs without changing structure validation. */
    public static AABB renderBounds(
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockDefinition definition
    ) {
        AABB structure = worldBounds(controller, facing, mirrored, definition);
        double topMargin = definition == MultiblockDefinition.SHELVING_UNIT ? 0.75D : 0.125D;
        return new AABB(
                structure.minX - 0.125D,
                structure.minY - 0.125D,
                structure.minZ - 0.125D,
                structure.maxX + 0.125D,
                structure.maxY + topMargin,
                structure.maxZ + 0.125D
        );
    }
}
