package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

/** Persistent visible radioactive material; no invisible chunk contamination is stored. */
public final class RadioactiveSourceBlock extends BaseEntityBlock {
    private final RadioactiveSourceKind kind;

    public RadioactiveSourceBlock(RadioactiveSourceKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends RadioactiveSourceBlock> codec() {
        return simpleCodec(properties -> new RadioactiveSourceBlock(kind, properties));
    }

    public RadioactiveSourceKind kind() {
        return kind;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new RadioactiveSourceBlockEntity(position, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModBlockEntities.RADIOACTIVE_SOURCE.get(), RadioactiveSourceBlockEntity::serverTick);
    }
}
