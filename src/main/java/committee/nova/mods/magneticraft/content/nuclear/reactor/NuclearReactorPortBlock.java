package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.content.nuclear.structure.NuclearStructureState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Directional structural penetration; runtime capabilities are added in later phases. */
public final class NuclearReactorPortBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = NuclearStructureState.FORMED;

    private final NuclearReactorPortType portType;

    public NuclearReactorPortBlock(NuclearReactorPortType portType, Properties properties) {
        super(properties);
        this.portType = Objects.requireNonNull(portType);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends NuclearReactorPortBlock> codec() {
        return simpleCodec(properties -> new NuclearReactorPortBlock(portType, properties));
    }

    public NuclearReactorPortType portType() {
        return portType;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new NuclearReactorPortBlockEntity(position, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide ? null : createTickerHelper(
                type, committee.nova.mods.magneticraft.init.ModBlockEntities.NUCLEAR_REACTOR_PORT.get(),
                NuclearReactorPortBlockEntity::serverTick);
    }
}
