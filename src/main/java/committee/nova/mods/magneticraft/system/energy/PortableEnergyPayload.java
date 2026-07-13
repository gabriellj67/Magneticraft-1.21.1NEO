package committee.nova.mods.magneticraft.system.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Versioned, loader-independent payload for portable electrical storage.
 */
public final class PortableEnergyPayload {
    public static final int SCHEMA_VERSION = 1;

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String ENERGY_TAG = "energy";

    private PortableEnergyPayload() {
    }

    public static CompoundTag write(int energy) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putInt(ENERGY_TAG, Math.max(0, energy));
        return tag;
    }

    public static int readEnergyOrDefault(CompoundTag tag, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Energy capacity must be positive");
        }
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(ENERGY_TAG, Tag.TAG_INT)) {
            return 0;
        }
        return Math.max(0, Math.min(capacity, tag.getInt(ENERGY_TAG)));
    }
}
