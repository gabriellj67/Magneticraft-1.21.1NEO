package committee.nova.mods.magneticraft.content.machine.crushingtable;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Top-face, main-hand interaction surface for the manual crushing table.
 */
public final class CrushingTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 12, 1, 15, 16, 15),
            box(2, 0, 2, 5, 12, 5),
            box(11, 0, 2, 14, 12, 5),
            box(2, 0, 11, 5, 12, 14),
            box(11, 0, 11, 14, 12, 14)
    );

    public CrushingTableBlock(Properties properties) {
        super(properties);
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
        if (hand != InteractionHand.MAIN_HAND || hit.getDirection() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof CrushingTableBlockEntity table && table.interact(player, hand)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof CrushingTableBlockEntity table) {
                ItemStack stored = table.removeStoredItem();
                if (!stored.isEmpty()) {
                    Containers.dropItemStack(level, position.getX(), position.getY(), position.getZ(), stored);
                }
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new CrushingTableBlockEntity(position, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return SHAPE;
    }
}
