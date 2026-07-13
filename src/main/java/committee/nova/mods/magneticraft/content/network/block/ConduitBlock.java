package committee.nova.mods.magneticraft.content.network.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Compact conduit collision shape with arms only toward compatible neighbors.
 */
public abstract class ConduitBlock extends NetworkComponentBlock {
    private final VoxelShape center;
    private final VoxelShape[] arms;

    protected ConduitBlock(Properties properties) {
        this(properties, 5);
    }

    protected ConduitBlock(Properties properties, int insetPixels) {
        super(properties);
        if (insetPixels <= 0 || insetPixels >= 8) {
            throw new IllegalArgumentException("Conduit inset must be between 1 and 7 pixels");
        }
        int far = 16 - insetPixels;
        center = Block.box(insetPixels, insetPixels, insetPixels, far, far, far);
        arms = new VoxelShape[]{
                Block.box(insetPixels, 0, insetPixels, far, insetPixels, far),
                Block.box(insetPixels, far, insetPixels, far, 16, far),
                Block.box(insetPixels, insetPixels, 0, far, far, insetPixels),
                Block.box(insetPixels, insetPixels, far, far, far, 16),
                Block.box(0, insetPixels, insetPixels, insetPixels, far, far),
                Block.box(far, insetPixels, insetPixels, 16, far, far)
        };
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        VoxelShape shape = center;
        for (Direction direction : Direction.values()) {
            if (connectsVisuallyTo(level.getBlockState(position.relative(direction)))) {
                shape = Shapes.or(shape, arms[direction.ordinal()]);
            }
        }
        return shape;
    }

    protected abstract boolean connectsVisuallyTo(BlockState neighbor);
}
