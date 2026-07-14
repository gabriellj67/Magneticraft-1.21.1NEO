package committee.nova.mods.magneticraft.content.network.pneumatic;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
    protected NetworkDomain connectionDomain() {
        return NetworkDomain.LOGISTICS;
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.PNEUMATIC_TUBE.get())
                || neighbor.is(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get());
    }

    @Override
    protected boolean connectsToMachine(LevelAccessor level, BlockPos position, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        return super.connectsToMachine(level, position, side)
                || blockEntity instanceof NetworkConnectionHost host
                && host.supportsNetworkConnection(NetworkDomain.PRESSURE, side)
                || blockEntity != null && blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent();
    }
}
