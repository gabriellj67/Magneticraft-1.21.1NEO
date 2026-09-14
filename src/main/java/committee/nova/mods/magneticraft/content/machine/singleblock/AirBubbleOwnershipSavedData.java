package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Durable per-dimension ownership for air bubbles whose airlock may be in another chunk. */
public final class AirBubbleOwnershipSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    private static final String DATA_NAME = "magneticraft_air_bubble_ownership";
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String OWNERSHIP_TAG = "ownership";
    private static final String BUBBLE_POSITION_TAG = "bubble_position";
    private static final String AIRLOCK_POSITION_TAG = "airlock_position";

    private final Map<BlockPos, BlockPos> airlocksByBubble = new HashMap<>();

    private static final SavedData.Factory<AirBubbleOwnershipSavedData> FACTORY = new SavedData.Factory<>(
            AirBubbleOwnershipSavedData::new,
            AirBubbleOwnershipSavedData::load,
            null
    );

    public static AirBubbleOwnershipSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static AirBubbleOwnershipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AirBubbleOwnershipSavedData data = new AirBubbleOwnershipSavedData();
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(OWNERSHIP_TAG, Tag.TAG_LIST)) {
            return data;
        }
        ListTag ownership = tag.getList(OWNERSHIP_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : ownership) {
            if (!(rawEntry instanceof CompoundTag entry)
                    || !entry.contains(BUBBLE_POSITION_TAG, Tag.TAG_LONG)
                    || !entry.contains(AIRLOCK_POSITION_TAG, Tag.TAG_LONG)) {
                continue;
            }
            BlockPos bubble = BlockPos.of(entry.getLong(BUBBLE_POSITION_TAG));
            BlockPos airlock = BlockPos.of(entry.getLong(AIRLOCK_POSITION_TAG));
            data.airlocksByBubble.putIfAbsent(bubble, airlock);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ListTag ownership = new ListTag();
        airlocksByBubble.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .forEach(entry -> {
                    CompoundTag ownershipEntry = new CompoundTag();
                    ownershipEntry.putLong(BUBBLE_POSITION_TAG, entry.getKey().asLong());
                    ownershipEntry.putLong(AIRLOCK_POSITION_TAG, entry.getValue().asLong());
                    ownership.add(ownershipEntry);
                });
        tag.put(OWNERSHIP_TAG, ownership);
        return tag;
    }

    public void bind(BlockPos bubblePosition, BlockPos airlockPosition) {
        BlockPos bubble = bubblePosition.immutable();
        BlockPos airlock = airlockPosition.immutable();
        if (!airlock.equals(airlocksByBubble.put(bubble, airlock))) {
            setDirty();
        }
    }

    public Optional<BlockPos> ownerOf(BlockPos bubblePosition) {
        return Optional.ofNullable(airlocksByBubble.get(bubblePosition));
    }

    public boolean unbindBubble(BlockPos bubblePosition) {
        if (airlocksByBubble.remove(bubblePosition) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public List<BlockPos> unbindAirlock(BlockPos airlockPosition) {
        List<BlockPos> released = new ArrayList<>();
        var iterator = airlocksByBubble.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockPos> entry = iterator.next();
            if (entry.getValue().equals(airlockPosition)) {
                released.add(entry.getKey());
                iterator.remove();
            }
        }
        if (!released.isEmpty()) {
            released.sort(Comparator.comparingLong(BlockPos::asLong));
            setDirty();
        }
        return List.copyOf(released);
    }

    public int size() {
        return airlocksByBubble.size();
    }
}
