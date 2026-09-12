package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class WirelessEnergyReceiverBlock extends WallMountedElectricBlock {
    public WirelessEnergyReceiverBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new WirelessEnergyReceiverBlockEntity(position, state);
    }
}
