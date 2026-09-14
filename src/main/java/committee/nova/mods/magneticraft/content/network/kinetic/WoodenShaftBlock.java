package committee.nova.mods.magneticraft.content.network.kinetic;
import com.mojang.serialization.MapCodec;

import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** Straight low-inertia kinetic conductor. */
public final class WoodenShaftBlock extends NetworkComponentBlock {
    public static final MapCodec<WoodenShaftBlock> CODEC = simpleCodec(WoodenShaftBlock::new);

    @Override
    protected MapCodec<? extends WoodenShaftBlock> codec() {
        return CODEC;
    }
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public WoodenShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new WoodenShaftBlockEntity(position, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction.Axis axis = state.getValue(AXIS);
        if ((rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
                && axis.isHorizontal()) {
            return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
