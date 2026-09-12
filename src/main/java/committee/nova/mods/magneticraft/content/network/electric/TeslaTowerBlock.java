package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Three-block tower; only the bottom part owns simulation state. */
public final class TeslaTowerBlock extends NetworkComponentBlock {
    public static final EnumProperty<TeslaTowerPart> PART = EnumProperty.create("part", TeslaTowerPart.class);
    private static final int HEIGHT = 3;
    private static final ThreadLocal<Set<BlockPos>> STRUCTURE_UPDATES = ThreadLocal.withInitial(HashSet::new);

    public TeslaTowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, TeslaTowerPart.BOTTOM));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (int height = 0; height < HEIGHT; height++) {
            BlockPos position = context.getClickedPos().above(height);
            if (context.getLevel().isOutsideBuildHeight(position)
                    || !context.getLevel().getBlockState(position).canBeReplaced()) {
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos position,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, position, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        Set<BlockPos> updates = STRUCTURE_UPDATES.get();
        updates.add(position);
        try {
            for (int height = 0; height < HEIGHT; height++) {
                level.setBlock(
                        position.above(height),
                        defaultBlockState().setValue(PART, TeslaTowerPart.atHeight(height)),
                        Block.UPDATE_ALL
                );
            }
        } finally {
            updates.remove(position);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return state.getValue(PART) == TeslaTowerPart.BOTTOM
                ? new TeslaTowerBlockEntity(position, state)
                : null;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockPos bottom = position.below(oldState.getValue(PART).height());
            Set<BlockPos> updates = STRUCTURE_UPDATES.get();
            if (!level.isClientSide && updates.add(bottom)) {
                try {
                    if (!position.equals(bottom) && level.getBlockState(bottom).is(this)) {
                        level.destroyBlock(bottom, true);
                    }
                    for (int height = 0; height < HEIGHT; height++) {
                        BlockPos member = bottom.above(height);
                        if (!member.equals(position) && !member.equals(bottom) && level.getBlockState(member).is(this)) {
                            level.removeBlock(member, false);
                        }
                    }
                } finally {
                    updates.remove(bottom);
                }
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }
}
