package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;

/**
 * Shared controller shell for every immutable multiblock definition.
 */
public final class AdvancedMultiblockBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final MultiblockDefinition definition;

    public AdvancedMultiblockBlock(MultiblockDefinition definition, Properties properties) {
        super(properties);
        this.definition = Objects.requireNonNull(definition);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false));
    }

    public MultiblockDefinition definition() {
        return definition;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        if (placer instanceof ServerPlayer player
                && level.getBlockEntity(position) instanceof AdvancedMultiblockBlockEntity controller) {
            controller.setOwner(player.getUUID());
        }
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(position) instanceof AdvancedMultiblockBlockEntity controller)) {
            return InteractionResult.CONSUME;
        }
        if (controller.handleShelvingChest(serverPlayer, player.getItemInHand(hand))) {
            return InteractionResult.CONSUME;
        }
        if (!controller.formed()) {
            if (!controller.tryForm(serverPlayer)) {
                controller.toggleHologram(serverPlayer);
            }
        } else if (player.isShiftKeyDown()) {
            controller.cycleHydraulicMode(serverPlayer);
        } else if (controller.canManage(serverPlayer)) {
            serverPlayer.openMenu(controller, buffer -> {
                buffer.writeBlockPos(position);
                buffer.writeEnum(definition);
                buffer.writeBoolean(controller.mirrored());
                buffer.writeEnum(controller.facing());
                buffer.writeBoolean(controller.formed());
            });
        } else {
            controller.describe(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(position) instanceof AdvancedMultiblockBlockEntity controller) {
            controller.unform();
        }
        super.onRemove(state, level, position, replacement, moved);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof AdvancedMultiblockBlockEntity controller) {
            List<ItemStack> controllerDrops = drops.stream().filter(stack -> stack.is(asItem())).toList();
            if (controllerDrops.isEmpty() && controller.hasPortableState()) {
                ItemStack fallback = new ItemStack(asItem());
                controller.saveToItem(fallback);
                drops.add(fallback);
            } else {
                controllerDrops.forEach(controller::saveToItem);
            }
            if (controller.shelvingStorage() != null) {
                drops.addAll(controller.shelvingStorage().dropContents());
            }
        }
        return List.copyOf(drops);
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

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return state.getValue(FORMED)
                ? LegacyMultiblockCollision.shapeAt(position, position, definition, state.getValue(FACING))
                : super.getShape(state, level, position, context);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return state.getValue(FORMED)
                ? LegacyMultiblockCollision.shapeAt(position, position, definition, state.getValue(FACING))
                : super.getCollisionShape(state, level, position, context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new AdvancedMultiblockBlockEntity(position, state);
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
                : createTickerHelper(type,
                committee.nova.mods.magneticraft.init.ModBlockEntities.advancedMultiblock(definition).get(),
                AdvancedMultiblockBlockEntity::serverTick);
    }
}
