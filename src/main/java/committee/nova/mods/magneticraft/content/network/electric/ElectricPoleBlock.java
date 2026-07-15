package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.item.TieredElectricalDrops;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlock;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Five-block pole whose loaded electrical endpoint lives in the top BASE segment. */
public final class ElectricPoleBlock extends NetworkComponentBlock {
    public static final EnumProperty<PoleDirection> DIRECTION = EnumProperty.create("direction", PoleDirection.class);
    public static final EnumProperty<PoleSegment> SEGMENT = EnumProperty.create("segment", PoleSegment.class);
    private static final int HEIGHT = 5;
    private static final VoxelShape POLE_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final ThreadLocal<Set<BlockPos>> STRUCTURE_UPDATES = ThreadLocal.withInitial(HashSet::new);

    private final boolean transformer;

    public ElectricPoleBlock(Properties properties, boolean transformer) {
        super(properties);
        this.transformer = transformer;
        registerDefaultState(stateDefinition.any()
                .setValue(DIRECTION, PoleDirection.NORTH)
                .setValue(SEGMENT, PoleSegment.DOWN_4));
    }

    public boolean isTransformer() {
        return transformer;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return POLE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return POLE_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos bottom = context.getClickedPos();
        for (int height = 0; height < HEIGHT; height++) {
            BlockPos position = bottom.above(height);
            if (context.getLevel().isOutsideBuildHeight(position)
                    || !context.getLevel().getBlockState(position).canBeReplaced()) {
                return null;
            }
        }
        return defaultBlockState()
                .setValue(DIRECTION, PoleDirection.fromYaw(context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot()))
                .setValue(SEGMENT, PoleSegment.DOWN_4);
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
        if (level.isClientSide) {
            return;
        }
        PoleDirection direction = state.getValue(DIRECTION);
        Set<BlockPos> updates = STRUCTURE_UPDATES.get();
        BlockPos base = position.above(HEIGHT - 1);
        updates.add(base);
        try {
            for (int height = 0; height < HEIGHT; height++) {
                level.setBlock(
                        position.above(height),
                        defaultBlockState()
                                .setValue(DIRECTION, direction)
                                .setValue(SEGMENT, PoleSegment.atHeightFromBottom(height)),
                        Block.UPDATE_ALL
                );
            }
        } finally {
            updates.remove(base);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return state.getValue(SEGMENT) == PoleSegment.BASE
                ? new ElectricPoleBlockEntity(position, state)
                : null;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos position, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockPos base = position.above(oldState.getValue(SEGMENT).blocksBelowBase());
            Set<BlockPos> updates = STRUCTURE_UPDATES.get();
            if (!level.isClientSide && updates.add(base)) {
                try {
                    if (level instanceof ServerLevel serverLevel) {
                        LongDistanceElectricityService.get(serverLevel).removeConnectionsAt(base);
                    }
                    if (!position.equals(base) && level.getBlockState(base).is(this)) {
                        level.destroyBlock(base, true);
                    }
                    BlockPos bottom = base.below(HEIGHT - 1);
                    for (int height = 0; height < HEIGHT; height++) {
                        BlockPos member = bottom.above(height);
                        if (!member.equals(position) && !member.equals(base) && level.getBlockState(member).is(this)) {
                            level.removeBlock(member, false);
                        }
                    }
                } finally {
                    updates.remove(base);
                }
            }
        }
        super.onRemove(oldState, level, position, newState, moving);
    }

    public void convertStructure(Level level, BlockPos base, ElectricPoleBlock target) {
        BlockState baseState = level.getBlockState(base);
        if (!(baseState.getBlock() instanceof ElectricPoleBlock source)
                || source.transformer
                || target == source
                || baseState.getValue(SEGMENT) != PoleSegment.BASE) {
            return;
        }
        PoleDirection direction = baseState.getValue(DIRECTION);
        Set<BlockPos> updates = STRUCTURE_UPDATES.get();
        updates.add(base);
        try {
            BlockPos bottom = base.below(HEIGHT - 1);
            for (int height = 0; height < HEIGHT; height++) {
                level.setBlock(
                        bottom.above(height),
                        target.defaultBlockState()
                                .setValue(DIRECTION, direction)
                                .setValue(SEGMENT, PoleSegment.atHeightFromBottom(height)),
                        Block.UPDATE_ALL
                );
            }
        } finally {
            updates.remove(base);
        }
    }

    public static BlockPos basePosition(BlockPos position, BlockState state) {
        return position.above(state.getValue(SEGMENT).blocksBelowBase());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return TieredElectricalDrops.preserveTier(
                new ArrayList<>(super.getDrops(state, builder)),
                asItem(),
                builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRECTION, SEGMENT);
    }
}
