package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Temporary non-solid water replacement maintained by an airlock.
 */
public final class AirBubbleBlock extends Block {
    public static final BooleanProperty DECAYING = BooleanProperty.create("decaying");

    public AirBubbleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DECAYING, false));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (state.getValue(DECAYING)) {
            level.removeBlock(position, false);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (state.getValue(DECAYING)) {
            level.removeBlock(position, false);
        }
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DECAYING);
    }
}
