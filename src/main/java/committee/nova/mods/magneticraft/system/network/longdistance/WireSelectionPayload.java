package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Versioned item-owned payload for a copper wire coil's selected endpoint. */
public final class WireSelectionPayload {
    public static final int SCHEMA_VERSION = 1;

    private static final String OWNER_TAG = "wire_selection";
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String DIMENSION_TAG = "dimension";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";

    private WireSelectionPayload() {
    }

    public static void write(CompoundTag owner, Selection selection) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(selection);
        CompoundTag payload = new CompoundTag();
        payload.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        payload.putString(DIMENSION_TAG, selection.dimension().toString());
        payload.putInt(X_TAG, selection.position().getX());
        payload.putInt(Y_TAG, selection.position().getY());
        payload.putInt(Z_TAG, selection.position().getZ());
        owner.put(OWNER_TAG, payload);
    }

    public static Optional<Selection> read(CompoundTag owner) {
        Objects.requireNonNull(owner);
        if (!owner.contains(OWNER_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag payload = owner.getCompound(OWNER_TAG);
        if (!payload.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || payload.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !payload.contains(DIMENSION_TAG, Tag.TAG_STRING)
                || !payload.contains(X_TAG, Tag.TAG_INT)
                || !payload.contains(Y_TAG, Tag.TAG_INT)
                || !payload.contains(Z_TAG, Tag.TAG_INT)) {
            return Optional.empty();
        }
        ResourceLocation dimension = ResourceLocation.tryParse(payload.getString(DIMENSION_TAG));
        if (dimension == null) {
            return Optional.empty();
        }
        return Optional.of(new Selection(
                dimension,
                new BlockPos(payload.getInt(X_TAG), payload.getInt(Y_TAG), payload.getInt(Z_TAG))
        ));
    }

    public static void clear(CompoundTag owner) {
        Objects.requireNonNull(owner).remove(OWNER_TAG);
    }

    public record Selection(ResourceLocation dimension, BlockPos position) {
        public Selection {
            Objects.requireNonNull(dimension);
            position = Objects.requireNonNull(position).immutable();
        }
    }
}
