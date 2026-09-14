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

public final class MiningRobotBlock extends ProgrammableBlock {
    public static final MapCodec<MiningRobotBlock> CODEC = simpleCodec(MiningRobotBlock::new);

    @Override
    protected MapCodec<? extends MiningRobotBlock> codec() {
        return CODEC;
    }
    public MiningRobotBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof MiningRobotBlockEntity robot && !robot.isRelocating()) {
                robot.dropContents(level);
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new MiningRobotBlockEntity(position, state);
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
                : createTickerHelper(
                        type,
                        ModComputerContent.MINING_ROBOT_BLOCK_ENTITY.get(),
                        MiningRobotBlockEntity::serverTick
                );
    }
}
