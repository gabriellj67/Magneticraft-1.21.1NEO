package committee.nova.mods.magneticraft.content.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** World-space bounds for scalable nuclear multiblock scenes anchored at a front controller. */
public final class NuclearMultiblockBounds {
    private static final double MODEL_OVERHANG = 0.5D;

    private NuclearMultiblockBounds() {
    }

    public static AABB renderBounds(
            BlockPos controller,
            Direction facing,
            int width,
            int height,
            int length
    ) {
        if (width <= 0 || height <= 0 || length <= 0) {
            return new AABB(controller);
        }
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Nuclear multiblock facing must be horizontal: " + facing);
        }

        Direction right = facing.getClockWise();
        Direction inward = facing.getOpposite();
        int halfWidth = Math.floorDiv(width, 2);
        BlockPos first = controller.relative(right, -halfWidth).below();
        BlockPos opposite = controller
                .relative(right, width - halfWidth - 1)
                .relative(inward, length - 1)
                .above(height - 2);

        return new AABB(
                Math.min(first.getX(), opposite.getX()),
                Math.min(first.getY(), opposite.getY()),
                Math.min(first.getZ(), opposite.getZ()),
                Math.max(first.getX(), opposite.getX()) + 1.0D,
                Math.max(first.getY(), opposite.getY()) + 1.0D,
                Math.max(first.getZ(), opposite.getZ()) + 1.0D
        ).inflate(MODEL_OVERHANG);
    }
}
