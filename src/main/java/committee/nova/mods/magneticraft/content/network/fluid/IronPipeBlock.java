package committee.nova.mods.magneticraft.content.network.fluid;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public final class IronPipeBlock extends ConduitBlock {
    public IronPipeBlock(Properties properties) {
        super(properties, 4);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new IronPipeBlockEntity(position, state);
    }

    @Override
    protected NetworkDomain connectionDomain() {
        return NetworkDomain.FLUID;
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.IRON_PIPE.get());
    }

    @Override
    protected boolean connectsToMachine(LevelAccessor level, BlockPos position, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        return super.connectsToMachine(level, position, side)
                || blockEntity != null && blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent();
    }
}
