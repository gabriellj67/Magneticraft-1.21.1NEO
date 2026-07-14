package committee.nova.mods.magneticraft.content.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared placement, ownership, floppy and redstone boundary for programmable blocks.
 */
public abstract class ProgrammableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected ProgrammableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        if (!level.isClientSide
                && placer instanceof Player player
                && level.getBlockEntity(position) instanceof ProgrammableBlockEntity programmable) {
            programmable.setOwner(player.getUUID());
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
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(position) instanceof ProgrammableBlockEntity programmable)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (programmable.owner() == null) {
            programmable.claim(player);
        }
        if (!programmable.canManage(player)) {
            player.displayClientMessage(Component.translatable("message.magneticraft.computer.access_denied"), true);
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof FloppyDiskItem) {
            if (player.isShiftKeyDown()) {
                boolean stored = programmable.scriptProgram()
                        .map(program -> FloppyDiskItem.storeScript(held, program, programmable.virtualDisk()))
                        .orElseGet(() -> FloppyDiskItem.storeProgram(held, programmable.program()));
                player.displayClientMessage(Component.translatable(stored
                        ? "message.magneticraft.computer.floppy_written"
                        : "message.magneticraft.computer.floppy_read_only"), true);
                return InteractionResult.CONSUME;
            }
            boolean loaded = FloppyDiskItem.readState(held).map(payload -> {
                if (payload.scriptProgram().isPresent()) {
                    boolean replaced = programmable.tryReplaceScript(
                            programmable.programRevision(),
                            payload.scriptProgram().orElseThrow()
                    );
                    return replaced && payload.disk().map(programmable::replaceVirtualDisk).orElse(true);
                }
                return payload.legacyProgram()
                        .map(program -> programmable.tryReplaceProgram(programmable.programRevision(), program))
                        .orElse(false);
            }).orElse(false);
            player.displayClientMessage(
                    Component.translatable(loaded
                            ? "message.magneticraft.computer.floppy_loaded"
                            : "message.magneticraft.computer.floppy_invalid"),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            long sessionToken = UUID.randomUUID().getLeastSignificantBits();
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) -> programmable.createMenuForSession(
                                    containerId,
                                    inventory,
                                    menuPlayer,
                                    sessionToken
                            ),
                            programmable.getDisplayName()
                    ),
                    buffer -> programmable.writeMenuOpeningData(buffer, sessionToken)
            );
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos position, Direction direction) {
        return level.getBlockEntity(position) instanceof ProgrammableBlockEntity programmable
                ? programmable.redstoneOutput()
                : 0;
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
        builder.add(FACING);
    }
}
