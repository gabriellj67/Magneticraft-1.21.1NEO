package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loader-independent authoritative wire topology. It mutates only when a wire
 * is explicitly added or removed; simulation ticks never rebuild it.
 */
public final class LongDistanceConnectionGraph {
    private static final Comparator<LongDistanceConnection> CONNECTION_ORDER = Comparator
            .comparingLong((LongDistanceConnection connection) -> connection.first().position().asLong())
            .thenComparingInt(connection -> connection.first().port().ordinal())
            .thenComparingLong(connection -> connection.second().position().asLong())
            .thenComparingInt(connection -> connection.second().port().ordinal());

    private final Set<LongDistanceConnection> connections = new LinkedHashSet<>();
    private long mutationVersion;
    private long snapshotVersion = -1L;
    private List<LongDistanceConnection> connectionSnapshot = List.of();

    public ConnectResult connect(LongDistanceEndpoint first, LongDistanceEndpoint second) {
        if (first.position().equals(second.position())) {
            return ConnectResult.SAME_ENDPOINT;
        }
        if (first.port() != second.port()) {
            return ConnectResult.INCOMPATIBLE_PORT;
        }
        double maxDistance = Math.min(first.port().maxDistance(), second.port().maxDistance());
        if (first.position().distSqr(second.position()) > maxDistance * maxDistance) {
            return ConnectResult.TOO_FAR;
        }
        LongDistanceConnection connection = LongDistanceConnection.of(first, second);
        if (!connections.add(connection)) {
            return ConnectResult.ALREADY_CONNECTED;
        }
        mutationVersion++;
        return ConnectResult.SUCCESS;
    }

    public boolean disconnect(LongDistanceConnection connection) {
        if (!connections.remove(connection)) {
            return false;
        }
        mutationVersion++;
        return true;
    }

    public int removeAt(BlockPos position) {
        int before = connections.size();
        connections.removeIf(connection -> connection.containsPosition(position));
        int removed = before - connections.size();
        if (removed > 0) {
            mutationVersion++;
        }
        return removed;
    }

    public List<LongDistanceConnection> connectionsAt(BlockPos position) {
        return connections().stream()
                .filter(connection -> connection.containsPosition(position))
                .toList();
    }

    public List<LongDistanceConnection> connections() {
        if (snapshotVersion == mutationVersion) {
            return connectionSnapshot;
        }
        ArrayList<LongDistanceConnection> snapshot = new ArrayList<>(connections);
        snapshot.sort(CONNECTION_ORDER);
        connectionSnapshot = List.copyOf(snapshot);
        snapshotVersion = mutationVersion;
        return connectionSnapshot;
    }

    public int size() {
        return connections.size();
    }

    public long mutationVersion() {
        return mutationVersion;
    }

    public enum ConnectResult {
        SUCCESS,
        SAME_ENDPOINT,
        INCOMPATIBLE_PORT,
        TOO_FAR,
        ALREADY_CONNECTED
    }
}
