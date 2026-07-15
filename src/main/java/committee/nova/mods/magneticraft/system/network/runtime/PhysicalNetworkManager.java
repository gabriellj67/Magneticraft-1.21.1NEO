package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.core.GraphMetrics;
import committee.nova.mods.magneticraft.system.network.core.IncrementalGraph;
import committee.nova.mods.magneticraft.system.network.core.WeightedPathfinder;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsRouteDecision;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Per-dimension runtime topology. Node state remains owned by block entities.
 */
public final class PhysicalNetworkManager {
    public static final int MAX_LOGISTICS_ROUTE_VISITS = 4_096;
    private static final int MAX_LOGISTICS_ROUTE_CACHE_ENTRIES = 4_096;

    private final ServerLevel level;
    private final Map<NetworkDomain, IncrementalGraph<Long, PhysicalNetworkNode>> graphs =
            new EnumMap<>(NetworkDomain.class);
    private long lastTick = Long.MIN_VALUE;
    private long edgeTicks;
    private int electricalPauseTicks;
    private int electricalDamageGraceTicks;
    private long logisticsCacheTopologyVersion = Long.MIN_VALUE;
    private final Map<LogisticsCacheKey, CachedLogisticsRoute> logisticsRouteCache =
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<LogisticsCacheKey, CachedLogisticsRoute> eldest) {
                    return size() > MAX_LOGISTICS_ROUTE_CACHE_ENTRIES;
                }
            };

    PhysicalNetworkManager(ServerLevel level) {
        this.level = level;
        for (NetworkDomain domain : NetworkDomain.values()) {
            graphs.put(domain, new IncrementalGraph<>());
        }
    }

    public ServerLevel level() {
        return level;
    }

    public void register(PhysicalNetworkNode node) {
        IncrementalGraph<Long, PhysicalNetworkNode> graph = graph(node.domain());
        long key = node.position().asLong();
        graph.removeNode(key);
        graph.addNode(key, node);
        refreshAt(node.domain(), node.position());
        for (Direction direction : Direction.values()) {
            refreshAt(node.domain(), node.position().relative(direction));
        }
    }

    public void unregister(PhysicalNetworkNode node) {
        graph(node.domain()).removeNode(node.position().asLong());
    }

    public void update(PhysicalNetworkNode node) {
        IncrementalGraph<Long, PhysicalNetworkNode> graph = graph(node.domain());
        if (!graph.contains(node.position().asLong())) {
            register(node);
            return;
        }
        graph.addNode(node.position().asLong(), node);
        refreshAt(node.domain(), node.position());
        for (Direction direction : Direction.values()) {
            refreshAt(node.domain(), node.position().relative(direction));
        }
    }

    public void tick(long gameTime) {
        if (lastTick == gameTime) {
            return;
        }
        lastTick = gameTime;

        boolean pauseElectrical = electricalPauseTicks > 0;

        for (NetworkDomain domain : NetworkDomain.values()) {
            if (domain == NetworkDomain.ELECTRICITY && pauseElectrical) {
                continue;
            }
            IncrementalGraph<Long, PhysicalNetworkNode> graph = graph(domain);
            graph.values().forEach(node -> node.beforeNetworkTick(this));
            for (long firstKey : graph.keys()) {
                for (long secondKey : graph.neighbors(firstKey)) {
                    if (Long.compare(firstKey, secondKey) < 0) {
                        PhysicalNetworkNode first = graph.value(firstKey).orElse(null);
                        PhysicalNetworkNode second = graph.value(secondKey).orElse(null);
                        if (first != null && second != null) {
                            first.exchangeWith(second);
                            edgeTicks++;
                        }
                    }
                }
            }
            graph.values().forEach(node -> node.afterNetworkTick(this));
        }
        if (electricalPauseTicks > 0) {
            electricalPauseTicks--;
        }
        if (electricalDamageGraceTicks > 0) {
            electricalDamageGraceTicks--;
        }
    }

    public void onElectricalProfilesReloaded(ElectricalDataSnapshot snapshot, int graceTicks) {
        graph(NetworkDomain.ELECTRICITY).values().forEach(node -> {
            if (node instanceof ElectricalProfileBinding binding) {
                binding.rebindElectricalProfile(snapshot);
            }
        });
        electricalPauseTicks = 1;
        electricalDamageGraceTicks = Math.max(0, graceTicks);
    }

    public boolean electricalSimulationPaused() {
        return electricalPauseTicks > 0;
    }

    public boolean electricalDamageSuppressed() {
        return electricalDamageGraceTicks > 0;
    }

    public int electricalDamageGraceTicks() {
        return electricalDamageGraceTicks;
    }

    public Optional<PhysicalNetworkNode> node(NetworkDomain domain, BlockPos position) {
        return graph(domain).value(position.asLong());
    }

    public Set<Long> neighbors(NetworkDomain domain, BlockPos position) {
        return graph(domain).neighbors(position.asLong());
    }

    public Set<Long> component(NetworkDomain domain, BlockPos position) {
        return graph(domain).componentOf(position.asLong());
    }

    public int nodeCount(NetworkDomain domain) {
        return graph(domain).size();
    }

    public int componentCount(NetworkDomain domain) {
        return graph(domain).componentCount();
    }

    public GraphMetrics metrics(NetworkDomain domain) {
        return graph(domain).metrics();
    }

    public long edgeTicks() {
        return edgeTicks;
    }

    public Optional<LogisticsRouteDecision> findLogisticsRoute(
            BlockPos start,
            Direction incoming,
            ItemStack stack,
            int roundRobinOffset,
            int maxVisited
    ) {
        IncrementalGraph<Long, PhysicalNetworkNode> graph = graph(NetworkDomain.LOGISTICS);
        LogisticsNetworkNode startNode = logisticsNode(graph, start.asLong());
        if (startNode == null) {
            return Optional.empty();
        }

        int visitLimit = Math.max(1, Math.min(maxVisited, MAX_LOGISTICS_ROUTE_VISITS));
        long topologyVersion = graph.metrics().topologyVersion();
        if (logisticsCacheTopologyVersion != topologyVersion) {
            logisticsRouteCache.clear();
            logisticsCacheTopologyVersion = topologyVersion;
        }

        List<Direction> localOutputs = startNode.acceptingExternalOutputs(stack).stream()
                .filter(direction -> direction != incoming)
                .toList();
        if (!localOutputs.isEmpty()) {
            Direction output = localOutputs.get(Math.floorMod(roundRobinOffset, localOutputs.size()));
            return Optional.of(new LogisticsRouteDecision(output, true, 1, false));
        }

        long blockedFirst = incoming == null ? Long.MIN_VALUE : start.relative(incoming).asLong();
        List<Long> firstNeighbors = orderedFirstNeighbors(graph, start, blockedFirst);
        long preferredFirst = firstNeighbors.isEmpty()
                ? Long.MIN_VALUE
                : firstNeighbors.get(Math.floorMod(roundRobinOffset, firstNeighbors.size()));
        LogisticsCacheKey cacheKey = new LogisticsCacheKey(start.asLong(), incoming, preferredFirst, visitLimit);
        CachedLogisticsRoute cached = logisticsRouteCache.get(cacheKey);
        if (cached != null && cached.matches(graph, stack, topologyVersion)) {
            return Optional.of(new LogisticsRouteDecision(cached.direction(), false, 0, false));
        }

        WeightedPathfinder.SearchResult<Long> result = searchLogisticsRoute(
                graph,
                start,
                stack,
                blockedFirst,
                preferredFirst,
                visitLimit
        );
        if (result.path().isEmpty() && incoming != null && !result.truncated()) {
            firstNeighbors = orderedFirstNeighbors(graph, start, Long.MIN_VALUE);
            preferredFirst = firstNeighbors.isEmpty()
                    ? Long.MIN_VALUE
                    : firstNeighbors.get(Math.floorMod(roundRobinOffset, firstNeighbors.size()));
            cacheKey = new LogisticsCacheKey(start.asLong(), incoming, preferredFirst, visitLimit);
            cached = logisticsRouteCache.get(cacheKey);
            if (cached != null && cached.matches(graph, stack, topologyVersion)) {
                return Optional.of(new LogisticsRouteDecision(cached.direction(), false, 0, false));
            }
            result = searchLogisticsRoute(
                    graph,
                    start,
                    stack,
                    Long.MIN_VALUE,
                    preferredFirst,
                    visitLimit
            );
        }
        if (result.path().isEmpty() || result.path().get().size() < 2) {
            return Optional.empty();
        }
        BlockPos next = BlockPos.of(result.path().get().get(1));
        Direction direction = directionBetween(start, next);
        if (direction == null) {
            return Optional.empty();
        }
        long destination = result.path().get().get(result.path().get().size() - 1);
        logisticsRouteCache.put(
                cacheKey,
                new CachedLogisticsRoute(
                        stack.copy(), direction, start.asLong(), next.asLong(), destination, topologyVersion
                )
        );
        return Optional.of(new LogisticsRouteDecision(
                direction,
                false,
                result.visitedNodes(),
                result.truncated()
        ));
    }

    private WeightedPathfinder.SearchResult<Long> searchLogisticsRoute(
            IncrementalGraph<Long, PhysicalNetworkNode> graph,
            BlockPos start,
            ItemStack stack,
            long blockedFirst,
            long preferredFirst,
            int maxVisited
    ) {
        return WeightedPathfinder.find(
                start.asLong(),
                key -> {
                    LogisticsNetworkNode node = logisticsNode(graph, key);
                    return node != null && !node.acceptingExternalOutputs(stack).isEmpty();
                },
                key -> {
                    ArrayList<WeightedPathfinder.WeightedEdge<Long>> edges = new ArrayList<>();
                    for (long neighbor : orderedNeighbors(graph, key)) {
                        if (key == start.asLong() && neighbor == blockedFirst) {
                            continue;
                        }
                        LogisticsNetworkNode node = logisticsNode(graph, neighbor);
                        if (node != null) {
                            int baseCost = key == start.asLong()
                                    ? (neighbor == preferredFirst ? 0 : 1)
                                    : 2;
                            edges.add(new WeightedPathfinder.WeightedEdge<>(neighbor, baseCost + node.routingWeight()));
                        }
                    }
                    return edges;
                },
                maxVisited
        );
    }

    private static List<Long> orderedFirstNeighbors(
            IncrementalGraph<Long, PhysicalNetworkNode> graph,
            BlockPos start,
            long blockedFirst
    ) {
        return orderedNeighbors(graph, start.asLong()).stream()
                .filter(neighbor -> neighbor != blockedFirst)
                .toList();
    }

    private static List<Long> orderedNeighbors(
            IncrementalGraph<Long, PhysicalNetworkNode> graph,
            long key
    ) {
        BlockPos position = BlockPos.of(key);
        return graph.neighbors(key).stream()
                .sorted(Comparator.comparingInt(neighbor -> {
                    Direction direction = directionBetween(position, BlockPos.of(neighbor));
                    return direction == null ? Integer.MAX_VALUE : direction.ordinal();
                }))
                .toList();
    }

    private void refreshAt(NetworkDomain domain, BlockPos position) {
        IncrementalGraph<Long, PhysicalNetworkNode> graph = graph(domain);
        PhysicalNetworkNode node = graph.value(position.asLong()).orElse(null);
        if (node == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            long neighborKey = position.relative(direction).asLong();
            PhysicalNetworkNode neighbor = graph.value(neighborKey).orElse(null);
            boolean connected = neighbor != null
                    && node.canConnect(direction, neighbor)
                    && neighbor.canConnect(direction.getOpposite(), node);
            if (connected) {
                graph.connect(position.asLong(), neighborKey);
            } else {
                graph.disconnect(position.asLong(), neighborKey);
            }
        }
    }

    private IncrementalGraph<Long, PhysicalNetworkNode> graph(NetworkDomain domain) {
        return graphs.get(domain);
    }

    private static LogisticsNetworkNode logisticsNode(
            IncrementalGraph<Long, PhysicalNetworkNode> graph,
            long key
    ) {
        PhysicalNetworkNode node = graph.value(key).orElse(null);
        return node instanceof LogisticsNetworkNode logistics ? logistics : null;
    }

    private static Direction directionBetween(BlockPos start, BlockPos next) {
        int x = next.getX() - start.getX();
        int y = next.getY() - start.getY();
        int z = next.getZ() - start.getZ();
        for (Direction direction : Direction.values()) {
            if (direction.getStepX() == x && direction.getStepY() == y && direction.getStepZ() == z) {
                return direction;
            }
        }
        return null;
    }

    private record LogisticsCacheKey(long start, Direction incoming, long preferredFirst, int visitLimit) {
    }

    private record CachedLogisticsRoute(
            ItemStack stack,
            Direction direction,
            long startNode,
            long firstNode,
            long destination,
            long topologyVersion
    ) {
        private boolean matches(
                IncrementalGraph<Long, PhysicalNetworkNode> graph,
                ItemStack candidate,
                long currentTopologyVersion
        ) {
            if (topologyVersion != currentTopologyVersion
                    || stack.getCount() != candidate.getCount()
                    || !ItemStack.isSameItemSameTags(stack, candidate)
                    || !graph.neighbors(startNode).contains(firstNode)) {
                return false;
            }
            LogisticsNetworkNode destinationNode = logisticsNode(graph, destination);
            return destinationNode != null && !destinationNode.acceptingExternalOutputs(candidate).isEmpty();
        }
    }
}
