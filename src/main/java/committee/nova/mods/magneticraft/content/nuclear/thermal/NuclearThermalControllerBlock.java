package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

/** Controller and sole interaction surface for one variable-volume thermal facility. */
public final class NuclearThermalControllerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final NuclearThermalFacilityType facilityType;

    public NuclearThermalControllerBlock(NuclearThermalFacilityType facilityType, Properties properties) {
        super(properties);
        this.facilityType = facilityType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends NuclearThermalControllerBlock> codec() {
        return simpleCodec(properties -> new NuclearThermalControllerBlock(facilityType, properties));
    }

    public NuclearThermalFacilityType facilityType() {
        return facilityType;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos position, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, position, state, placer, stack);
        if (placer instanceof ServerPlayer player
                && level.getBlockEntity(position) instanceof NuclearThermalControllerBlockEntity controller) {
            controller.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position, Player player,
                                 BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(position) instanceof NuclearThermalControllerBlockEntity controller)
                || !controller.canManage(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!controller.formed()) controller.tryForm(serverPlayer);
        if (controller.formed()) {
            serverPlayer.openMenu(controller, buffer -> {
                buffer.writeBlockPos(position);
                buffer.writeEnum(facilityType);
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(position) instanceof NuclearThermalControllerBlockEntity controller) {
            controller.unform();
        }
        super.onRemove(state, level, position, replacement, moved);
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
        return new NuclearThermalControllerBlockEntity(position, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModBlockEntities.NUCLEAR_THERMAL_CONTROLLER.get(),
                NuclearThermalControllerBlockEntity::serverTick);
    }
}
