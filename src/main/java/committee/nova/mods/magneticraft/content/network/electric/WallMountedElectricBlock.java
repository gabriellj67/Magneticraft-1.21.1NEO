package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.Nullable;

/** Shared six-direction support semantics for connector-style endpoints. */
abstract class WallMountedElectricBlock extends NetworkComponentBlock {
    static final DirectionProperty FACING = BlockStateProperties.FACING;

    WallMountedElectricBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos position) {
        Direction facing = state.getValue(FACING);
        BlockPos support = position.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(support);
        return supportState.is(ModNetworkBlocks.ELECTRIC_CABLE.get())
                || supportState.isFaceSturdy(level, support, facing)
                || level.getBlockEntity(support) instanceof NetworkConnectionHost host
                && host.supportsNetworkConnection(NetworkDomain.ELECTRICITY, facing);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction changedSide,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos position,
            BlockPos neighborPosition
    ) {
        if (changedSide == state.getValue(FACING).getOpposite() && !state.canSurvive(level, position)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, changedSide, neighbor, level, position, neighborPosition);
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
