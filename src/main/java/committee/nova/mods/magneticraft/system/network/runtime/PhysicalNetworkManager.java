package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.core.GraphMetrics;
import committee.nova.mods.magneticraft.system.network.core.IncrementalGraph;
import committee.nova.mods.magneticraft.system.network.core.WeightedPathfinder;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsRouteDecision;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/**
 * Per-dimension runtime topology. Node state remains owned by block entities.
 */
public final class PhysicalNetworkManager {
    private final ServerLevel level;
    private final Map<NetworkDomain, IncrementalGraph<Long, PhysicalNetworkNode>> graphs =
            new EnumMap<>(NetworkDomain.class);
    private long lastTick = Long.MIN_VALUE;
    private long edgeTicks;

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

        for (NetworkDomain domain : NetworkDomain.values()) {
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

        List<Direction> localOutputs = startNode.acceptingExternalOutputs(stack).stream()
                .filter(direction -> direction != incoming)
                .toList();
        if (!localOutputs.isEmpty()) {
            Direction output = localOutputs.get(Math.floorMod(roundRobinOffset, localOutputs.size()));
            return Optional.of(new LogisticsRouteDecision(output, true, 1, false));
        }

        long blockedFirst = incoming == null ? Long.MIN_VALUE : start.relative(incoming).asLong();
        WeightedPathfinder.SearchResult<Long> result = searchLogisticsRoute(
                graph,
                start,
                stack,
                blockedFirst,
                maxVisited
        );
        if (result.path().isEmpty() && incoming != null && !result.truncated()) {
            result = searchLogisticsRoute(graph, start, stack, Long.MIN_VALUE, maxVisited);
        }
        if (result.path().isEmpty() || result.path().get().size() < 2) {
            return Optional.empty();
        }
        BlockPos next = BlockPos.of(result.path().get().get(1));
        Direction direction = directionBetween(start, next);
        return direction == null
                ? Optional.empty()
                : Optional.of(new LogisticsRouteDecision(
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
                    for (long neighbor : graph.neighbors(key)) {
                        if (key == start.asLong() && neighbor == blockedFirst) {
                            continue;
                        }
                        LogisticsNetworkNode node = logisticsNode(graph, neighbor);
                        if (node != null) {
                            edges.add(new WeightedPathfinder.WeightedEdge<>(neighbor, 1 + node.routingWeight()));
                        }
                    }
                    return edges;
                },
                maxVisited
        );
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
}
