package committee.nova.mods.magneticraft.content.worldgen;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Durable finite oil reserve measured in milliBuckets.
 */
public final class OilDepositBlockEntity extends BlockEntity {
    public static final int EXTRACTION_STAGE_MILLIBUCKETS = 1_000;
    public static final int EXTRACTION_STAGES = 10;
    public static final int DEFAULT_RESERVE_MILLIBUCKETS =
            EXTRACTION_STAGE_MILLIBUCKETS * EXTRACTION_STAGES;
    private static final String FIELD_ORIGIN_TAG = "field_origin";

    private int remaining = DEFAULT_RESERVE_MILLIBUCKETS;
    private BlockPos fieldOrigin;

    public OilDepositBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.OIL_DEPOSIT.get(), position, state);
        fieldOrigin = position.immutable();
    }

    public int remaining() {
        synchronizeSavedData();
        return remaining;
    }

    public BlockPos fieldOrigin() {
        return fieldOrigin;
    }

    public void setFieldOrigin(BlockPos fieldOrigin) {
        BlockPos stable = fieldOrigin.immutable();
        if (!stable.equals(this.fieldOrigin)) {
            this.fieldOrigin = stable;
            setChanged();
        }
    }

    public int drain(int requested, boolean simulate) {
        if (level instanceof ServerLevel serverLevel) {
            OilDepositSavedData data = OilDepositSavedData.get(serverLevel);
            if (!simulate) {
                remaining = data.register(worldPosition, fieldOrigin, remaining);
            }
            int drained = data.drain(
                    worldPosition, fieldOrigin, remaining, requested, simulate);
            if (!simulate && drained > 0) {
                remaining -= drained;
                setChanged();
            }
            return drained;
        }
        int drained = Math.min(Math.max(requested, 0), remaining);
        if (!simulate && drained > 0) {
            remaining -= drained;
            setChanged();
        }
        return drained;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        synchronizeSavedData();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        OilDepositPersistence.write(tag, remaining);
        tag.putLong(FIELD_ORIGIN_TAG, fieldOrigin.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        remaining = OilDepositPersistence.read(tag);
        fieldOrigin = tag.contains(FIELD_ORIGIN_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(FIELD_ORIGIN_TAG))
                : worldPosition.immutable();
    }

    private void synchronizeSavedData() {
        if (level instanceof ServerLevel serverLevel) {
            remaining = OilDepositSavedData.get(serverLevel)
                    .register(worldPosition, fieldOrigin, remaining);
        }
    }
}
