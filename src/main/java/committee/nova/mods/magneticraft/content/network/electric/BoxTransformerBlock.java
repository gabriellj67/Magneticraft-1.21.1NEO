package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import committee.nova.mods.magneticraft.content.item.TransformerElectricalDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BoxTransformerBlock extends NetworkComponentBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BoxTransformerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new BoxTransformerBlockEntity(position, state);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            net.minecraft.world.level.Level level,
            BlockPos position,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        InteractionResult wrenchResult = super.use(state, level, position, player, hand, hit);
        if (wrenchResult != InteractionResult.PASS) {
            return wrenchResult;
        }
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof BoxTransformerBlockEntity transformer) {
            ElectricalDeviceMenu.open(
                    serverPlayer,
                    transformer,
                    position,
                    ElectricalDeviceKind.BOX_TRANSFORMER
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        return TransformerElectricalDrops.preserveProfile(
                drops,
                asItem(),
                builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        );
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
