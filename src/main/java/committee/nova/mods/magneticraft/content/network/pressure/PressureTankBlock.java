package committee.nova.mods.magneticraft.content.network.pressure;
import com.mojang.serialization.MapCodec;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class PressureTankBlock extends NetworkComponentBlock {
    public static final MapCodec<PressureTankBlock> CODEC = simpleCodec(PressureTankBlock::new);

    @Override
    protected MapCodec<? extends PressureTankBlock> codec() {
        return CODEC;
    }
    public PressureTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof PressureTankBlockEntity tank) {
            serverPlayer.openMenu(tank, position);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new PressureTankBlockEntity(position, state);
    }
}
