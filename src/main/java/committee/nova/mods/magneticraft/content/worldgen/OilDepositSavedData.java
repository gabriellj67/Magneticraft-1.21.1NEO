package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/** Durable finite reserves and oil-field ownership, independent of loaded chunks. */
public final class OilDepositSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 2;

    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final String DATA_NAME = "magneticraft_oil_deposits";
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String DEPOSITS_TAG = "deposits";
    private static final String POSITION_TAG = "position";
    private static final String FIELD_ORIGIN_TAG = "field_origin";
    private static final String REMAINING_TAG = "remaining_millibuckets";

    private final Map<BlockPos, Deposit> deposits = new HashMap<>();

    public static OilDepositSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                OilDepositSavedData::load,
                OilDepositSavedData::new,
                DATA_NAME
        );
    }

    public static OilDepositSavedData load(CompoundTag tag) {
        OilDepositSavedData data = new OilDepositSavedData();
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || !tag.contains(DEPOSITS_TAG, Tag.TAG_LIST)) {
            return data;
        }
        int version = tag.getInt(SCHEMA_VERSION_TAG);
        if (version != SCHEMA_VERSION && version != LEGACY_SCHEMA_VERSION) {
            return data;
        }
        ListTag deposits = tag.getList(DEPOSITS_TAG, Tag.TAG_COMPOUND);
        for (Tag rawDeposit : deposits) {
            if (!(rawDeposit instanceof CompoundTag deposit)
                    || !deposit.contains(POSITION_TAG, Tag.TAG_LONG)
                    || !deposit.contains(FIELD_ORIGIN_TAG, Tag.TAG_LONG)
                    || !deposit.contains(REMAINING_TAG, Tag.TAG_INT)) {
                continue;
            }
            BlockPos position = BlockPos.of(deposit.getLong(POSITION_TAG));
            BlockPos fieldOrigin = BlockPos.of(deposit.getLong(FIELD_ORIGIN_TAG));
            int remaining = version == LEGACY_SCHEMA_VERSION
                    ? OilDepositPersistence.migrateLegacyReserve(deposit.getInt(REMAINING_TAG))
                    : bounded(deposit.getInt(REMAINING_TAG));
            data.deposits.putIfAbsent(position, new Deposit(fieldOrigin, remaining));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ListTag encoded = new ListTag();
        deposits.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .forEach(entry -> {
                    CompoundTag deposit = new CompoundTag();
                    deposit.putLong(POSITION_TAG, entry.getKey().asLong());
                    deposit.putLong(FIELD_ORIGIN_TAG, entry.getValue().fieldOrigin().asLong());
                    deposit.putInt(REMAINING_TAG, entry.getValue().remainingMillibuckets());
                    encoded.add(deposit);
                });
        tag.put(DEPOSITS_TAG, encoded);
        return tag;
    }

    public int register(BlockPos position, BlockPos fieldOrigin, int fallbackRemaining) {
        BlockPos stablePosition = position.immutable();
        Deposit existing = deposits.get(stablePosition);
        if (existing != null) {
            return existing.remainingMillibuckets();
        }
        int remaining = bounded(fallbackRemaining);
        deposits.put(stablePosition, new Deposit(fieldOrigin, remaining));
        setDirty();
        return remaining;
    }

    public int drain(
            BlockPos position,
            BlockPos fieldOrigin,
            int fallbackRemaining,
            int requested,
            boolean simulate
    ) {
        Deposit existing = deposits.get(position);
        int remaining = existing == null
                ? bounded(fallbackRemaining)
                : existing.remainingMillibuckets();
        int drained = Math.min(Math.max(requested, 0), remaining);
        if (!simulate && drained > 0) {
            deposits.put(position.immutable(), new Deposit(fieldOrigin, remaining - drained));
            setDirty();
        }
        return drained;
    }

    public boolean remove(BlockPos position) {
        if (deposits.remove(position) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Deposit depositAt(BlockPos position) {
        return deposits.get(position);
    }

    public int size() {
        return deposits.size();
    }

    private static int bounded(int remaining) {
        return Math.max(0, Math.min(OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS, remaining));
    }

    public record Deposit(BlockPos fieldOrigin, int remainingMillibuckets) {
        public Deposit {
            fieldOrigin = fieldOrigin.immutable();
            remainingMillibuckets = bounded(remainingMillibuckets);
        }
    }
}
