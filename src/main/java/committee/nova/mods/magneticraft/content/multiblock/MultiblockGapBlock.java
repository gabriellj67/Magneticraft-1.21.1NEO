package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Invisible formed-structure proxy used by the released 1.12 multiblock renderer. */
public final class MultiblockGapBlock extends Block {
    public MultiblockGapBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
