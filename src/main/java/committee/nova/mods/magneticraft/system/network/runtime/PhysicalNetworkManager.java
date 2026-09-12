package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.core.GraphMetrics;
import committee.nova.mods.magneticraft.system.network.core.IncrementalGraph;
import committee.nova.mods.magneticraft.system.network.core.WeightedPathfinder;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultLocation;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultSearchResult;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.NetworkSummary;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalCoupler;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeAccess;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsRouteDecision;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Per-dimension incremental topology. Authoritative state remains block-entity owned. */
public final class PhysicalNetworkManager {
    public static final int MAX_LOGISTICS_ROUTE_VISITS = 4_096;
    public static final int MAX_ELECTRICAL_DIAGNOSTIC_VISITS = 4_096;
    private static final int MAX_LOGISTICS_ROUTE_CACHE_ENTRIES = 4_096;

    private final ServerLevel level;
    private final Map<NetworkDomain, IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode>> graphs =
            new EnumMap<>(NetworkDomain.class);
    private final Map<NetworkDomain, Map<Long, LinkedHashSet<PhysicalNodeKey>>> positionIndex =
            new EnumMap<>(NetworkDomain.class);
    private final Set<NodePair> internalConnections = new LinkedHashSet<>();
    private final Map<NodePair, ElectricalCoupler> electricalCouplers = new LinkedHashMap<>();
    private final Map<LogisticsCacheKey, CachedLogisticsRoute> logisticsRouteCache =
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<LogisticsCacheKey, CachedLogisticsRoute> eldest) {
                    return size() > MAX_LOGISTICS_ROUTE_CACHE_ENTRIES;
                }
            };

    private long lastTick = Long.MIN_VALUE;
    private long edgeTicks;
    private int electricalPauseTicks;
    private int electricalDamageGraceTicks;
    private long logisticsCacheTopologyVersion = Long.MIN_VALUE;
    private boolean electricalTickActive;
    private List<ElectricalEdgeTelemetry> activeElectricalTelemetry = List.of();
    private List<ElectricalEdgeTelemetry> completedElectricalTelemetry = List.of();
    private Map<ElectricalNodeAccess, Double> completedElectricalGeneration = Map.of();
    private Map<ElectricalNodeAccess, Double> completedElectricalConsumption = Map.of();

    PhysicalNetworkManager(ServerLevel level) {
        this.level = level;
        for (NetworkDomain domain : NetworkDomain.values()) {
            graphs.put(domain, new IncrementalGraph<>());
            positionIndex.put(domain, new LinkedHashMap<>());
        }
    }

    public ServerLevel level() {
        return level;
    }

    public void register(PhysicalNetworkNode node) {
        Objects.requireNonNull(node, "node");
        PhysicalNodeKey key = checkedKey(node);
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(node.domain());
        boolean added = graph.addNode(key, node);
        if (added) {
            index(node.domain(), key);
        }
        refreshNeighborhood(node.domain(), key.position());
        if (node.domain() == NetworkDomain.ELECTRICITY) {
            refreshInternalConnectionsAt(key.position());
        }
    }

    public void unregister(PhysicalNetworkNode node) {
        Objects.requireNonNull(node, "node");
        PhysicalNodeKey key = checkedKey(node);
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(node.domain());
        if (graph.value(key).orElse(null) != node) {
            return;
        }
        graph.removeNode(key);
        unindex(node.domain(), key);
        refreshNeighborhood(node.domain(), key.position());
    }

    public void update(PhysicalNetworkNode node) {
        Objects.requireNonNull(node, "node");
        PhysicalNodeKey key = checkedKey(node);
        if (!graph(node.domain()).contains(key)) {
            register(node);
            return;
        }
        graph(node.domain()).addNode(key, node);
        refreshNeighborhood(node.domain(), key.position());
        if (node.domain() == NetworkDomain.ELECTRICITY) {
            refreshInternalConnectionsAt(key.position());
        }
    }

    /** Opens or closes an explicit same-block electrical edge without changing terminal identity. */
    public void setInternalConnection(PhysicalNodeKey first, PhysicalNodeKey second, boolean closed) {
        requireSamePosition(first, second);
        NodePair pair = NodePair.of(first, second);
        if (closed) {
            internalConnections.add(pair);
            refreshInternalConnection(pair);
        } else {
            internalConnections.remove(pair);
            graph(NetworkDomain.ELECTRICITY).disconnect(pair.first(), pair.second());
        }
    }

    public boolean internalConnectionClosed(PhysicalNodeKey first, PhysicalNodeKey second) {
        return internalConnections.contains(NodePair.of(first, second));
    }

    /** Registers an isolated cross-tier exchange that deliberately does not become a graph edge. */
    public void registerElectricalCoupler(ElectricalCoupler coupler) {
        Objects.requireNonNull(coupler, "coupler");
        if (coupler.firstTerminal().equals(coupler.secondTerminal())) {
            throw new IllegalArgumentException("Electrical coupler terminals must be distinct");
        }
        NodePair pair = NodePair.of(coupler.firstTerminal(), coupler.secondTerminal());
        electricalCouplers.put(pair, coupler);
    }

    public void unregisterElectricalCoupler(ElectricalCoupler coupler) {
        Objects.requireNonNull(coupler, "coupler");
        electricalCouplers.remove(NodePair.of(coupler.firstTerminal(), coupler.secondTerminal()), coupler);
    }

    public int electricalCouplerCount() {
        return electricalCouplers.size();
    }

    public void tick(long gameTime) {
        if (lastTick == gameTime) {
            return;
        }
        lastTick = gameTime;
        completedElectricalTelemetry = List.of();
        completedElectricalGeneration = Map.of();
        completedElectricalConsumption = Map.of();

        for (NetworkDomain domain : NetworkDomain.values()) {
            if (domain == NetworkDomain.ELECTRICITY) {
                if (electricalPauseTicks == 0) {
                    tickElectricity();
                }
            } else {
                tickOrdinaryDomain(domain);
            }
        }
        if (electricalPauseTicks > 0) {
            electricalPauseTicks--;
        }
        if (electricalDamageGraceTicks > 0) {
            electricalDamageGraceTicks--;
        }
    }

    private void tickOrdinaryDomain(NetworkDomain domain) {
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(domain);
        List<Map.Entry<PhysicalNodeKey, PhysicalNetworkNode>> nodes = orderedEntries(graph);
        nodes.forEach(entry -> entry.getValue().beforeNetworkTick(this));
        orderedEdges(graph).forEach(pair -> {
            PhysicalNetworkNode first = graph.value(pair.first()).orElse(null);
            PhysicalNetworkNode second = graph.value(pair.second()).orElse(null);
            if (first != null && second != null) {
                first.exchangeWith(second);
                edgeTicks++;
            }
        });
        nodes.forEach(entry -> entry.getValue().afterNetworkTick(this));
    }

    private void tickElectricity() {
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(NetworkDomain.ELECTRICITY);
        List<Map.Entry<PhysicalNodeKey, PhysicalNetworkNode>> entries = orderedEntries(graph);
        List<ElectricalNodeAccess> accesses = distinctElectricalAccesses(entries);
        List<ElectricalTickParticipant> participants = distinctElectricalParticipants(entries);
        activeElectricalTelemetry = new ArrayList<>();
        electricalTickActive = true;
        try {
            entries.forEach(entry -> entry.getValue().beforeNetworkTick(this));
            accesses.forEach(access -> access.electricalNode().beginNetworkTick());
            IdentityHashMap<ElectricalNodeAccess, Double> beforeInjection = electricalEnergySnapshot(accesses);
            participants.forEach(participant -> participant.injectElectricalEnergy(this));
            IdentityHashMap<ElectricalNodeAccess, Double> generation = positiveEnergyDelta(
                    beforeInjection,
                    accesses,
                    true
            );
            executeElectricalCouplers(graph);
            orderedEdges(graph).forEach(pair -> transferElectrical(
                    pair.first(),
                    pair.second(),
                    1.0D,
                    internalConnections.contains(pair)
                            ? ElectricalEdgeTelemetry.EdgeType.INTERNAL
                            : ElectricalEdgeTelemetry.EdgeType.ADJACENT
            ));
            if (level != null) {
                LongDistanceElectricityService.tickIfPresent(level, level.getGameTime());
            }
            IdentityHashMap<ElectricalNodeAccess, Double> beforeExtraction = electricalEnergySnapshot(accesses);
            participants.forEach(participant -> participant.extractElectricalEnergy(this));
            IdentityHashMap<ElectricalNodeAccess, Double> consumption = positiveEnergyDelta(
                    beforeExtraction,
                    accesses,
                    false
            );
            accesses.forEach(access -> access.electricalNode().completeNetworkTick());
            entries.forEach(entry -> entry.getValue().afterNetworkTick(this));
            participants.forEach(participant -> participant.commitElectricalState(this));
            completedElectricalTelemetry = List.copyOf(activeElectricalTelemetry);
            completedElectricalGeneration = immutableIdentityMap(generation);
            completedElectricalConsumption = immutableIdentityMap(consumption);
        } finally {
            electricalTickActive = false;
            activeElectricalTelemetry = List.of();
        }
    }

    /** Executes a loaded ordinary electrical edge and records it in the active tick audit. */
    public ElectricalLink.Transfer transferElectrical(
            PhysicalNodeKey firstKey,
            PhysicalNodeKey secondKey,
            double distance,
            ElectricalEdgeTelemetry.EdgeType edgeType
    ) {
        if (edgeType == ElectricalEdgeTelemetry.EdgeType.COUPLER) {
            throw new IllegalArgumentException("Couplers must use registerElectricalCoupler");
        }
        PhysicalNetworkNode firstNode = graph(NetworkDomain.ELECTRICITY).value(firstKey).orElse(null);
        PhysicalNetworkNode secondNode = graph(NetworkDomain.ELECTRICITY).value(secondKey).orElse(null);
        ElectricalNodeAccess first = electricalAccess(firstNode);
        ElectricalNodeAccess second = electricalAccess(secondNode);
        if (!ordinaryElectricalCompatible(first, second)) {
            return ElectricalLink.Transfer.ZERO;
        }

        ElectricalLink.Transfer transfer = ElectricalLink.transfer(
                first.electricalNode(),
                second.electricalNode(),
                distance
        );
        edgeTicks++;
        if (transfer.moved()) {
            first.markElectricalStateChanged();
            if (second != first) {
                second.markElectricalStateChanged();
            }
        }
        if (electricalTickActive) {
            activeElectricalTelemetry.add(new ElectricalEdgeTelemetry(
                    edgeType,
                    firstKey,
                    secondKey,
                    transfer.chargeCoulombsPerTick(),
                    transfer.currentAmps(),
                    transfer.deliveredJoules(),
                    transfer.lostJoules(),
                    transfer.firstWasSource()
            ));
        }
        return transfer;
    }

    private void executeElectricalCouplers(IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph) {
        electricalCouplers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ElectricalCoupler coupler = entry.getValue();
                    ElectricalNodeAccess first = electricalAccess(graph.value(coupler.firstTerminal()).orElse(null));
                    ElectricalNodeAccess second = electricalAccess(graph.value(coupler.secondTerminal()).orElse(null));
                    if (first == null || second == null
                            || !first.electricalProfileBound() || !second.electricalProfileBound()) {
                        return;
                    }
                    ElectricalCoupler.CouplingResult result = coupler.transfer(
                            first.electricalNode(), second.electricalNode(), false
                    );
                    edgeTicks++;
                    if (result.moved()) {
                        first.markElectricalStateChanged();
                        if (second != first) {
                            second.markElectricalStateChanged();
                        }
                    }
                    activeElectricalTelemetry.add(new ElectricalEdgeTelemetry(
                            ElectricalEdgeTelemetry.EdgeType.COUPLER,
                            coupler.firstTerminal(),
                            coupler.secondTerminal(),
                            Math.max(result.sourceChargeCoulombs(), result.destinationChargeCoulombs()),
                            Math.max(result.sourceChargeCoulombs(), result.destinationChargeCoulombs())
                                    * ElectricalNode.TICKS_PER_SECOND,
                            result.deliveredJoules(),
                            result.lostJoules(),
                            result.firstWasSource()
                    ));
                });
    }

    public void onElectricalProfilesReloaded(ElectricalDataSnapshot snapshot, int graceTicks) {
        Set<Object> rebound = Collections.newSetFromMap(new IdentityHashMap<>());
        graph(NetworkDomain.ELECTRICITY).values().forEach(node -> {
            if (node instanceof ElectricalProfileBinding binding && rebound.add(binding)) {
                binding.rebindElectricalProfile(snapshot);
            }
        });
        refreshAllElectricity();
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

    public List<ElectricalEdgeTelemetry> electricalEdgeTelemetry() {
        return completedElectricalTelemetry;
    }

    /** Bounded, loaded-node-only summary of one same-tier electrical component. */
    public Optional<NetworkSummary> electricalNetworkSummary(PhysicalNodeKey start, int maxVisitedNodes) {
        Objects.requireNonNull(start, "start");
        Optional<BoundedElectricalComponent> bounded = boundedElectricalComponent(start, maxVisitedNodes);
        if (bounded.isEmpty()) {
            return Optional.empty();
        }
        BoundedElectricalComponent component = bounded.orElseThrow();
        Set<PhysicalNodeKey> keys = component.keys();
        Set<ElectricalNodeAccess> counted = Collections.newSetFromMap(new IdentityHashMap<>());
        double storedJoules = 0.0D;
        double generation = 0.0D;
        double consumption = 0.0D;
        double maximumLoad = 0.0D;
        int faults = 0;
        for (PhysicalNodeKey key : keys) {
            ElectricalNodeAccess access = electricalAccess(graph(NetworkDomain.ELECTRICITY).value(key).orElse(null));
            if (access == null || !counted.add(access)) {
                continue;
            }
            storedJoules += access.electricalNode().energyJoules();
            generation += completedElectricalGeneration.getOrDefault(access, 0.0D);
            consumption += completedElectricalConsumption.getOrDefault(access, 0.0D);
            double rating = access.electricalRatedCurrentAmps();
            if (Double.isFinite(rating) && rating > 0.0D) {
                maximumLoad = Math.max(maximumLoad, maximumTerminalCurrentAmps(key) / rating);
            }
            if (access.electricalFaultKind() != FaultKind.NONE) {
                faults++;
            }
        }

        LinkedHashSet<NodePair> edges = new LinkedHashSet<>();
        for (PhysicalNodeKey key : keys) {
            for (PhysicalNodeKey neighbor : diagnosticElectricalNeighbors(key)) {
                if (keys.contains(neighbor)) {
                    edges.add(NodePair.of(key, neighbor));
                }
            }
        }
        double losses = completedElectricalTelemetry.stream()
                .filter(edge -> keys.contains(sourceTerminal(edge)))
                .mapToDouble(ElectricalEdgeTelemetry::lostJoulesPerTick)
                .sum();
        return Optional.of(new NetworkSummary(
                keys.size(),
                edges.size(),
                storedJoules,
                generation,
                consumption,
                losses,
                maximumLoad,
                faults,
                keys.size(),
                component.truncated()
        ));
    }

    /** Breadth-first nearest fault search that never leaves the loaded topology. */
    public Optional<FaultSearchResult> nearestElectricalFault(PhysicalNodeKey start, int maxVisitedNodes) {
        Objects.requireNonNull(start, "start");
        Optional<BoundedElectricalComponent> bounded = boundedElectricalComponent(start, maxVisitedNodes);
        if (bounded.isEmpty()) {
            return Optional.empty();
        }
        BoundedElectricalComponent component = bounded.orElseThrow();
        FaultLocation location = null;
        Set<ElectricalNodeAccess> checked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PhysicalNodeKey key : component.keys()) {
            ElectricalNodeAccess access = electricalAccess(graph(NetworkDomain.ELECTRICITY).value(key).orElse(null));
            if (access == null || !checked.add(access)) {
                continue;
            }
            FaultKind fault = access.electricalFaultKind();
            if (fault == FaultKind.NONE) {
                continue;
            }
            BlockPos offset = key.position().subtract(start.position());
            Optional<Direction> direction = offset.equals(BlockPos.ZERO)
                    ? Optional.empty()
                    : Optional.of(Direction.getNearest(offset.getX(), offset.getY(), offset.getZ()));
            location = new FaultLocation(
                    fault,
                    key.terminalId(),
                    key.position(),
                    direction,
                    Math.sqrt(start.position().distSqr(key.position()))
            );
            break;
        }
        return Optional.of(new FaultSearchResult(
                Optional.ofNullable(location),
                component.keys().size(),
                component.truncated()
        ));
    }

    /** Maximum absolute current touching one terminal in the active (or most recently completed) tick. */
    public double maximumTerminalCurrentAmps(PhysicalNodeKey terminal) {
        Objects.requireNonNull(terminal, "terminal");
        List<ElectricalEdgeTelemetry> telemetry = electricalTickActive
                ? activeElectricalTelemetry
                : completedElectricalTelemetry;
        return telemetry.stream()
                .filter(edge -> edge.firstTerminal().equals(terminal) || edge.secondTerminal().equals(terminal))
                .mapToDouble(edge -> Math.abs(edge.currentAmps()))
                .max()
                .orElse(0.0D);
    }

    /** Exact current through one physical edge in the active (or most recently completed) tick. */
    public double electricalEdgeCurrentAmps(
            PhysicalNodeKey first,
            PhysicalNodeKey second,
            ElectricalEdgeTelemetry.EdgeType edgeType
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(edgeType, "edgeType");
        List<ElectricalEdgeTelemetry> telemetry = electricalTickActive
                ? activeElectricalTelemetry
                : completedElectricalTelemetry;
        return telemetry.stream()
                .filter(edge -> edge.edgeType() == edgeType)
                .filter(edge -> (edge.firstTerminal().equals(first) && edge.secondTerminal().equals(second))
                        || (edge.firstTerminal().equals(second) && edge.secondTerminal().equals(first)))
                .mapToDouble(ElectricalEdgeTelemetry::currentAmps)
                .max()
                .orElse(0.0D);
    }

    /** Position-only compatibility query; prefers the conventional main terminal. */
    public Optional<PhysicalNetworkNode> node(NetworkDomain domain, BlockPos position) {
        Optional<PhysicalNetworkNode> main = node(domain, PhysicalNodeKey.main(position));
        if (main.isPresent()) {
            return main;
        }
        return keysAt(domain, position).stream().findFirst().flatMap(key -> node(domain, key));
    }

    public Optional<PhysicalNetworkNode> node(NetworkDomain domain, PhysicalNodeKey key) {
        return graph(domain).value(key);
    }

    public List<PhysicalNetworkNode> nodesAt(NetworkDomain domain, BlockPos position) {
        return keysAt(domain, position).stream()
                .map(key -> graph(domain).value(key).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public Set<PhysicalNodeKey> keysAt(NetworkDomain domain, BlockPos position) {
        LinkedHashSet<PhysicalNodeKey> keys = positionIndex.get(domain).get(position.asLong());
        if (keys == null) {
            return Set.of();
        }
        LinkedHashSet<PhysicalNodeKey> ordered = new LinkedHashSet<>();
        keys.stream().sorted().forEach(ordered::add);
        return Collections.unmodifiableSet(ordered);
    }

    /** Position-only compatibility view; multiple terminals at one neighbor collapse to one position. */
    public Set<Long> neighbors(NetworkDomain domain, BlockPos position) {
        LinkedHashSet<Long> positions = new LinkedHashSet<>();
        for (PhysicalNodeKey key : keysAt(domain, position)) {
            graph(domain).neighbors(key).stream()
                    .map(neighbor -> neighbor.position().asLong())
                    .forEach(positions::add);
        }
        return Collections.unmodifiableSet(positions);
    }

    public Set<PhysicalNodeKey> neighborKeys(NetworkDomain domain, PhysicalNodeKey key) {
        return graph(domain).neighbors(key);
    }

    /** Position-only compatibility view of every terminal position in the selected component. */
    public Set<Long> component(NetworkDomain domain, BlockPos position) {
        PhysicalNodeKey key = node(domain, PhysicalNodeKey.main(position)).isPresent()
                ? PhysicalNodeKey.main(position)
                : keysAt(domain, position).stream().findFirst().orElse(null);
        if (key == null) {
            return Set.of();
        }
        LinkedHashSet<Long> positions = new LinkedHashSet<>();
        graph(domain).componentOf(key).stream()
                .map(componentKey -> componentKey.position().asLong())
                .forEach(positions::add);
        return Collections.unmodifiableSet(positions);
    }

    public Set<PhysicalNodeKey> component(NetworkDomain domain, PhysicalNodeKey key) {
        return graph(domain).componentOf(key);
    }

    public int nodeCount(NetworkDomain domain) {
        return graph(domain).size();
    }

    public int componentCount(NetworkDomain domain) {
        return graph(domain).componentCount();
    }

    private Optional<BoundedElectricalComponent> boundedElectricalComponent(
            PhysicalNodeKey start,
            int maxVisitedNodes
    ) {
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(NetworkDomain.ELECTRICITY);
        if (!graph.contains(start)) {
            return Optional.empty();
        }
        int limit = Math.max(1, Math.min(maxVisitedNodes, MAX_ELECTRICAL_DIAGNOSTIC_VISITS));
        ArrayDeque<PhysicalNodeKey> pending = new ArrayDeque<>();
        LinkedHashSet<PhysicalNodeKey> visited = new LinkedHashSet<>();
        pending.add(start);
        while (!pending.isEmpty() && visited.size() < limit) {
            PhysicalNodeKey current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            diagnosticElectricalNeighbors(current).stream()
                    .filter(neighbor -> !visited.contains(neighbor))
                    .forEach(pending::addLast);
        }
        boolean truncated = !pending.isEmpty();
        return Optional.of(new BoundedElectricalComponent(
                Collections.unmodifiableSet(visited),
                truncated
        ));
    }

    private Set<PhysicalNodeKey> diagnosticElectricalNeighbors(PhysicalNodeKey key) {
        LinkedHashSet<PhysicalNodeKey> neighbors = new LinkedHashSet<>(
                graph(NetworkDomain.ELECTRICITY).neighbors(key)
        );
        completedElectricalTelemetry.stream()
                .filter(edge -> edge.edgeType() == ElectricalEdgeTelemetry.EdgeType.LONG_DISTANCE)
                .forEach(edge -> {
                    if (edge.firstTerminal().equals(key)) {
                        neighbors.add(edge.secondTerminal());
                    } else if (edge.secondTerminal().equals(key)) {
                        neighbors.add(edge.firstTerminal());
                    }
                });
        LinkedHashSet<PhysicalNodeKey> ordered = new LinkedHashSet<>();
        neighbors.stream()
                .filter(neighbor -> graph(NetworkDomain.ELECTRICITY).contains(neighbor))
                .sorted(PhysicalNodeKey.ORDER)
                .forEach(ordered::add);
        return ordered;
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
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(NetworkDomain.LOGISTICS);
        PhysicalNodeKey startKey = PhysicalNodeKey.main(start);
        LogisticsNetworkNode startNode = logisticsNode(graph, startKey);
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

        PhysicalNodeKey blockedFirst = incoming == null ? null : PhysicalNodeKey.main(start.relative(incoming));
        List<PhysicalNodeKey> firstNeighbors = orderedFirstNeighbors(graph, startKey, blockedFirst);
        PhysicalNodeKey preferredFirst = firstNeighbors.isEmpty()
                ? null
                : firstNeighbors.get(Math.floorMod(roundRobinOffset, firstNeighbors.size()));
        LogisticsCacheKey cacheKey = new LogisticsCacheKey(startKey, incoming, preferredFirst, visitLimit);
        CachedLogisticsRoute cached = logisticsRouteCache.get(cacheKey);
        if (cached != null && cached.matches(graph, stack, topologyVersion)) {
            return Optional.of(new LogisticsRouteDecision(cached.direction(), false, 0, false));
        }

        WeightedPathfinder.SearchResult<PhysicalNodeKey> result = searchLogisticsRoute(
                graph, startKey, stack, blockedFirst, preferredFirst, visitLimit
        );
        if (result.path().isEmpty() && incoming != null && !result.truncated()) {
            firstNeighbors = orderedFirstNeighbors(graph, startKey, null);
            preferredFirst = firstNeighbors.isEmpty()
                    ? null
                    : firstNeighbors.get(Math.floorMod(roundRobinOffset, firstNeighbors.size()));
            cacheKey = new LogisticsCacheKey(startKey, incoming, preferredFirst, visitLimit);
            cached = logisticsRouteCache.get(cacheKey);
            if (cached != null && cached.matches(graph, stack, topologyVersion)) {
                return Optional.of(new LogisticsRouteDecision(cached.direction(), false, 0, false));
            }
            result = searchLogisticsRoute(graph, startKey, stack, null, preferredFirst, visitLimit);
        }
        if (result.path().isEmpty() || result.path().orElseThrow().size() < 2) {
            return Optional.empty();
        }
        List<PhysicalNodeKey> path = result.path().orElseThrow();
        PhysicalNodeKey next = path.get(1);
        Direction direction = directionBetween(start, next.position());
        if (direction == null) {
            return Optional.empty();
        }
        PhysicalNodeKey destination = path.get(path.size() - 1);
        logisticsRouteCache.put(
                cacheKey,
                new CachedLogisticsRoute(stack.copy(), direction, startKey, next, destination, topologyVersion)
        );
        return Optional.of(new LogisticsRouteDecision(direction, false, result.visitedNodes(), result.truncated()));
    }

    private WeightedPathfinder.SearchResult<PhysicalNodeKey> searchLogisticsRoute(
            IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph,
            PhysicalNodeKey start,
            ItemStack stack,
            PhysicalNodeKey blockedFirst,
            PhysicalNodeKey preferredFirst,
            int maxVisited
    ) {
        return WeightedPathfinder.find(
                start,
                key -> {
                    LogisticsNetworkNode node = logisticsNode(graph, key);
                    return node != null && !node.acceptingExternalOutputs(stack).isEmpty();
                },
                key -> {
                    ArrayList<WeightedPathfinder.WeightedEdge<PhysicalNodeKey>> edges = new ArrayList<>();
                    for (PhysicalNodeKey neighbor : orderedNeighbors(graph, key)) {
                        if (key.equals(start) && Objects.equals(neighbor, blockedFirst)) {
                            continue;
                        }
                        LogisticsNetworkNode node = logisticsNode(graph, neighbor);
                        if (node != null) {
                            int baseCost = key.equals(start)
                                    ? (Objects.equals(neighbor, preferredFirst) ? 0 : 1)
                                    : 2;
                            edges.add(new WeightedPathfinder.WeightedEdge<>(neighbor, baseCost + node.routingWeight()));
                        }
                    }
                    return edges;
                },
                maxVisited
        );
    }

    private static List<PhysicalNodeKey> orderedFirstNeighbors(
            IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph,
            PhysicalNodeKey start,
            PhysicalNodeKey blockedFirst
    ) {
        return orderedNeighbors(graph, start).stream()
                .filter(neighbor -> !Objects.equals(neighbor, blockedFirst))
                .toList();
    }

    private static List<PhysicalNodeKey> orderedNeighbors(
            IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph,
            PhysicalNodeKey key
    ) {
        return graph.neighbors(key).stream()
                .sorted(Comparator
                        .comparingInt((PhysicalNodeKey neighbor) -> {
                            Direction direction = directionBetween(key.position(), neighbor.position());
                            return direction == null ? Integer.MAX_VALUE : direction.ordinal();
                        })
                        .thenComparing(PhysicalNodeKey.ORDER))
                .toList();
    }

    private void refreshAllElectricity() {
        List<BlockPos> positions = positionIndex.get(NetworkDomain.ELECTRICITY).keySet().stream()
                .map(BlockPos::of)
                .toList();
        positions.forEach(position -> refreshNeighborhood(NetworkDomain.ELECTRICITY, position));
        internalConnections.forEach(this::refreshInternalConnection);
    }

    private void refreshNeighborhood(NetworkDomain domain, BlockPos position) {
        for (Direction direction : Direction.values()) {
            refreshPositionPair(domain, position, position.relative(direction), direction);
        }
    }

    private void refreshPositionPair(
            NetworkDomain domain,
            BlockPos firstPosition,
            BlockPos secondPosition,
            Direction direction
    ) {
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(domain);
        List<PhysicalNodeKey> firstKeys = new ArrayList<>(keysAt(domain, firstPosition));
        List<PhysicalNodeKey> secondKeys = new ArrayList<>(keysAt(domain, secondPosition));
        for (PhysicalNodeKey firstKey : firstKeys) {
            PhysicalNetworkNode first = graph.value(firstKey).orElse(null);
            if (first == null) {
                continue;
            }
            for (PhysicalNodeKey secondKey : secondKeys) {
                PhysicalNetworkNode second = graph.value(secondKey).orElse(null);
                boolean connected = second != null
                        && first.canConnect(direction, second)
                        && second.canConnect(direction.getOpposite(), first)
                        && (domain != NetworkDomain.ELECTRICITY
                        || ordinaryElectricalCompatible(electricalAccess(first), electricalAccess(second)));
                if (connected) {
                    graph.connect(firstKey, secondKey);
                } else {
                    graph.disconnect(firstKey, secondKey);
                }
            }
        }
    }

    private void refreshInternalConnectionsAt(BlockPos position) {
        internalConnections.stream()
                .filter(pair -> pair.first().position().equals(position))
                .forEach(this::refreshInternalConnection);
    }

    private void refreshInternalConnection(NodePair pair) {
        IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph = graph(NetworkDomain.ELECTRICITY);
        PhysicalNetworkNode first = graph.value(pair.first()).orElse(null);
        PhysicalNetworkNode second = graph.value(pair.second()).orElse(null);
        boolean connected = first != null && second != null
                && first.position().equals(second.position())
                && ordinaryElectricalCompatible(electricalAccess(first), electricalAccess(second));
        if (connected) {
            graph.connect(pair.first(), pair.second());
        } else {
            graph.disconnect(pair.first(), pair.second());
        }
    }

    private void index(NetworkDomain domain, PhysicalNodeKey key) {
        positionIndex.get(domain)
                .computeIfAbsent(key.position().asLong(), ignored -> new LinkedHashSet<>())
                .add(key);
    }

    private void unindex(NetworkDomain domain, PhysicalNodeKey key) {
        Map<Long, LinkedHashSet<PhysicalNodeKey>> byPosition = positionIndex.get(domain);
        LinkedHashSet<PhysicalNodeKey> keys = byPosition.get(key.position().asLong());
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            byPosition.remove(key.position().asLong());
        }
    }

    private IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph(NetworkDomain domain) {
        return graphs.get(domain);
    }

    private static PhysicalNodeKey checkedKey(PhysicalNetworkNode node) {
        PhysicalNodeKey key = Objects.requireNonNull(node.nodeKey(), "nodeKey");
        if (!key.position().equals(node.position())) {
            throw new IllegalArgumentException("Physical node key position must match node position");
        }
        return key;
    }

    private static void requireSamePosition(PhysicalNodeKey first, PhysicalNodeKey second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second) || !first.position().equals(second.position())) {
            throw new IllegalArgumentException("Internal electrical terminals must be distinct and share a position");
        }
    }

    private static ElectricalNodeAccess electricalAccess(PhysicalNetworkNode node) {
        if (node == null) {
            return null;
        }
        PhysicalNetworkNode transferNode = node.transferNode();
        return transferNode instanceof ElectricalNodeAccess access ? access : null;
    }

    private static boolean ordinaryElectricalCompatible(ElectricalNodeAccess first, ElectricalNodeAccess second) {
        return first != null && second != null
                && first.electricalProfileBound()
                && second.electricalProfileBound()
                && first.electricalTierId().equals(second.electricalTierId());
    }

    private static List<ElectricalNodeAccess> distinctElectricalAccesses(
            Collection<Map.Entry<PhysicalNodeKey, PhysicalNetworkNode>> entries
    ) {
        Set<ElectricalNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ElectricalNodeAccess> result = new ArrayList<>();
        entries.forEach(entry -> {
            ElectricalNodeAccess access = entry.getValue() instanceof ElectricalNodeAccess direct ? direct : null;
            if (access != null && seen.add(access.electricalNode())) {
                result.add(access);
            }
        });
        return result;
    }

    private static List<ElectricalTickParticipant> distinctElectricalParticipants(
            Collection<Map.Entry<PhysicalNodeKey, PhysicalNetworkNode>> entries
    ) {
        Set<ElectricalTickParticipant> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ElectricalTickParticipant> result = new ArrayList<>();
        entries.forEach(entry -> {
            if (entry.getValue() instanceof ElectricalTickParticipant participant && seen.add(participant)) {
                result.add(participant);
            }
        });
        return result;
    }

    private static IdentityHashMap<ElectricalNodeAccess, Double> electricalEnergySnapshot(
            Collection<ElectricalNodeAccess> accesses
    ) {
        IdentityHashMap<ElectricalNodeAccess, Double> snapshot = new IdentityHashMap<>();
        accesses.forEach(access -> snapshot.put(access, access.electricalNode().energyJoules()));
        return snapshot;
    }

    private static IdentityHashMap<ElectricalNodeAccess, Double> positiveEnergyDelta(
            Map<ElectricalNodeAccess, Double> before,
            Collection<ElectricalNodeAccess> accesses,
            boolean increase
    ) {
        IdentityHashMap<ElectricalNodeAccess, Double> result = new IdentityHashMap<>();
        for (ElectricalNodeAccess access : accesses) {
            double previous = before.getOrDefault(access, access.electricalNode().energyJoules());
            double current = access.electricalNode().energyJoules();
            double delta = increase ? current - previous : previous - current;
            if (Double.isFinite(delta) && delta > 0.0D) {
                result.put(access, delta);
            }
        }
        return result;
    }

    private static Map<ElectricalNodeAccess, Double> immutableIdentityMap(
            Map<ElectricalNodeAccess, Double> source
    ) {
        IdentityHashMap<ElectricalNodeAccess, Double> copy = new IdentityHashMap<>();
        copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }

    private static PhysicalNodeKey sourceTerminal(ElectricalEdgeTelemetry telemetry) {
        return telemetry.firstWasSource() ? telemetry.firstTerminal() : telemetry.secondTerminal();
    }

    private static List<Map.Entry<PhysicalNodeKey, PhysicalNetworkNode>> orderedEntries(
            IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph
    ) {
        return graph.keys().stream()
                .sorted()
                .map(key -> Map.entry(key, graph.value(key).orElseThrow()))
                .toList();
    }

    private static List<NodePair> orderedEdges(IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph) {
        return graph.edges().stream()
                .map(edge -> NodePair.of(edge.first(), edge.second()))
                .sorted()
                .toList();
    }

    private static LogisticsNetworkNode logisticsNode(
            IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph,
            PhysicalNodeKey key
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

    private record LogisticsCacheKey(
            PhysicalNodeKey start,
            Direction incoming,
            PhysicalNodeKey preferredFirst,
            int visitLimit
    ) {
    }

    private record BoundedElectricalComponent(Set<PhysicalNodeKey> keys, boolean truncated) {
        private BoundedElectricalComponent {
            Objects.requireNonNull(keys, "keys");
        }
    }

    private record CachedLogisticsRoute(
            ItemStack stack,
            Direction direction,
            PhysicalNodeKey startNode,
            PhysicalNodeKey firstNode,
            PhysicalNodeKey destination,
            long topologyVersion
    ) {
        private boolean matches(
                IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode> graph,
                ItemStack candidate,
                long currentTopologyVersion
        ) {
            if (topologyVersion != currentTopologyVersion
                    || stack.getCount() != candidate.getCount()
                    || !ItemStack.isSameItemSameComponents(stack, candidate)
                    || !graph.neighbors(startNode).contains(firstNode)) {
                return false;
            }
            LogisticsNetworkNode destinationNode = logisticsNode(graph, destination);
            return destinationNode != null && !destinationNode.acceptingExternalOutputs(candidate).isEmpty();
        }
    }

    private record NodePair(PhysicalNodeKey first, PhysicalNodeKey second) implements Comparable<NodePair> {
        private static NodePair of(PhysicalNodeKey first, PhysicalNodeKey second) {
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
            return first.compareTo(second) <= 0 ? new NodePair(first, second) : new NodePair(second, first);
        }

        @Override
        public int compareTo(NodePair other) {
            int firstOrder = first.compareTo(other.first);
            return firstOrder != 0 ? firstOrder : second.compareTo(other.second);
        }
    }
}
