package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Objects;

/**
 * Applies mirror-before-rotation transforms around a definition's controller anchor.
 */
public final class MultiblockTransform {
    private MultiblockTransform() {
    }

    public static StructureOffset relative(
            StructureOffset cell,
            StructureOffset center,
            Direction facing,
            boolean mirrored
    ) {
        Objects.requireNonNull(cell);
        Objects.requireNonNull(center);
        Objects.requireNonNull(facing);
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Multiblock facing must be horizontal: " + facing);
        }

        int x = cell.x() - center.x();
        int y = cell.y() - center.y();
        int z = cell.z() - center.z();
        if (mirrored) {
            x = -x;
        }
        return switch (facing) {
            case NORTH -> new StructureOffset(x, y, z);
            case EAST -> new StructureOffset(-z, y, x);
            case SOUTH -> new StructureOffset(-x, y, -z);
            case WEST -> new StructureOffset(z, y, -x);
            default -> throw new IllegalArgumentException("Multiblock facing must be horizontal: " + facing);
        };
    }

    public static BlockPos worldPosition(
            BlockPos controller,
            StructureOffset cell,
            StructureOffset center,
            Direction facing,
            boolean mirrored
    ) {
        StructureOffset relative = relative(cell, center, facing, mirrored);
        return controller.offset(relative.x(), relative.y(), relative.z());
    }
}
