package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

public final class ElectricalProtectionBlock extends NetworkComponentBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final ElectricalProtectionKind kind;

    public ElectricalProtectionBlock(Properties properties, ElectricalProtectionKind kind) {
        super(properties);
        this.kind = Objects.requireNonNull(kind, "kind");
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends ElectricalProtectionBlock> codec() {
        return simpleCodec(properties -> new ElectricalProtectionBlock(properties, kind));
    }

    public ElectricalProtectionKind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new ElectricalProtectionBlockEntity(position, state);
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
        if (kind == ElectricalProtectionKind.FUSE_BOX && stack.is(ModNetworkItems.FUSE.get())) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(position) instanceof ElectricalProtectionBlockEntity protection
                    && protection.protection().insertFuse(stack)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return ItemInteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable("message.magneticraft.invalid_fuse"), true);
            return ItemInteractionResult.FAIL;
        }
        return super.useItemOn(stack, state, level, position, player, hand, hit);
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
                && level.getBlockEntity(position) instanceof ElectricalProtectionBlockEntity protection) {
            ElectricalDeviceMenu.open(
                    serverPlayer,
                    protection,
                    position,
                    ElectricalDeviceKind.from(kind)
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos position,
            net.minecraft.world.level.block.Block neighbor,
            BlockPos neighborPosition,
            boolean moving
    ) {
        if (!level.isClientSide
                && level.getBlockEntity(position) instanceof ElectricalProtectionBlockEntity protection) {
            protection.protection().refreshConnection();
        }
        super.neighborChanged(state, level, position, neighbor, neighborPosition, moving);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
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
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ElectricalProtectionBlockEntity protection) {
            TieredElectricalItemData data = protection.protection().itemData();
            drops.stream().filter(stack -> stack.is(asItem())).forEach(data::write);
        }
        return List.copyOf(drops);
    }
}
