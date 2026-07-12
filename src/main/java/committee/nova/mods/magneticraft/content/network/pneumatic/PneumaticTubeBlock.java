package committee.nova.mods.magneticraft.content.network.pneumatic;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PneumaticTubeBlock extends ConduitBlock {
    public PneumaticTubeBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new PneumaticTubeBlockEntity(position, state);
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.PNEUMATIC_TUBE.get())
                || neighbor.is(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get());
    }
}
