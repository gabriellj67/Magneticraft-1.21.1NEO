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
    private static final VoxelShape CENTER = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] ARMS = new VoxelShape[]{
            Block.box(5, 0, 5, 11, 5, 11),
            Block.box(5, 11, 5, 11, 16, 11),
            Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16),
            Block.box(0, 5, 5, 5, 11, 11),
            Block.box(11, 5, 5, 16, 11, 11)
    };

    protected ConduitBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        VoxelShape shape = CENTER;
        for (Direction direction : Direction.values()) {
            if (connectsVisuallyTo(level.getBlockState(position.relative(direction)))) {
                shape = Shapes.or(shape, ARMS[direction.ordinal()]);
            }
        }
        return shape;
    }

    protected abstract boolean connectsVisuallyTo(BlockState neighbor);
}
