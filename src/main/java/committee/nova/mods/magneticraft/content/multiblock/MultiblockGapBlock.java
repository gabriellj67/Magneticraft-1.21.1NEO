package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new MultiblockGapBlockEntity(position, state);
    }
}
