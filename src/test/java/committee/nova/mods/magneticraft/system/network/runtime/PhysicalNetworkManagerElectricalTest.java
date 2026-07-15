package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.core.GraphMetrics;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalCoupler;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeAccess;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicalNetworkManagerElectricalTest {
    private static final ResourceLocation LOW = ResourceLocation.fromNamespaceAndPath("magneticraft", "low_voltage");
    private static final ResourceLocation MEDIUM = ResourceLocation.fromNamespaceAndPath("magneticraft", "medium_voltage");
    private static final ResourceLocation FIRST_TERMINAL = ResourceLocation.fromNamespaceAndPath("test", "first");
    private static final ResourceLocation SECOND_TERMINAL = ResourceLocation.fromNamespaceAndPath("test", "second");

    @Test
    void samePositionTerminalsRemainIndependentUntilInternalEdgeCloses() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        BlockPos position = new BlockPos(4, 5, 6);
        TestElectricalNode first = node(position, FIRST_TERMINAL, LOW);
        TestElectricalNode second = node(position, SECOND_TERMINAL, LOW);
        first.electricalNode().setVoltage(120.0D);
        manager.register(first);
        manager.register(second);

        assertEquals(2, manager.nodeCount(NetworkDomain.ELECTRICITY));
        assertEquals(2, manager.keysAt(NetworkDomain.ELECTRICITY, position).size());
        assertEquals(1, manager.component(NetworkDomain.ELECTRICITY, first.nodeKey()).size());
        assertEquals(1, manager.component(NetworkDomain.ELECTRICITY, second.nodeKey()).size());

        manager.setInternalConnection(first.nodeKey(), second.nodeKey(), true);
        assertEquals(2, manager.component(NetworkDomain.ELECTRICITY, first.nodeKey()).size());
        manager.tick(1L);

        assertEquals(1, manager.electricalEdgeTelemetry().size());
        assertEquals(ElectricalEdgeTelemetry.EdgeType.INTERNAL,
                manager.electricalEdgeTelemetry().get(0).edgeType());
        assertTrue(second.electricalNode().energyJoules() > 0.0D);

        manager.setInternalConnection(first.nodeKey(), second.nodeKey(), false);
        assertEquals(1, manager.component(NetworkDomain.ELECTRICITY, first.nodeKey()).size());
        assertEquals(1, manager.component(NetworkDomain.ELECTRICITY, second.nodeKey()).size());
    }

    @Test
    void differentTiersNeverCreateAnOrdinaryAdjacentOrInternalEdge() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        TestElectricalNode low = node(new BlockPos(0, 0, 0), FIRST_TERMINAL, LOW);
        TestElectricalNode medium = node(new BlockPos(1, 0, 0), FIRST_TERMINAL, MEDIUM);
        manager.register(low);
        manager.register(medium);

        assertTrue(manager.neighborKeys(NetworkDomain.ELECTRICITY, low.nodeKey()).isEmpty());
        assertEquals(2, manager.componentCount(NetworkDomain.ELECTRICITY));

        TestElectricalNode samePositionMedium = node(low.position(), SECOND_TERMINAL, MEDIUM);
        manager.register(samePositionMedium);
        manager.setInternalConnection(low.nodeKey(), samePositionMedium.nodeKey(), true);
        assertFalse(manager.internalConnectionClosed(low.nodeKey(), samePositionMedium.nodeKey())
                        && manager.neighborKeys(NetworkDomain.ELECTRICITY, low.nodeKey())
                        .contains(samePositionMedium.nodeKey()),
                "A closed intent must remain electrically open when tiers differ");
    }

    @Test
    void couplerTransfersAcrossTiersWithoutMergingComponentsAndKeepsPhaseOrder() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        List<String> phases = new ArrayList<>();
        BlockPos position = new BlockPos(2, 3, 4);
        TestElectricalNode low = node(position, FIRST_TERMINAL, LOW, phases, "first");
        TestElectricalNode medium = node(position, SECOND_TERMINAL, MEDIUM, phases, "second");
        low.electricalNode().setVoltage(120.0D);
        manager.register(low);
        manager.register(medium);
        manager.registerElectricalCoupler(new TestCoupler(low.nodeKey(), medium.nodeKey(), phases));

        assertEquals(2, manager.componentCount(NetworkDomain.ELECTRICITY));
        manager.tick(1L);

        assertEquals(2, manager.componentCount(NetworkDomain.ELECTRICITY));
        assertTrue(medium.electricalNode().energyJoules() > 0.0D);
        assertEquals(ElectricalEdgeTelemetry.EdgeType.COUPLER,
                manager.electricalEdgeTelemetry().get(0).edgeType());
        assertTrue(phases.indexOf("first:inject") < phases.indexOf("coupler"));
        assertTrue(phases.indexOf("coupler") < phases.indexOf("first:extract"));
        assertTrue(phases.indexOf("first:extract") < phases.indexOf("first:commit"));
    }

    @Test
    void couplerCannotBindATerminalToItself() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        PhysicalNodeKey terminal = new PhysicalNodeKey(new BlockPos(2, 3, 4), FIRST_TERMINAL);

        assertThrows(IllegalArgumentException.class,
                () -> manager.registerElectricalCoupler(new TestCoupler(terminal, terminal, new ArrayList<>())));
    }

    @Test
    void stable1024NodeNetworkDoesNotRebuildTopologyPerTick() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        for (int x = 0; x < 1_024; x++) {
            manager.register(node(new BlockPos(x, 0, 0), PhysicalNodeKey.MAIN_TERMINAL, LOW));
        }
        GraphMetrics before = manager.metrics(NetworkDomain.ELECTRICITY);

        for (long tick = 1L; tick <= 5L; tick++) {
            manager.tick(tick);
        }
        GraphMetrics after = manager.metrics(NetworkDomain.ELECTRICITY);

        assertEquals(before.topologyVersion(), after.topologyVersion());
        assertEquals(before.componentRebuilds(), after.componentRebuilds());
        assertEquals(1, manager.componentCount(NetworkDomain.ELECTRICITY));
        assertEquals(1_024, manager.nodeCount(NetworkDomain.ELECTRICITY));
        assertEquals(5L * 1_023L, manager.edgeTicks());
    }

    @Test
    void diagnosticsReportPhaseTelemetryLoadAndNearestFault() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        TestElectricalNode source = node(new BlockPos(0, 0, 0), FIRST_TERMINAL, LOW);
        TestElectricalNode fault = node(new BlockPos(1, 0, 0), FIRST_TERMINAL, LOW);
        TestElectricalNode tail = node(new BlockPos(2, 0, 0), FIRST_TERMINAL, LOW);
        source.injectJoules = 100.0D;
        source.consumeJoules = 10.0D;
        source.ratedCurrentAmps = 1.0D;
        fault.faultKind = FaultKind.MACHINE_FAULT;
        manager.register(source);
        manager.register(fault);
        manager.register(tail);

        manager.tick(1L);

        var summary = manager.electricalNetworkSummary(
                source.nodeKey(),
                PhysicalNetworkManager.MAX_ELECTRICAL_DIAGNOSTIC_VISITS
        ).orElseThrow();
        assertEquals(3, summary.nodeCount());
        assertEquals(2, summary.edgeCount());
        assertEquals(100.0D, summary.generatedJoulesPerTick(), 1.0E-9D);
        assertEquals(10.0D, summary.consumedJoulesPerTick(), 1.0E-9D);
        assertEquals(1, summary.faultCount());
        assertTrue(summary.maximumLoadRatio() > 0.0D);
        assertFalse(summary.truncated());

        var search = manager.nearestElectricalFault(source.nodeKey(), 4_096).orElseThrow();
        assertFalse(search.truncated());
        assertEquals(3, search.visitedNodes());
        var located = search.location().orElseThrow();
        assertEquals(FaultKind.MACHINE_FAULT, located.faultKind());
        assertEquals(Direction.EAST, located.direction().orElseThrow());
        assertEquals(1.0D, located.distanceBlocks(), 1.0E-9D);
    }

    @Test
    void diagnosticsStopAtConfiguredVisitLimitWithoutLoadingAnything() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        TestElectricalNode start = null;
        TestElectricalNode last = null;
        for (int x = 0; x < 8; x++) {
            TestElectricalNode next = node(new BlockPos(x, 0, 0), FIRST_TERMINAL, LOW);
            manager.register(next);
            if (start == null) {
                start = next;
            }
            last = next;
        }
        last.faultKind = FaultKind.MACHINE_FAULT;

        var summary = manager.electricalNetworkSummary(start.nodeKey(), 3).orElseThrow();
        assertEquals(3, summary.nodeCount());
        assertEquals(3, summary.visitedNodes());
        assertTrue(summary.truncated());
        var search = manager.nearestElectricalFault(start.nodeKey(), 3).orElseThrow();
        assertEquals(3, search.visitedNodes());
        assertTrue(search.truncated());
        assertTrue(search.location().isEmpty());
    }

    private static TestElectricalNode node(
            BlockPos position,
            ResourceLocation terminal,
            ResourceLocation tier
    ) {
        return node(position, terminal, tier, new ArrayList<>(), "node");
    }

    private static TestElectricalNode node(
            BlockPos position,
            ResourceLocation terminal,
            ResourceLocation tier,
            List<String> phases,
            String phaseName
    ) {
        return new TestElectricalNode(
                new PhysicalNodeKey(position, terminal),
                tier,
                new ElectricalNode(0.5D, 2_000.0D, 0.005D),
                phases,
                phaseName
        );
    }

    private static final class TestElectricalNode
            implements PhysicalNetworkNode, ElectricalNodeAccess, ElectricalTickParticipant {
        private static final Set<Direction> SIDES = EnumSet.allOf(Direction.class);

        private final PhysicalNodeKey key;
        private final ResourceLocation tier;
        private final ElectricalNode node;
        private final List<String> phases;
        private final String phaseName;
        private int dirtyMarks;
        private double injectJoules;
        private double consumeJoules;
        private double ratedCurrentAmps = Double.POSITIVE_INFINITY;
        private FaultKind faultKind = FaultKind.NONE;

        private TestElectricalNode(
                PhysicalNodeKey key,
                ResourceLocation tier,
                ElectricalNode node,
                List<String> phases,
                String phaseName
        ) {
            this.key = key;
            this.tier = tier;
            this.node = node;
            this.phases = phases;
            this.phaseName = phaseName;
        }

        @Override
        public NetworkDomain domain() {
            return NetworkDomain.ELECTRICITY;
        }

        @Override
        public BlockPos position() {
            return key.position();
        }

        @Override
        public PhysicalNodeKey nodeKey() {
            return key;
        }

        @Override
        public Set<Direction> connectionSides() {
            return SIDES;
        }

        @Override
        public ElectricalNode electricalNode() {
            return node;
        }

        @Override
        public ResourceLocation electricalTierId() {
            return tier;
        }

        @Override
        public boolean electricalProfileBound() {
            return true;
        }

        @Override
        public double electricalRatedCurrentAmps() {
            return ratedCurrentAmps;
        }

        @Override
        public FaultKind electricalFaultKind() {
            return faultKind;
        }

        @Override
        public void markElectricalStateChanged() {
            dirtyMarks++;
        }

        @Override
        public void injectElectricalEnergy(PhysicalNetworkManager manager) {
            phases.add(phaseName + ":inject");
            node.addEnergy(injectJoules, false);
        }

        @Override
        public void extractElectricalEnergy(PhysicalNetworkManager manager) {
            phases.add(phaseName + ":extract");
            node.removeEnergy(consumeJoules, false);
        }

        @Override
        public void commitElectricalState(PhysicalNetworkManager manager) {
            phases.add(phaseName + ":commit");
        }
    }

    private record TestCoupler(
            PhysicalNodeKey firstTerminal,
            PhysicalNodeKey secondTerminal,
            List<String> phases
    ) implements ElectricalCoupler {
        @Override
        public CouplingResult transfer(ElectricalNode first, ElectricalNode second, boolean simulate) {
            phases.add("coupler");
            double available = first.removeEnergy(100.0D, true);
            double delivered = second.addEnergy(available * 0.8D, true);
            double withdrawn = delivered / 0.8D;
            double firstVoltage = first.voltage();
            double secondVoltage = second.voltage();
            if (!simulate) {
                first.removeEnergy(withdrawn, false);
                second.addEnergy(delivered, false);
            }
            double sourceCharge = first.capacitance() * Math.max(0.0D, firstVoltage - first.voltage());
            double destinationCharge = second.capacitance() * Math.max(0.0D, second.voltage() - secondVoltage);
            return new CouplingResult(
                    withdrawn,
                    delivered,
                    withdrawn - delivered,
                    sourceCharge,
                    destinationCharge,
                    true
            );
        }
    }
}
