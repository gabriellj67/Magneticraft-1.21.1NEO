package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Invisible formed-structure proxy used by the released 1.12 multiblock renderer. */
public final class MultiblockGapBlock extends BaseEntityBlock {
    public MultiblockGapBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return shape(level, position);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return shape(level, position);
    }

    private static VoxelShape shape(BlockGetter level, BlockPos position) {
        return level.getBlockEntity(position) instanceof MultiblockGapBlockEntity gap
                ? gap.collisionShape()
                : Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new MultiblockGapBlockEntity(position, state);
    }
}
