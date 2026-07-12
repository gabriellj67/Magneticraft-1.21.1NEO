package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.core.GraphMetrics;
import committee.nova.mods.magneticraft.system.network.core.IncrementalGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalGraphTest {
    @Test
    void mergesSplitsAndRejoinsOnlyAffectedComponent() {
        IncrementalGraph<Integer, String> graph = new IncrementalGraph<>();
        for (int node = 0; node < 1_024; node++) {
            graph.addNode(node, "node-" + node);
            if (node > 0) {
                graph.connect(node - 1, node);
            }
        }
        assertEquals(1, graph.componentCount());
        assertEquals(1_024, graph.componentOf(0).size());
        GraphMetrics steadyBefore = graph.metrics();
        for (int tick = 0; tick < 100; tick++) {
            assertEquals(1_023, graph.edges().size());
        }
        assertEquals(steadyBefore, graph.metrics(), "Steady topology performed rebuild work");

        GraphMetrics before = graph.metrics();
        graph.removeNode(512);
        GraphMetrics after = graph.metrics();
        assertEquals(2, graph.componentCount());
        assertEquals(512, graph.componentOf(0).size());
        assertEquals(511, graph.componentOf(1_023).size());
        assertEquals(1_023, after.nodesVisitedDuringRebuilds() - before.nodesVisitedDuringRebuilds());
        assertTrue(after.maxRebuildQueueDepth() <= 512);

        graph.addNode(512, "restored");
        graph.connect(511, 512);
        graph.connect(512, 513);
        assertEquals(1, graph.componentCount());
        assertEquals(1_024, graph.componentOf(512).size());
    }

    @Test
    void duplicateTopologyOperationsAreNoOpsAndValueCanRefresh() {
        IncrementalGraph<String, Integer> graph = new IncrementalGraph<>();
        assertTrue(graph.addNode("a", 1));
        assertTrue(graph.addNode("b", 2));
        assertTrue(graph.connect("a", "b"));
        long version = graph.metrics().topologyVersion();

        assertFalse(graph.connect("a", "b"));
        assertFalse(graph.addNode("a", 3));
        assertEquals(3, graph.value("a").orElseThrow());
        assertEquals(version, graph.metrics().topologyVersion());
        assertTrue(graph.disconnect("a", "b"));
        assertEquals(2, graph.componentCount());
        assertFalse(graph.disconnect("a", "b"));
    }
}
