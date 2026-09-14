package committee.nova.mods.magneticraft.content.network.electric;
import com.mojang.serialization.MapCodec;

import committee.nova.mods.magneticraft.content.item.TieredElectricalDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ElectricConnectorBlock extends WallMountedElectricBlock {
    public static final MapCodec<ElectricConnectorBlock> CODEC = simpleCodec(ElectricConnectorBlock::new);

    @Override
    protected MapCodec<? extends ElectricConnectorBlock> codec() {
        return CODEC;
    }
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

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return TieredElectricalDrops.preserveTier(
                new ArrayList<>(super.getDrops(state, builder)),
                asItem(),
                builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        );
    }
}
