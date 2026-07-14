package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Rebuildable loaded-endpoint cache around the durable per-dimension graph.
 * Missing endpoints pause an edge; this service never loads a chunk.
 */
public final class LongDistanceElectricityService {
    private static final int MAX_AUTOMATIC_POLE_CONNECTIONS = 16;
    private static final Map<ServerLevel, LongDistanceElectricityService> SERVICES = new WeakHashMap<>();

    private final ServerLevel level;
    private final Map<BlockPos, LongDistanceEndpointHost> endpoints = new HashMap<>();
    private final Map<BlockPos, WirelessEnergyReceiverHost> wirelessReceivers = new HashMap<>();
    private long lastTick = Long.MIN_VALUE;

    private LongDistanceElectricityService(ServerLevel level) {
        this.level = level;
    }

    public static synchronized LongDistanceElectricityService get(ServerLevel level) {
        return SERVICES.computeIfAbsent(level, LongDistanceElectricityService::new);
    }

    public static synchronized void discard(ServerLevel level) {
        SERVICES.remove(level);
    }

    public void register(LongDistanceEndpointHost endpoint) {
        BlockPos position = endpoint.position().immutable();
        endpoints.put(position, endpoint);
        endpoint.requestClientSync();
        autoConnectPole(endpoint);
    }

    public void unregister(LongDistanceEndpointHost endpoint) {
        endpoints.remove(endpoint.position(), endpoint);
    }

    public void registerReceiver(WirelessEnergyReceiverHost receiver) {
        wirelessReceivers.put(receiver.position().immutable(), receiver);
    }

    public void unregisterReceiver(WirelessEnergyReceiverHost receiver) {
        wirelessReceivers.remove(receiver.position(), receiver);
    }

    public ConnectionAttempt connect(BlockPos firstPosition, BlockPos secondPosition) {
        if (firstPosition.equals(secondPosition)) {
            return new ConnectionAttempt(ConnectionResult.SAME_ENDPOINT, 0);
        }
        LongDistanceEndpointHost first = endpoints.get(firstPosition);
        LongDistanceEndpointHost second = endpoints.get(secondPosition);
        if (first == null || second == null) {
            return new ConnectionAttempt(ConnectionResult.ENDPOINT_UNLOADED, 0);
        }

        Set<LongDistancePort> compatible = EnumSet.noneOf(LongDistancePort.class);
        compatible.addAll(first.longDistancePorts());
        compatible.retainAll(second.longDistancePorts());
        if (compatible.isEmpty()) {
            return new ConnectionAttempt(ConnectionResult.INCOMPATIBLE_PORT, 0);
        }

        LongDistanceElectricitySavedData data = LongDistanceElectricitySavedData.get(level);
        int added = 0;
        boolean tooFar = false;
        boolean alreadyConnected = false;
        for (LongDistancePort port : compatible) {
            var result = data.connect(
                    new LongDistanceEndpoint(firstPosition, port),
                    new LongDistanceEndpoint(secondPosition, port)
            );
            switch (result) {
                case SUCCESS -> added++;
                case TOO_FAR -> tooFar = true;
                case ALREADY_CONNECTED -> alreadyConnected = true;
                default -> {
                }
            }
        }
        ConnectionResult result = selectConnectionResult(added, alreadyConnected, tooFar);
        if (result == ConnectionResult.SUCCESS) {
            first.requestClientSync();
            second.requestClientSync();
        }
        return new ConnectionAttempt(result, added);
    }

    static ConnectionResult selectConnectionResult(int added, boolean alreadyConnected, boolean tooFar) {
        if (added > 0) {
            return ConnectionResult.SUCCESS;
        }
        if (alreadyConnected) {
            return ConnectionResult.ALREADY_CONNECTED;
        }
        return tooFar ? ConnectionResult.TOO_FAR : ConnectionResult.INCOMPATIBLE_PORT;
    }

    public int removeConnectionsAt(BlockPos position) {
        LongDistanceElectricitySavedData data = LongDistanceElectricitySavedData.get(level);
        List<LongDistanceConnection> removedConnections = data.connectionsAt(position);
        int removed = data.removeAt(position);
        if (removed > 0) {
            removedConnections.forEach(connection -> {
                requestSync(connection.first().position());
                requestSync(connection.second().position());
            });
        }
        return removed;
    }

    public boolean hasConnections(BlockPos position) {
        return !LongDistanceElectricitySavedData.get(level).connectionsAt(position).isEmpty();
    }

    public List<LongDistanceConnection> connectionsAt(BlockPos position) {
        return LongDistanceElectricitySavedData.get(level).connectionsAt(position);
    }

    public Collection<WirelessEnergyReceiverHost> receiversWithin(BlockPos center, double radius) {
        return wirelessReceivers.values().stream()
                .filter(receiver -> Math.abs(receiver.position().getX() - center.getX()) <= radius
                        && Math.abs(receiver.position().getY() - center.getY()) <= radius
                        && Math.abs(receiver.position().getZ() - center.getZ()) <= radius)
                .sorted(Comparator
                        .comparingDouble((WirelessEnergyReceiverHost receiver) ->
                                receiver.position().distSqr(center))
                        .thenComparingLong(receiver -> receiver.position().asLong()))
                .toList();
    }

    public void tick(long gameTime) {
        if (lastTick == gameTime) {
            return;
        }
        lastTick = gameTime;
        for (LongDistanceConnection connection : LongDistanceElectricitySavedData.get(level).connections()) {
            LongDistanceEndpointHost first = endpoints.get(connection.first().position());
            LongDistanceEndpointHost second = endpoints.get(connection.second().position());
            if (first == null || second == null
                    || !first.longDistancePorts().contains(connection.first().port())
                    || !second.longDistancePorts().contains(connection.second().port())) {
                continue;
            }
            first.electricity().exchangeLongDistance(second.electricity(), connection.distance());
        }
    }

    private void autoConnectPole(LongDistanceEndpointHost endpoint) {
        if (!endpoint.longDistancePorts().contains(LongDistancePort.POLE)) {
            return;
        }
        LongDistanceElectricitySavedData data = LongDistanceElectricitySavedData.get(level);
        long existing = data.connectionsAt(endpoint.position()).stream()
                .filter(connection -> connection.first().port() == LongDistancePort.POLE)
                .count();
        if (existing >= MAX_AUTOMATIC_POLE_CONNECTIONS) {
            return;
        }

        List<LongDistanceEndpointHost> candidates = new ArrayList<>(endpoints.values());
        candidates.sort((first, second) -> {
            int distance = Double.compare(
                    first.position().distSqr(endpoint.position()),
                    second.position().distSqr(endpoint.position())
            );
            return distance != 0 ? distance : Long.compare(first.position().asLong(), second.position().asLong());
        });
        for (LongDistanceEndpointHost candidate : candidates) {
            if (candidate == endpoint
                    || !candidate.longDistancePorts().contains(LongDistancePort.POLE)
                    || existing >= MAX_AUTOMATIC_POLE_CONNECTIONS) {
                continue;
            }
            var result = data.connect(
                    new LongDistanceEndpoint(endpoint.position(), LongDistancePort.POLE),
                    new LongDistanceEndpoint(candidate.position(), LongDistancePort.POLE)
            );
            if (result == LongDistanceConnectionGraph.ConnectResult.SUCCESS) {
                existing++;
                endpoint.requestClientSync();
                candidate.requestClientSync();
            }
        }
    }

    private void requestSync(BlockPos position) {
        LongDistanceEndpointHost endpoint = endpoints.get(position);
        if (endpoint != null) {
            endpoint.requestClientSync();
        }
    }

    public record ConnectionAttempt(ConnectionResult result, int connectionsAdded) {
    }

    public enum ConnectionResult {
        SUCCESS,
        ENDPOINT_UNLOADED,
        SAME_ENDPOINT,
        INCOMPATIBLE_PORT,
        TOO_FAR,
        ALREADY_CONNECTED
    }
}
