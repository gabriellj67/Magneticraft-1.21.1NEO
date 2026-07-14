package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared placement, paired-block and menu lifecycle for the Task 5 machines.
 */
public final class SingleBlockMachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty MASTER = BooleanProperty.create("master");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SLUICE_SECONDARY_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape FEEDING_TROUGH_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    private final SingleBlockMachineDefinition definition;

    public SingleBlockMachineBlock(SingleBlockMachineDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MASTER, true)
                .setValue(LIT, false));
    }

    public SingleBlockMachineDefinition definition() {
        return definition;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = placementFacing(
                definition,
                context.getClickedFace(),
                context.getHorizontalDirection()
        );
        BlockState state = defaultBlockState().setValue(FACING, facing);
        if (definition.doubleLength()) {
            BlockPos secondary = context.getClickedPos().relative(facing);
            if (!context.getLevel().getBlockState(secondary).canBeReplaced(context)) {
                return null;
            }
        }
        return state;
    }

    static Direction placementFacing(
            SingleBlockMachineDefinition definition,
            Direction clickedFace,
            Direction horizontalDirection
    ) {
        return switch (definition.facingMode()) {
            case NONE -> Direction.NORTH;
            case HORIZONTAL -> definition.doubleLength()
                    ? horizontalDirection
                    : horizontalDirection.getOpposite();
            case ALL -> clickedFace.getOpposite();
        };
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        if (definition == SingleBlockMachineDefinition.FEEDING_TROUGH) {
            return FEEDING_TROUGH_SHAPE;
        }
        if (definition == SingleBlockMachineDefinition.SLUICE_BOX && !state.getValue(MASTER)) {
            return SLUICE_SECONDARY_SHAPE;
        }
        return super.getShape(state, level, position, context);
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
        if (!level.isClientSide && definition.doubleLength() && state.getValue(MASTER)) {
            Direction facing = state.getValue(FACING);
            level.setBlock(
                    position.relative(facing),
                    state.setValue(FACING, facing.getOpposite()).setValue(MASTER, false),
                    Block.UPDATE_ALL
            );
        }
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
        if (definition.doubleLength() && direction == state.getValue(FACING)) {
            boolean expectedMaster = !state.getValue(MASTER);
            if (!neighborState.is(this) || neighborState.getValue(MASTER) != expectedMaster) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, position, neighborPosition);
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
        BlockPos machinePosition = masterPosition(position, state);
        BlockEntity blockEntity = level.getBlockEntity(machinePosition);
        if (!(blockEntity instanceof SingleBlockMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        InteractionResult handled = machine.interact(player, hand, hit);
        if (handled.consumesAction()) {
            return handled;
        }
        if (definition.hasMenu()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, machine, machinePosition);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos position, BlockState state, Player player) {
        if (!level.isClientSide && definition.doubleLength()) {
            BlockPos other = position.relative(state.getValue(FACING));
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this) && otherState.getValue(MASTER) != state.getValue(MASTER)) {
                if (state.getValue(MASTER)) {
                    level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                } else {
                    level.destroyBlock(other, !player.getAbilities().instabuild, player);
                }
            }
        }
        super.playerWillDestroy(level, position, state, player);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())
                && !newState.is(ModAdvancedBlocks.MULTIBLOCK_GAP.get())
                && oldState.getValue(MASTER)) {
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof SingleBlockMachineBlockEntity machine) {
                machine.dropContents(level);
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (definition.doubleLength() && !state.getValue(MASTER)) {
            return List.of();
        }
        if (definition == SingleBlockMachineDefinition.SMALL_TANK) {
            ItemStack stack = new ItemStack(ModMachineBlocks.machine(definition).get());
            BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockEntity instanceof SingleBlockMachineBlockEntity machine) {
                machine.saveTankToItem(stack);
            }
            return List.of(stack);
        }
        return super.getDrops(state, builder);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return state.getValue(MASTER) ? new SingleBlockMachineBlockEntity(position, state) : null;
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
                        ModBlockEntities.singleBlockMachine(definition).get(),
                        SingleBlockMachineBlockEntity::serverTick
                );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        builder.add(FACING, MASTER, LIT);
    }

    private static BlockPos masterPosition(BlockPos position, BlockState state) {
        return state.getValue(MASTER) ? position : position.relative(state.getValue(FACING));
    }
}
