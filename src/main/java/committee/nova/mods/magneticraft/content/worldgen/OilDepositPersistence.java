package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Current-format codec for one finite oil reserve. */
final class OilDepositPersistence {
    static final String SCHEMA_VERSION_TAG = "schema_version";
    static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int LEGACY_MAX_RESERVE_MILLIBUCKETS = 4_000_000;
    private static final String REMAINING_TAG = "remaining_millibuckets";

    private OilDepositPersistence() {
    }

    static void write(CompoundTag tag, int remaining) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putInt(REMAINING_TAG, boundedReserve(remaining));
    }

    static int read(CompoundTag tag) {
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)) {
            return OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS;
        }
        if (!tag.contains(REMAINING_TAG, Tag.TAG_INT)) {
            return OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS;
        }
        int version = tag.getInt(SCHEMA_VERSION_TAG);
        if (version == SCHEMA_VERSION) {
            return boundedReserve(tag.getInt(REMAINING_TAG));
        }
        if (version == LEGACY_SCHEMA_VERSION) {
            return migrateLegacyReserve(tag.getInt(REMAINING_TAG));
        }
        return OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS;
    }

    static int boundedReserve(int remaining) {
        return Math.max(0, Math.min(OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS, remaining));
    }

    static int migrateLegacyReserve(int remaining) {
        int bounded = Math.max(0, Math.min(LEGACY_MAX_RESERVE_MILLIBUCKETS, remaining));
        long scaled = (long) bounded * OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS
                + LEGACY_MAX_RESERVE_MILLIBUCKETS / 2L;
        return (int) (scaled / LEGACY_MAX_RESERVE_MILLIBUCKETS);
    }
}
