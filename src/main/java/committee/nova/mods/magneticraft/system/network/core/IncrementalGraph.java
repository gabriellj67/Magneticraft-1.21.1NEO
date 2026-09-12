package committee.nova.mods.magneticraft.system.network.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Loader-independent undirected graph with local component merge/split updates.
 *
 * <p>Values are authoritative outside this class. The graph only owns runtime
 * topology and can therefore be rebuilt after a chunk or level reload.</p>
 */
public final class IncrementalGraph<K, V> {
    private final Map<K, V> values = new LinkedHashMap<>();
    private final Map<K, LinkedHashSet<K>> adjacency = new LinkedHashMap<>();
    private final Map<K, Integer> componentByNode = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> nodesByComponent = new HashMap<>();

    private int nextComponentId = 1;
    private long topologyVersion;
    private long componentRebuilds;
    private long nodesVisitedDuringRebuilds;
    private int maxRebuildQueueDepth;

    public boolean addNode(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (values.putIfAbsent(key, value) != null) {
            values.put(key, value);
            return false;
        }

        adjacency.put(key, new LinkedHashSet<>());
        int component = nextComponentId++;
        componentByNode.put(key, component);
        nodesByComponent.put(component, linkedSetOf(key));
        topologyVersion++;
        return true;
    }

    public Optional<V> removeNode(K key) {
        V removed = values.remove(key);
        if (removed == null) {
            return Optional.empty();
        }

        Integer oldComponent = componentByNode.remove(key);
        LinkedHashSet<K> oldMembers = oldComponent == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(nodesByComponent.getOrDefault(oldComponent, new LinkedHashSet<>()));
        oldMembers.remove(key);
        nodesByComponent.remove(oldComponent);

        LinkedHashSet<K> neighbors = adjacency.remove(key);
        if (neighbors != null) {
            neighbors.forEach(neighbor -> adjacency.getOrDefault(neighbor, new LinkedHashSet<>()).remove(key));
        }

        oldMembers.forEach(componentByNode::remove);
        rebuildComponents(oldMembers);
        topologyVersion++;
        return Optional.of(removed);
    }

    public boolean connect(K first, K second) {
        requireNode(first);
        requireNode(second);
        if (Objects.equals(first, second) || !adjacency.get(first).add(second)) {
            return false;
        }
        adjacency.get(second).add(first);
        mergeComponents(first, second);
        topologyVersion++;
        return true;
    }

    public boolean disconnect(K first, K second) {
        LinkedHashSet<K> firstNeighbors = adjacency.get(first);
        LinkedHashSet<K> secondNeighbors = adjacency.get(second);
        if (firstNeighbors == null || secondNeighbors == null || !firstNeighbors.remove(second)) {
            return false;
        }
        secondNeighbors.remove(first);

        Integer component = componentByNode.get(first);
        if (component != null && Objects.equals(component, componentByNode.get(second))) {
            LinkedHashSet<K> members = new LinkedHashSet<>(nodesByComponent.getOrDefault(component, new LinkedHashSet<>()));
            LinkedHashSet<K> reachable = traverse(first, members);
            if (!reachable.contains(second)) {
                nodesByComponent.remove(component);
                members.forEach(componentByNode::remove);
                rebuildComponents(members);
            }
        }
        topologyVersion++;
        return true;
    }

    public Optional<V> value(K key) {
        return Optional.ofNullable(values.get(key));
    }

    public Collection<V> values() {
        return Collections.unmodifiableCollection(values.values());
    }

    public Set<K> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public Set<K> neighbors(K key) {
        LinkedHashSet<K> neighbors = adjacency.get(key);
        return neighbors == null ? Set.of() : Collections.unmodifiableSet(neighbors);
    }

    public List<Edge<K>> edges() {
        List<Edge<K>> result = new ArrayList<>();
        Set<UnorderedPair<K>> seen = new HashSet<>();
        adjacency.forEach((first, neighbors) -> neighbors.forEach(second -> {
            UnorderedPair<K> pair = new UnorderedPair<>(first, second);
            if (seen.add(pair)) {
                result.add(new Edge<>(first, second));
            }
        }));
        return List.copyOf(result);
    }

    public Set<K> componentOf(K key) {
        Integer component = componentByNode.get(key);
        if (component == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(nodesByComponent.getOrDefault(component, new LinkedHashSet<>()));
    }

    public int componentCount() {
        return nodesByComponent.size();
    }

    public int size() {
        return values.size();
    }

    public boolean contains(K key) {
        return values.containsKey(key);
    }

    public GraphMetrics metrics() {
        return new GraphMetrics(
                topologyVersion,
                componentRebuilds,
                nodesVisitedDuringRebuilds,
                maxRebuildQueueDepth
        );
    }

    private void mergeComponents(K first, K second) {
        int firstId = componentByNode.get(first);
        int secondId = componentByNode.get(second);
        if (firstId == secondId) {
            return;
        }

        LinkedHashSet<K> firstMembers = nodesByComponent.get(firstId);
        LinkedHashSet<K> secondMembers = nodesByComponent.get(secondId);
        if (firstMembers.size() < secondMembers.size()) {
            int temporaryId = firstId;
            firstId = secondId;
            secondId = temporaryId;
            LinkedHashSet<K> temporaryMembers = firstMembers;
            firstMembers = secondMembers;
            secondMembers = temporaryMembers;
        }

        int targetId = firstId;
        secondMembers.forEach(node -> componentByNode.put(node, targetId));
        firstMembers.addAll(secondMembers);
        nodesByComponent.remove(secondId);
    }

    private void rebuildComponents(Collection<K> candidates) {
        LinkedHashSet<K> remaining = new LinkedHashSet<>(candidates);
        while (!remaining.isEmpty()) {
            K seed = remaining.iterator().next();
            LinkedHashSet<K> component = traverse(seed, remaining);
            remaining.removeAll(component);
            int componentId = nextComponentId++;
            component.forEach(node -> componentByNode.put(node, componentId));
            nodesByComponent.put(componentId, component);
            componentRebuilds++;
        }
    }

    private LinkedHashSet<K> traverse(K seed, Collection<K> allowed) {
        Set<K> allowedSet = allowed instanceof Set<K> set ? set : new HashSet<>(allowed);
        LinkedHashSet<K> visited = new LinkedHashSet<>();
        ArrayDeque<K> queue = new ArrayDeque<>();
        queue.add(seed);

        while (!queue.isEmpty()) {
            maxRebuildQueueDepth = Math.max(maxRebuildQueueDepth, queue.size());
            K current = queue.removeFirst();
            if (!allowedSet.contains(current) || !visited.add(current)) {
                continue;
            }
            nodesVisitedDuringRebuilds++;
            for (K neighbor : adjacency.getOrDefault(current, new LinkedHashSet<>())) {
                if (allowedSet.contains(neighbor) && !visited.contains(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
        return visited;
    }

    private void requireNode(K key) {
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("Unknown graph node: " + key);
        }
    }

    private static <K> LinkedHashSet<K> linkedSetOf(K key) {
        LinkedHashSet<K> set = new LinkedHashSet<>();
        set.add(key);
        return set;
    }

    public record Edge<K>(K first, K second) {
    }

    private static final class UnorderedPair<K> {
        private final K first;
        private final K second;

        private UnorderedPair(K first, K second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof UnorderedPair<?> pair)) {
                return false;
            }
            return (Objects.equals(first, pair.first) && Objects.equals(second, pair.second))
                    || (Objects.equals(first, pair.second) && Objects.equals(second, pair.first));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(first) ^ Objects.hashCode(second);
        }
    }
}
