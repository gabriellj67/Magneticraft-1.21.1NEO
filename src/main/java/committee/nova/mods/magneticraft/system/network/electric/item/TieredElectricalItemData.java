package committee.nova.mods.magneticraft.system.network.electric.item;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/** Versioned item-owned electrical identity; it never contains node energy. */
public record TieredElectricalItemData(
        ResourceLocation tierId,
        Optional<ResourceLocation> ratingId,
        Optional<ResourceLocation> transformerProfileId
) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_ID_LENGTH = 128;
    public static final String OWNER_TAG = "magneticraft_electrical";

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String TIER_ID_TAG = "tier_id";
    private static final String RATING_ID_TAG = "rating_id";
    private static final String TRANSFORMER_PROFILE_ID_TAG = "transformer_profile_id";

    public TieredElectricalItemData {
        tierId = requireId(tierId, "tierId");
        ratingId = normalized(ratingId, "ratingId");
        transformerProfileId = normalized(transformerProfileId, "transformerProfileId");
    }

    public static TieredElectricalItemData forTier(ResourceLocation tierId) {
        return new TieredElectricalItemData(tierId, Optional.empty(), Optional.empty());
    }

    public void write(ItemStack stack) {
        Objects.requireNonNull(stack, "stack").addTagElement(OWNER_TAG, toTag());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putString(TIER_ID_TAG, tierId.toString());
        ratingId.ifPresent(id -> tag.putString(RATING_ID_TAG, id.toString()));
        transformerProfileId.ifPresent(id -> tag.putString(TRANSFORMER_PROFILE_ID_TAG, id.toString()));
        return tag;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        json.addProperty(TIER_ID_TAG, tierId.toString());
        ratingId.ifPresent(id -> json.addProperty(RATING_ID_TAG, id.toString()));
        transformerProfileId.ifPresent(id -> json.addProperty(TRANSFORMER_PROFILE_ID_TAG, id.toString()));
        return json;
    }

    public void writeNetwork(FriendlyByteBuf buffer) {
        writeId(buffer, tierId);
        writeOptionalId(buffer, ratingId);
        writeOptionalId(buffer, transformerProfileId);
    }

    public String subtypeKey() {
        return "tier=" + tierId
                + ";rating=" + ratingId.map(ResourceLocation::toString).orElse("")
                + ";transformer=" + transformerProfileId.map(ResourceLocation::toString).orElse("");
    }

    public static Optional<TieredElectricalItemData> read(ItemStack stack) {
        CompoundTag owner = Objects.requireNonNull(stack, "stack").getTag();
        if (owner == null || !owner.contains(OWNER_TAG, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return fromTag(owner.getCompound(OWNER_TAG));
    }

    public static Optional<TieredElectricalItemData> fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(TIER_ID_TAG, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Optional<ResourceLocation> tier = parseId(tag.getString(TIER_ID_TAG));
        Optional<Optional<ResourceLocation>> rating = readOptionalId(tag, RATING_ID_TAG);
        Optional<Optional<ResourceLocation>> transformer = readOptionalId(tag, TRANSFORMER_PROFILE_ID_TAG);
        if (tier.isEmpty() || rating.isEmpty() || transformer.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TieredElectricalItemData(tier.get(), rating.get(), transformer.get()));
    }

    public static TieredElectricalItemData fromJson(JsonObject json) {
        Objects.requireNonNull(json, "json");
        int schemaVersion = GsonHelper.getAsInt(json, SCHEMA_VERSION_TAG);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported electrical item schema " + schemaVersion);
        }
        ResourceLocation tier = parseRequiredId(GsonHelper.getAsString(json, TIER_ID_TAG), TIER_ID_TAG);
        Optional<ResourceLocation> rating = parseOptionalJsonId(json, RATING_ID_TAG);
        Optional<ResourceLocation> transformer = parseOptionalJsonId(json, TRANSFORMER_PROFILE_ID_TAG);
        return new TieredElectricalItemData(tier, rating, transformer);
    }

    public static TieredElectricalItemData readNetwork(FriendlyByteBuf buffer) {
        ResourceLocation tier = readId(buffer);
        Optional<ResourceLocation> rating = readOptionalId(buffer);
        Optional<ResourceLocation> transformer = readOptionalId(buffer);
        return new TieredElectricalItemData(tier, rating, transformer);
    }

    private static Optional<Optional<ResourceLocation>> readOptionalId(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return Optional.of(Optional.empty());
        }
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Optional<ResourceLocation> parsed = parseId(tag.getString(key));
        return parsed.isEmpty() ? Optional.empty() : Optional.of(parsed);
    }

    private static Optional<ResourceLocation> parseOptionalJsonId(JsonObject json, String key) {
        return json.has(key)
                ? Optional.of(parseRequiredId(GsonHelper.getAsString(json, key), key))
                : Optional.empty();
    }

    private static Optional<ResourceLocation> parseId(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_ID_LENGTH) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    private static ResourceLocation parseRequiredId(String value, String field) {
        return parseId(value).orElseThrow(() -> new IllegalArgumentException(field + " must be a valid ResourceLocation"));
    }

    private static ResourceLocation requireId(ResourceLocation id, String name) {
        ResourceLocation required = Objects.requireNonNull(id, name);
        if (required.toString().length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(name + " exceeds " + MAX_ID_LENGTH + " characters");
        }
        return required;
    }

    private static Optional<ResourceLocation> normalized(Optional<ResourceLocation> value, String name) {
        Optional<ResourceLocation> required = Objects.requireNonNull(value, name);
        required.ifPresent(id -> requireId(id, name));
        return required;
    }

    private static void writeId(FriendlyByteBuf buffer, ResourceLocation id) {
        buffer.writeUtf(id.toString(), MAX_ID_LENGTH);
    }

    private static ResourceLocation readId(FriendlyByteBuf buffer) {
        return parseRequiredId(buffer.readUtf(MAX_ID_LENGTH), "network electrical id");
    }

    private static void writeOptionalId(FriendlyByteBuf buffer, Optional<ResourceLocation> id) {
        buffer.writeBoolean(id.isPresent());
        id.ifPresent(value -> writeId(buffer, value));
    }

    private static Optional<ResourceLocation> readOptionalId(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(readId(buffer)) : Optional.empty();
    }
}
