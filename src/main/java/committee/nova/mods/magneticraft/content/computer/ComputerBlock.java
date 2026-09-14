package committee.nova.mods.magneticraft.content.computer;
import com.mojang.serialization.MapCodec;

import committee.nova.mods.magneticraft.init.ModComputerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ComputerBlock extends ProgrammableBlock {
    public static final MapCodec<ComputerBlock> CODEC = simpleCodec(ComputerBlock::new);

    @Override
    protected MapCodec<? extends ComputerBlock> codec() {
        return CODEC;
    }
    public ComputerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new ComputerBlockEntity(position, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, ModComputerContent.COMPUTER_BLOCK_ENTITY.get(), ComputerBlockEntity::serverTick);
    }
}
