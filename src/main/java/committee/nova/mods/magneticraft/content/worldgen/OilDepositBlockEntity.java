package committee.nova.mods.magneticraft.content.worldgen;

import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Durable finite oil reserve measured in milliBuckets.
 */
public final class OilDepositBlockEntity extends BlockEntity {
    public static final int DEFAULT_RESERVE_MILLIBUCKETS = 4_000_000;

    private int remaining = DEFAULT_RESERVE_MILLIBUCKETS;

    public OilDepositBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.OIL_DEPOSIT.get(), position, state);
    }

    public int remaining() {
        return remaining;
    }

    public int drain(int requested, boolean simulate) {
        int drained = Math.min(Math.max(requested, 0), remaining);
        if (!simulate && drained > 0) {
            remaining -= drained;
            setChanged();
        }
        return drained;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        OilDepositPersistence.write(tag, remaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        remaining = OilDepositPersistence.read(tag);
    }
}
