package committee.nova.mods.magneticraft.system.network.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Bounded Dijkstra search used by item logistics without an unbounded work queue.
 */
public final class WeightedPathfinder {
    private WeightedPathfinder() {
    }

    public static <K> SearchResult<K> find(
            K start,
            Predicate<K> destination,
            Function<K, Iterable<WeightedEdge<K>>> edges,
            int maxVisited
    ) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(edges, "edges");
        if (maxVisited <= 0) {
            throw new IllegalArgumentException("maxVisited must be positive");
        }

        PriorityQueue<QueueEntry<K>> queue = new PriorityQueue<>(Comparator
                .comparingInt(QueueEntry<K>::cost)
                .thenComparingLong(QueueEntry<K>::sequence));
        Map<K, Integer> distances = new HashMap<>();
        Map<K, K> previous = new HashMap<>();
        long sequence = 0;
        int maxQueueDepth = 1;
        int visited = 0;

        distances.put(start, 0);
        queue.add(new QueueEntry<>(start, 0, sequence++));

        while (!queue.isEmpty() && visited < maxVisited) {
            maxQueueDepth = Math.max(maxQueueDepth, queue.size());
            QueueEntry<K> current = queue.poll();
            if (current.cost() != distances.getOrDefault(current.node(), Integer.MAX_VALUE)) {
                continue;
            }
            visited++;
            if (!Objects.equals(start, current.node()) && destination.test(current.node())) {
                return new SearchResult<>(Optional.of(path(start, current.node(), previous)), visited, maxQueueDepth, false);
            }

            for (WeightedEdge<K> edge : edges.apply(current.node())) {
                if (edge.weight() < 0) {
                    throw new IllegalArgumentException("Negative route weight");
                }
                int nextCost = saturatedAdd(current.cost(), edge.weight());
                if (nextCost < distances.getOrDefault(edge.target(), Integer.MAX_VALUE)) {
                    distances.put(edge.target(), nextCost);
                    previous.put(edge.target(), current.node());
                    queue.add(new QueueEntry<>(edge.target(), nextCost, sequence++));
                }
            }
        }
        return new SearchResult<>(Optional.empty(), visited, maxQueueDepth, !queue.isEmpty());
    }

    private static <K> List<K> path(K start, K end, Map<K, K> previous) {
        ArrayList<K> reversed = new ArrayList<>();
        K current = end;
        reversed.add(current);
        while (!Objects.equals(current, start)) {
            current = previous.get(current);
            if (current == null) {
                return List.of();
            }
            reversed.add(current);
        }

        ArrayList<K> result = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            result.add(reversed.get(index));
        }
        return List.copyOf(result);
    }

    private static int saturatedAdd(int first, int second) {
        long result = (long) first + second;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    public record WeightedEdge<K>(K target, int weight) {
        public WeightedEdge {
            Objects.requireNonNull(target, "target");
        }
    }

    public record SearchResult<K>(
            Optional<List<K>> path,
            int visitedNodes,
            int maxQueueDepth,
            boolean truncated
    ) {
    }

    private record QueueEntry<K>(K node, int cost, long sequence) {
    }
}
