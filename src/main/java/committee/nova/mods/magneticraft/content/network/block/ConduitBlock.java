package committee.nova.mods.magneticraft.content.network.block;

import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockExternalPortService;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Compact conduit collision shape with arms only toward compatible neighbors.
 */
public abstract class ConduitBlock extends NetworkComponentBlock {
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    private final VoxelShape center;
    private final VoxelShape[] arms;

    protected ConduitBlock(Properties properties) {
        this(properties, 5);
    }

    protected ConduitBlock(Properties properties, int insetPixels) {
        super(properties);
        if (insetPixels <= 0 || insetPixels >= 8) {
            throw new IllegalArgumentException("Conduit inset must be between 1 and 7 pixels");
        }
        int far = 16 - insetPixels;
        center = Block.box(insetPixels, insetPixels, insetPixels, far, far, far);
        arms = new VoxelShape[]{
                Block.box(insetPixels, 0, insetPixels, far, insetPixels, far),
                Block.box(insetPixels, far, insetPixels, far, 16, far),
                Block.box(insetPixels, insetPixels, 0, far, far, insetPixels),
                Block.box(insetPixels, insetPixels, far, far, far, 16),
                Block.box(0, insetPixels, insetPixels, insetPixels, far, far),
                Block.box(far, insetPixels, insetPixels, 16, far, far)
        };
        registerDefaultState(defaultBlockState()
                .setValue(DOWN, false)
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos position,
            BlockPos neighborPosition
    ) {
        return state.setValue(property(direction), canConnect(level, position, direction));
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        VoxelShape shape = center;
        for (Direction direction : Direction.values()) {
            if (state.getValue(property(direction))) {
                shape = Shapes.or(shape, arms[direction.ordinal()]);
            }
        }
        return shape;
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static void refreshAround(Level level, BlockPos position) {
        refreshConnectionsIfLoaded(level, position);
        for (Direction direction : Direction.values()) {
            refreshConnectionsIfLoaded(level, position.relative(direction));
        }
    }

    private static void refreshConnectionsIfLoaded(Level level, BlockPos position) {
        if (level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
            refreshConnections(level, position);
        }
    }

    private static void refreshConnections(Level level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        if (!(state.getBlock() instanceof ConduitBlock conduit)) {
            return;
        }
        BlockState updated = conduit.withConnections(state, level, position);
        if (updated != state) {
            level.setBlock(position, updated, Block.UPDATE_ALL);
        }
    }

    private BlockState withConnections(BlockState state, LevelAccessor level, BlockPos position) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            result = result.setValue(property(direction), canConnect(level, position, direction));
        }
        return result;
    }

    private boolean canConnect(LevelAccessor level, BlockPos position, Direction direction) {
        BlockEntity self = level.getBlockEntity(position);
        if (self instanceof NetworkConnectionHost host
                && !host.supportsNetworkConnection(connectionDomain(), direction)) {
            return false;
        }

        BlockPos neighborPosition = position.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPosition);
        Direction neighborSide = direction.getOpposite();
        if (connectsVisuallyTo(level, position, direction, neighborState)) {
            BlockEntity neighbor = level.getBlockEntity(neighborPosition);
            return !(neighbor instanceof NetworkConnectionHost host)
                    || host.supportsNetworkConnection(connectionDomain(), neighborSide);
        }
        return connectsToMachine(level, neighborPosition, neighborSide);
    }

    protected boolean connectsToMachine(LevelAccessor level, BlockPos position, Direction side) {
        return level.getBlockEntity(position) instanceof NetworkConnectionHost host
                && host.supportsNetworkConnection(connectionDomain(), side)
                || MultiblockExternalPortService.supports(
                level, position, connectionDomain(), side
        );
    }

    protected abstract NetworkDomain connectionDomain();

    protected boolean connectsVisuallyTo(
            LevelAccessor level,
            BlockPos position,
            Direction direction,
            BlockState neighbor
    ) {
        return connectsVisuallyTo(neighbor);
    }

    protected abstract boolean connectsVisuallyTo(BlockState neighbor);
}
