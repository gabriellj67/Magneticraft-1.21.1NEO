package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import org.jetbrains.annotations.Nullable;

public final class ElectricConnectorBlock extends WallMountedElectricBlock {
    public ElectricConnectorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new ElectricConnectorBlockEntity(position, state);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            LongDistanceElectricityService.get(serverLevel).removeConnectionsAt(position);
        }
        super.onRemove(oldState, level, position, newState, moving);
    }
}
