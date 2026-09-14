package committee.nova.mods.magneticraft.content.machine.singleblock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Six-way attachable full-bright industrial light.
 */
public final class TubeLightBlock extends DirectionalBlock {
    public static final MapCodec<TubeLightBlock> CODEC = simpleCodec(TubeLightBlock::new);

    @Override
    protected MapCodec<? extends TubeLightBlock> codec() {
        return CODEC;
    }
    private static final VoxelShape DOWN = box(3, 12, 3, 13, 16, 13);
    private static final VoxelShape UP = box(3, 0, 3, 13, 4, 13);
    private static final VoxelShape NORTH = box(3, 3, 12, 13, 13, 16);
    private static final VoxelShape SOUTH = box(3, 3, 0, 13, 13, 4);
    private static final VoxelShape WEST = box(12, 3, 3, 16, 13, 13);
    private static final VoxelShape EAST = box(0, 3, 3, 4, 13, 13);

    public TubeLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos position) {
        Direction supportDirection = state.getValue(FACING).getOpposite();
        BlockPos supportPosition = position.relative(supportDirection);
        return level.getBlockState(supportPosition).isFaceSturdy(level, supportPosition, state.getValue(FACING));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
