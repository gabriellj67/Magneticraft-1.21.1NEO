package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Rebuildable loaded-endpoint cache around durable per-dimension wire identities.
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

    /** Runs only an already-created loaded-endpoint cache; never creates or loads world state. */
    public static synchronized void tickIfPresent(ServerLevel level, long gameTime) {
        LongDistanceElectricityService service = SERVICES.get(level);
        if (service != null) {
            service.tick(gameTime);
        }
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

    /** Compatibility entry point for fixtures; player tools submit exact endpoint identities. */
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

        int added = 0;
        ConnectionResult fallback = ConnectionResult.INCOMPATIBLE_PORT;
        for (LongDistancePort port : compatible) {
            Optional<LongDistanceEndpoint> firstEndpoint = first.longDistanceEndpoint(port);
            Optional<LongDistanceEndpoint> secondEndpoint = second.longDistanceEndpoint(port);
            if (firstEndpoint.isEmpty() || secondEndpoint.isEmpty()) {
                continue;
            }
            ConnectionAttempt attempt = connect(firstEndpoint.get(), secondEndpoint.get());
            added += attempt.connectionsAdded();
            fallback = preferredFailure(fallback, attempt.result());
        }
        return added > 0 ? new ConnectionAttempt(ConnectionResult.SUCCESS, added) : new ConnectionAttempt(fallback, 0);
    }

    /** Connects exactly the selected terminal/port/tier identities. */
    public ConnectionAttempt connect(LongDistanceEndpoint firstEndpoint, LongDistanceEndpoint secondEndpoint) {
        if (firstEndpoint.position().equals(secondEndpoint.position())) {
            return new ConnectionAttempt(ConnectionResult.SAME_ENDPOINT, 0);
        }
        if (firstEndpoint.port() != secondEndpoint.port()) {
            return new ConnectionAttempt(ConnectionResult.INCOMPATIBLE_PORT, 0);
        }
        if (!firstEndpoint.tierId().equals(secondEndpoint.tierId())) {
            return new ConnectionAttempt(ConnectionResult.INCOMPATIBLE_TIER, 0);
        }

        LongDistanceEndpointHost first = endpoints.get(firstEndpoint.position());
        LongDistanceEndpointHost second = endpoints.get(secondEndpoint.position());
        if (first == null || second == null) {
            return new ConnectionAttempt(ConnectionResult.ENDPOINT_UNLOADED, 0);
        }
        if (!matches(first, firstEndpoint) || !matches(second, secondEndpoint)) {
            return new ConnectionAttempt(ConnectionResult.INCOMPATIBLE_PORT, 0);
        }

        Optional<VoltageTier> tier = ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(firstEndpoint.tierId()));
        if (tier.isEmpty()) {
            return new ConnectionAttempt(ConnectionResult.MISSING_PROFILE, 0);
        }
        int maxDistance = firstEndpoint.port().maxDistance(tier.get());
        LongDistanceConnectionGraph.ConnectResult graphResult = LongDistanceElectricitySavedData.get(level)
                .connect(firstEndpoint, secondEndpoint, maxDistance);
        ConnectionResult result = fromGraph(graphResult);
        if (result == ConnectionResult.SUCCESS) {
            first.requestClientSync();
            second.requestClientSync();
            return new ConnectionAttempt(result, 1);
        }
        return new ConnectionAttempt(result, 0);
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
                        .comparingDouble((WirelessEnergyReceiverHost receiver) -> receiver.position().distSqr(center))
                        .thenComparingLong(receiver -> receiver.position().asLong()))
                .toList();
    }

    public void tick(long gameTime) {
        if (lastTick == gameTime) {
            return;
        }
        lastTick = gameTime;
        PhysicalNetworkManager manager = PhysicalNetworkService.manager(level);
        for (LongDistanceConnection connection : LongDistanceElectricitySavedData.get(level).connections()) {
            LongDistanceEndpointHost first = endpoints.get(connection.first().position());
            LongDistanceEndpointHost second = endpoints.get(connection.second().position());
            if (first == null || second == null
                    || !matches(first, connection.first())
                    || !matches(second, connection.second())
                    || !withinCurrentRange(connection)) {
                continue;
            }
            ElectricalNetworkModule firstModule = first.electricity(connection.first().port());
            ElectricalNetworkModule secondModule = second.electricity(connection.second().port());
            manager.transferElectrical(
                    firstModule.nodeKey(),
                    secondModule.nodeKey(),
                    connection.distance(),
                    ElectricalEdgeTelemetry.EdgeType.LONG_DISTANCE
            );
        }
    }

    private void autoConnectPole(LongDistanceEndpointHost host) {
        Optional<LongDistanceEndpoint> endpoint = host.longDistanceEndpoint(LongDistancePort.POLE);
        if (endpoint.isEmpty()) {
            return;
        }
        LongDistanceElectricitySavedData data = LongDistanceElectricitySavedData.get(level);
        long existing = data.connectionsAt(host.position()).stream()
                .filter(connection -> connection.first().port() == LongDistancePort.POLE)
                .count();
        if (existing >= MAX_AUTOMATIC_POLE_CONNECTIONS) {
            return;
        }

        List<LongDistanceEndpointHost> candidates = new ArrayList<>(endpoints.values());
        candidates.sort((first, second) -> {
            int distance = Double.compare(
                    first.position().distSqr(host.position()),
                    second.position().distSqr(host.position())
            );
            return distance != 0 ? distance : Long.compare(first.position().asLong(), second.position().asLong());
        });
        for (LongDistanceEndpointHost candidate : candidates) {
            if (candidate == host || existing >= MAX_AUTOMATIC_POLE_CONNECTIONS) {
                continue;
            }
            Optional<LongDistanceEndpoint> candidateEndpoint = candidate.longDistanceEndpoint(LongDistancePort.POLE);
            if (candidateEndpoint.isEmpty()) {
                continue;
            }
            ConnectionAttempt result = connect(endpoint.get(), candidateEndpoint.get());
            if (result.result() == ConnectionResult.SUCCESS) {
                existing++;
            }
        }
    }

    private boolean withinCurrentRange(LongDistanceConnection connection) {
        return ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(connection.first().tierId()))
                .filter(tier -> connection.distance() <= connection.first().port().maxDistance(tier))
                .isPresent();
    }

    private static boolean matches(LongDistanceEndpointHost host, LongDistanceEndpoint endpoint) {
        return host.longDistanceEndpoint(endpoint.port()).filter(endpoint::equals).isPresent();
    }

    private void requestSync(BlockPos position) {
        LongDistanceEndpointHost endpoint = endpoints.get(position);
        if (endpoint != null) {
            endpoint.requestClientSync();
        }
    }

    private static ConnectionResult fromGraph(LongDistanceConnectionGraph.ConnectResult result) {
        return switch (result) {
            case SUCCESS -> ConnectionResult.SUCCESS;
            case SAME_ENDPOINT -> ConnectionResult.SAME_ENDPOINT;
            case INCOMPATIBLE_PORT -> ConnectionResult.INCOMPATIBLE_PORT;
            case INCOMPATIBLE_TIER -> ConnectionResult.INCOMPATIBLE_TIER;
            case TOO_FAR -> ConnectionResult.TOO_FAR;
            case ALREADY_CONNECTED -> ConnectionResult.ALREADY_CONNECTED;
        };
    }

    private static ConnectionResult preferredFailure(ConnectionResult current, ConnectionResult candidate) {
        return failurePriority(candidate) > failurePriority(current) ? candidate : current;
    }

    private static int failurePriority(ConnectionResult result) {
        return switch (result) {
            case ALREADY_CONNECTED -> 5;
            case TOO_FAR -> 4;
            case INCOMPATIBLE_TIER -> 3;
            case MISSING_PROFILE -> 2;
            case ENDPOINT_UNLOADED -> 1;
            default -> 0;
        };
    }

    public record ConnectionAttempt(ConnectionResult result, int connectionsAdded) {
    }

    public enum ConnectionResult {
        SUCCESS,
        ENDPOINT_UNLOADED,
        SAME_ENDPOINT,
        INCOMPATIBLE_PORT,
        INCOMPATIBLE_TIER,
        MISSING_PROFILE,
        TOO_FAR,
        ALREADY_CONNECTED
    }
}
