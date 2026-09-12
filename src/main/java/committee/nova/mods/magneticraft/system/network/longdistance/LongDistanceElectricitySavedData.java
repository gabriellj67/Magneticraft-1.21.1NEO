package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Optional;

/** Durable per-dimension wire edges; loaded endpoint objects are never saved. */
public final class LongDistanceElectricitySavedData extends SavedData {
    public static final int SCHEMA_VERSION = 2;

    private static final String DATA_NAME = "magneticraft_long_distance_electricity";
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String CONNECTIONS_TAG = "connections";
    private static final String FIRST_TAG = "first";
    private static final String SECOND_TAG = "second";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String PORT_TAG = "port";
    private static final String TERMINAL_ID_TAG = "terminal_id";
    private static final String TIER_ID_TAG = "tier_id";

    private final LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();

    private static final SavedData.Factory<LongDistanceElectricitySavedData> FACTORY = new SavedData.Factory<>(
            LongDistanceElectricitySavedData::new,
            LongDistanceElectricitySavedData::load,
            null
    );

    public static LongDistanceElectricitySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static LongDistanceElectricitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LongDistanceElectricitySavedData data = new LongDistanceElectricitySavedData();
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(CONNECTIONS_TAG, Tag.TAG_LIST)) {
            return data;
        }
        ListTag connections = tag.getList(CONNECTIONS_TAG, Tag.TAG_COMPOUND);
        for (Tag rawConnection : connections) {
            if (!(rawConnection instanceof CompoundTag connectionTag)) {
                continue;
            }
            Optional<LongDistanceEndpoint> first = readEndpoint(connectionTag, FIRST_TAG);
            Optional<LongDistanceEndpoint> second = readEndpoint(connectionTag, SECOND_TAG);
            if (first.isPresent() && second.isPresent()) {
                data.graph.restore(first.get(), second.get());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ListTag connections = new ListTag();
        graph.connections().forEach(connection -> {
            CompoundTag connectionTag = new CompoundTag();
            connectionTag.put(FIRST_TAG, writeEndpoint(connection.first()));
            connectionTag.put(SECOND_TAG, writeEndpoint(connection.second()));
            connections.add(connectionTag);
        });
        tag.put(CONNECTIONS_TAG, connections);
        return tag;
    }

    public LongDistanceConnectionGraph.ConnectResult connect(
            LongDistanceEndpoint first,
            LongDistanceEndpoint second,
            int maxDistance
    ) {
        LongDistanceConnectionGraph.ConnectResult result = graph.connect(first, second, maxDistance);
        if (result == LongDistanceConnectionGraph.ConnectResult.SUCCESS) {
            setDirty();
        }
        return result;
    }

    public boolean disconnect(LongDistanceConnection connection) {
        boolean removed = graph.disconnect(connection);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int removeAt(BlockPos position) {
        int removed = graph.removeAt(position);
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public List<LongDistanceConnection> connections() {
        return graph.connections();
    }

    public List<LongDistanceConnection> connectionsAt(BlockPos position) {
        return graph.connectionsAt(position);
    }

    public long mutationVersion() {
        return graph.mutationVersion();
    }

    private static CompoundTag writeEndpoint(LongDistanceEndpoint endpoint) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(X_TAG, endpoint.position().getX());
        tag.putInt(Y_TAG, endpoint.position().getY());
        tag.putInt(Z_TAG, endpoint.position().getZ());
        tag.putString(TERMINAL_ID_TAG, endpoint.terminalId().toString());
        tag.putString(PORT_TAG, endpoint.port().serializedName());
        tag.putString(TIER_ID_TAG, endpoint.tierId().toString());
        return tag;
    }

    private static Optional<LongDistanceEndpoint> readEndpoint(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = parent.getCompound(key);
        if (!tag.contains(X_TAG, Tag.TAG_INT)
                || !tag.contains(Y_TAG, Tag.TAG_INT)
                || !tag.contains(Z_TAG, Tag.TAG_INT)
                || !tag.contains(TERMINAL_ID_TAG, Tag.TAG_STRING)
                || !tag.contains(TIER_ID_TAG, Tag.TAG_STRING)
                || !tag.contains(PORT_TAG, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        ResourceLocation terminalId = ResourceLocation.tryParse(tag.getString(TERMINAL_ID_TAG));
        ResourceLocation tierId = ResourceLocation.tryParse(tag.getString(TIER_ID_TAG));
        if (terminalId == null || tierId == null) {
            return Optional.empty();
        }
        return LongDistancePort.byName(tag.getString(PORT_TAG)).map(port -> new LongDistanceEndpoint(
                new BlockPos(tag.getInt(X_TAG), tag.getInt(Y_TAG), tag.getInt(Z_TAG)),
                terminalId,
                port,
                tierId
        ));
    }
}
