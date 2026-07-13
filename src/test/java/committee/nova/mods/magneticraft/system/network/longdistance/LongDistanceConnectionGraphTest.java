package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongDistanceConnectionGraphTest {
    @Test
    void connectorAndPoleDistancesAreIndependentFrozenContracts() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(endpoint(0, LongDistancePort.CONNECTOR), endpoint(8, LongDistancePort.CONNECTOR))
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(endpoint(0, LongDistancePort.CONNECTOR), endpoint(9, LongDistancePort.CONNECTOR))
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(endpoint(0, LongDistancePort.POLE), endpoint(16, LongDistancePort.POLE))
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(endpoint(0, LongDistancePort.POLE), endpoint(17, LongDistancePort.POLE))
        );
        assertEquals(2, graph.size());
    }

    @Test
    void graphRejectsInvalidAndDuplicateEdgesWithoutChangingTopologyVersion() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        LongDistanceEndpoint connector = endpoint(0, LongDistancePort.CONNECTOR);
        LongDistanceEndpoint otherConnector = endpoint(4, LongDistancePort.CONNECTOR);

        assertEquals(LongDistanceConnectionGraph.ConnectResult.SAME_ENDPOINT, graph.connect(connector, connector));
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.INCOMPATIBLE_PORT,
                graph.connect(connector, endpoint(4, LongDistancePort.POLE))
        );
        assertEquals(0L, graph.mutationVersion());
        assertEquals(LongDistanceConnectionGraph.ConnectResult.SUCCESS, graph.connect(connector, otherConnector));
        assertEquals(1L, graph.mutationVersion());
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.ALREADY_CONNECTED,
                graph.connect(otherConnector, connector)
        );
        assertEquals(1L, graph.mutationVersion());

        var steadySnapshot = graph.connections();
        for (int tick = 0; tick < 1_024; tick++) {
            assertSame(steadySnapshot, graph.connections());
        }
        assertEquals(1L, graph.mutationVersion(), "Steady simulation reads rebuilt or mutated the topology");

        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(otherConnector, endpoint(8, LongDistancePort.CONNECTOR))
        );
        var changedSnapshot = graph.connections();
        assertNotSame(steadySnapshot, changedSnapshot);
        assertSame(changedSnapshot, graph.connections());
    }

    @Test
    void transformerReconnectPrefersExistingPoleEdgeToOutOfRangeConnectorPort() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(12, 64, 0);

        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(
                        new LongDistanceEndpoint(first, LongDistancePort.POLE),
                        new LongDistanceEndpoint(second, LongDistancePort.POLE)
                )
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.ALREADY_CONNECTED,
                graph.connect(
                        new LongDistanceEndpoint(first, LongDistancePort.POLE),
                        new LongDistanceEndpoint(second, LongDistancePort.POLE)
                )
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(
                        new LongDistanceEndpoint(first, LongDistancePort.CONNECTOR),
                        new LongDistanceEndpoint(second, LongDistancePort.CONNECTOR)
                )
        );
        assertEquals(
                LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                LongDistanceElectricityService.selectConnectionResult(0, true, true)
        );
        assertEquals(1, graph.size(), "Rejected connector attempt changed the existing pole topology");
    }

    @Test
    void stableThousandNodeTopologyReusesItsSortedSnapshot() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        for (int x = 1; x < 1_024; x++) {
            assertEquals(
                    LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                    graph.connect(endpoint(x - 1, LongDistancePort.POLE), endpoint(x, LongDistancePort.POLE))
            );
        }

        var steadySnapshot = graph.connections();
        assertEquals(1_023, steadySnapshot.size());
        for (int tick = 0; tick < 1_024; tick++) {
            assertSame(steadySnapshot, graph.connections());
        }
        assertEquals(1_023L, graph.mutationVersion());
    }

    @Test
    void savedDataRoundTripIsVersionedDeterministicAndRejectsLegacyPayloads() {
        LongDistanceElectricitySavedData data = new LongDistanceElectricitySavedData();
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                data.connect(endpoint(8, LongDistancePort.CONNECTOR), endpoint(0, LongDistancePort.CONNECTOR))
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                data.connect(endpoint(16, LongDistancePort.POLE), endpoint(0, LongDistancePort.POLE))
        );

        CompoundTag firstSave = data.save(new CompoundTag());
        assertEquals(LongDistanceElectricitySavedData.SCHEMA_VERSION, firstSave.getInt("schema_version"));
        LongDistanceElectricitySavedData restored = LongDistanceElectricitySavedData.load(firstSave);
        CompoundTag secondSave = restored.save(new CompoundTag());
        assertEquals(firstSave, secondSave);
        assertEquals(2, restored.connections().size());

        CompoundTag versionless = firstSave.copy();
        versionless.remove("schema_version");
        assertTrue(LongDistanceElectricitySavedData.load(versionless).connections().isEmpty());
        CompoundTag future = firstSave.copy();
        future.putInt("schema_version", 99);
        assertTrue(LongDistanceElectricitySavedData.load(future).connections().isEmpty());
    }

    @Test
    void endpointRemovalDeletesEveryIncidentEdgeExactlyOnce() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        LongDistanceEndpoint center = endpoint(0, LongDistancePort.POLE);
        graph.connect(center, endpoint(4, LongDistancePort.POLE));
        graph.connect(center, endpoint(8, LongDistancePort.POLE));
        graph.connect(endpoint(8, LongDistancePort.POLE), endpoint(12, LongDistancePort.POLE));

        assertEquals(2, graph.removeAt(center.position()));
        assertEquals(1, graph.size());
        assertFalse(graph.connections().get(0).containsPosition(center.position()));
        assertEquals(4L, graph.mutationVersion());
    }

    private static LongDistanceEndpoint endpoint(int x, LongDistancePort port) {
        return new LongDistanceEndpoint(new BlockPos(x, 64, 0), port);
    }
}
