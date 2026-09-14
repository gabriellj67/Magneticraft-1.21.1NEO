package committee.nova.mods.magneticraft.content.network.block;

import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Shared wrench, ticking and removal lifecycle for network components.
 */
public abstract class NetworkComponentBlock extends BaseEntityBlock {
    protected NetworkComponentBlock(Properties properties) {
        super(properties);
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
        if (!stack.is(ModNetworkItems.WRENCH.get()) && !stack.is(ModTags.Items.WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(position) instanceof NetworkComponentBlockEntity component) {
            player.displayClientMessage(
                    component.configure(hit.getDirection(), player.isSecondaryUseActive()),
                    true
            );
            ConduitBlock.refreshAround(level, position);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof NetworkComponentBlockEntity component) {
                component.dropContents(level);
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return (tickerLevel, position, tickerState, blockEntity) -> {
            if (blockEntity instanceof NetworkComponentBlockEntity component) {
                component.serverTick();
            }
        };
    }
}
