package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Horizontal two-terminal housing shared by the switch, diode and adjustable resistor. */
public final class ElectricalControlBlock extends NetworkComponentBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final ElectricalControlKind kind;

    public ElectricalControlBlock(Properties properties, ElectricalControlKind kind) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind");
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends ElectricalControlBlock> codec() {
        return simpleCodec(properties -> new ElectricalControlBlock(properties, kind));
    }

    public ElectricalControlKind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new ElectricalControlBlockEntity(position, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (stack.is(ModNetworkItems.WRENCH.get()) || stack.is(ModTags.Items.WRENCHES)) {
            if (!level.isClientSide) {
                level.setBlock(position, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
                ConduitBlock.refreshAround(level, position);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos position,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof ElectricalControlBlockEntity control) {
            ElectricalDeviceMenu.open(serverPlayer, control, position, ElectricalDeviceKind.from(kind));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ElectricalControlBlockEntity control) {
            drops.stream()
                    .filter(stack -> stack.is(asItem()))
                    .forEach(stack -> control.control().itemData().write(stack));
        }
        return List.copyOf(drops);
    }
}
