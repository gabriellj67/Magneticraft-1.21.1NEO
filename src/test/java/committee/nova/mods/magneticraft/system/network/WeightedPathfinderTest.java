package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.core.WeightedPathfinder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPathfinderTest {
    @Test
    void restrictionWeightLosesToLongerNormalRoute() {
        Map<String, List<WeightedPathfinder.WeightedEdge<String>>> graph = Map.of(
                "start", List.of(edge("restricted", 101), edge("normal-a", 1)),
                "restricted", List.of(edge("target", 1)),
                "normal-a", List.of(edge("normal-b", 1)),
                "normal-b", List.of(edge("target", 1)),
                "target", List.of()
        );

        var result = WeightedPathfinder.find("start", "target"::equals, graph::get, 16);

        assertEquals(List.of("start", "normal-a", "normal-b", "target"), result.path().orElseThrow());
        assertTrue(result.visitedNodes() <= 5);
        assertTrue(result.maxQueueDepth() <= 2);
    }

    @Test
    void searchStopsAtHardVisitBudget() {
        var result = WeightedPathfinder.find(
                0,
                node -> node == 100,
                node -> List.of(edge(node + 1, 1)),
                10
        );
        assertTrue(result.path().isEmpty());
        assertTrue(result.truncated());
        assertEquals(10, result.visitedNodes());
    }

    private static <K> WeightedPathfinder.WeightedEdge<K> edge(K target, int weight) {
        return new WeightedPathfinder.WeightedEdge<>(target, weight);
    }
}
