package committee.nova.mods.magneticraft.content.network.pressure;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BrassPressurePipeBlock extends ConduitBlock {
    public BrassPressurePipeBlock(Properties properties) {
        super(properties, 5);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new BrassPressurePipeBlockEntity(position, state);
    }

    @Override
    protected NetworkDomain connectionDomain() {
        return NetworkDomain.PRESSURE;
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.BRASS_PRESSURE_PIPE.get());
    }
}
