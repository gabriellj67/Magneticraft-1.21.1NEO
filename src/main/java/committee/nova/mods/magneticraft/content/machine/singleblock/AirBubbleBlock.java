package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Temporary non-solid water replacement maintained by an airlock.
 */
public final class AirBubbleBlock extends Block {
    public static final BooleanProperty DECAYING = BooleanProperty.create("decaying");
    private static final int OWNER_VALIDATION_INTERVAL_TICKS = 40;
    private static final int DECAY_DELAY_BOUND_TICKS = 20;

    public AirBubbleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DECAYING, false));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (state.getValue(DECAYING)) {
            AirBubbleOwnershipSavedData.get(level).unbindBubble(position);
            level.removeBlock(position, false);
            return;
        }
        validateOwner(level, position, state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        tick(state, level, position, random);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            AirBubbleOwnershipSavedData.get(serverLevel).unbindBubble(position);
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAYING);
    }

    static void scheduleOwnerValidation(ServerLevel level, BlockPos position) {
        level.scheduleTick(position, ModMachineBlocks.AIR_BUBBLE.get(), OWNER_VALIDATION_INTERVAL_TICKS);
    }

    static void beginDecay(ServerLevel level, BlockPos position, BlockState state) {
        AirBubbleOwnershipSavedData.get(level).unbindBubble(position);
        if (!state.is(ModMachineBlocks.AIR_BUBBLE.get())) {
            return;
        }
        if (!state.getValue(DECAYING)) {
            level.setBlock(position, state.setValue(DECAYING, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(
                position,
                ModMachineBlocks.AIR_BUBBLE.get(),
                1 + level.random.nextInt(DECAY_DELAY_BOUND_TICKS)
        );
    }

    private static void validateOwner(ServerLevel level, BlockPos position, BlockState state) {
        AirBubbleOwnershipSavedData ownership = AirBubbleOwnershipSavedData.get(level);
        BlockPos owner = ownership.ownerOf(position).orElse(null);
        if (owner == null) {
            beginDecay(level, position, state);
            return;
        }
        var ownerChunk = level.getChunkSource().getChunkNow(owner.getX() >> 4, owner.getZ() >> 4);
        if (ownerChunk == null) {
            scheduleOwnerValidation(level, position);
            return;
        }
        if (!(ownerChunk.getBlockEntity(owner) instanceof SingleBlockMachineBlockEntity machine)
                || machine.definition() != SingleBlockMachineDefinition.AIRLOCK) {
            beginDecay(level, position, state);
            return;
        }
        scheduleOwnerValidation(level, position);
    }
}
