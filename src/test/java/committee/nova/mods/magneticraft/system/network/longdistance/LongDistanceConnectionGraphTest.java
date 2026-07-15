package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongDistanceConnectionGraphTest {
    @Test
    void graphUsesTheRangeSuppliedByTheResolvedTierProfile() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(endpoint(0, LongDistancePort.CONNECTOR), endpoint(8, LongDistancePort.CONNECTOR), 8)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(endpoint(0, LongDistancePort.CONNECTOR), endpoint(9, LongDistancePort.CONNECTOR), 8)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(endpoint(0, LongDistancePort.POLE), endpoint(16, LongDistancePort.POLE), 16)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(endpoint(0, LongDistancePort.POLE), endpoint(17, LongDistancePort.POLE), 16)
        );
        assertEquals(2, graph.size());
    }

    @Test
    void graphRejectsInvalidAndDuplicateEdgesWithoutChangingTopologyVersion() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        LongDistanceEndpoint connector = endpoint(0, LongDistancePort.CONNECTOR);
        LongDistanceEndpoint otherConnector = endpoint(4, LongDistancePort.CONNECTOR);

        assertEquals(LongDistanceConnectionGraph.ConnectResult.SAME_ENDPOINT, graph.connect(connector, connector, 8));
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.INCOMPATIBLE_PORT,
                graph.connect(connector, endpoint(4, LongDistancePort.POLE), 8)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.INCOMPATIBLE_TIER,
                graph.connect(connector, endpoint(4, LongDistancePort.CONNECTOR, VoltageTierIds.MEDIUM), 8)
        );
        assertEquals(0L, graph.mutationVersion());
        assertEquals(LongDistanceConnectionGraph.ConnectResult.SUCCESS, graph.connect(connector, otherConnector, 8));
        assertEquals(1L, graph.mutationVersion());
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.ALREADY_CONNECTED,
                graph.connect(otherConnector, connector, 8)
        );
        assertEquals(1L, graph.mutationVersion());

        var steadySnapshot = graph.connections();
        for (int tick = 0; tick < 1_024; tick++) {
            assertSame(steadySnapshot, graph.connections());
        }
        assertEquals(1L, graph.mutationVersion(), "Steady simulation reads rebuilt or mutated the topology");

        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(otherConnector, endpoint(8, LongDistancePort.CONNECTOR), 8)
        );
        var changedSnapshot = graph.connections();
        assertNotSame(steadySnapshot, changedSnapshot);
        assertSame(changedSnapshot, graph.connections());
    }

    @Test
    void portAndTierArePartOfTheDurableEndpointIdentity() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        BlockPos first = new BlockPos(0, 64, 0);
        BlockPos second = new BlockPos(12, 64, 0);
        LongDistanceEndpoint firstPole = endpoint(first, LongDistancePort.POLE, VoltageTierIds.LOW);
        LongDistanceEndpoint secondPole = endpoint(second, LongDistancePort.POLE, VoltageTierIds.LOW);

        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                graph.connect(firstPole, secondPole, 16)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.ALREADY_CONNECTED,
                graph.connect(secondPole, firstPole, 16)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.TOO_FAR,
                graph.connect(
                        endpoint(first, LongDistancePort.CONNECTOR, VoltageTierIds.LOW),
                        endpoint(second, LongDistancePort.CONNECTOR, VoltageTierIds.LOW),
                        8
                )
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.INCOMPATIBLE_TIER,
                graph.connect(
                        endpoint(first, LongDistancePort.POLE, VoltageTierIds.LOW),
                        endpoint(second, LongDistancePort.POLE, VoltageTierIds.MEDIUM),
                        32
                )
        );
        assertEquals(1, graph.size(), "Rejected endpoint attempts changed the existing topology");
    }

    @Test
    void stableThousandNodeTopologyReusesItsSortedSnapshot() {
        LongDistanceConnectionGraph graph = new LongDistanceConnectionGraph();
        for (int x = 1; x < 1_024; x++) {
            assertEquals(
                    LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                    graph.connect(endpoint(x - 1, LongDistancePort.POLE), endpoint(x, LongDistancePort.POLE), 16)
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
                data.connect(endpoint(8, LongDistancePort.CONNECTOR), endpoint(0, LongDistancePort.CONNECTOR), 8)
        );
        assertEquals(
                LongDistanceConnectionGraph.ConnectResult.SUCCESS,
                data.connect(endpoint(16, LongDistancePort.POLE), endpoint(0, LongDistancePort.POLE), 16)
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
        graph.connect(center, endpoint(4, LongDistancePort.POLE), 16);
        graph.connect(center, endpoint(8, LongDistancePort.POLE), 16);
        graph.connect(endpoint(8, LongDistancePort.POLE), endpoint(12, LongDistancePort.POLE), 16);

        assertEquals(2, graph.removeAt(center.position()));
        assertEquals(1, graph.size());
        assertFalse(graph.connections().get(0).containsPosition(center.position()));
        assertEquals(4L, graph.mutationVersion());
    }

    private static LongDistanceEndpoint endpoint(int x, LongDistancePort port) {
        return endpoint(new BlockPos(x, 64, 0), port, VoltageTierIds.LOW);
    }

    private static LongDistanceEndpoint endpoint(int x, LongDistancePort port, ResourceLocation tierId) {
        return endpoint(new BlockPos(x, 64, 0), port, tierId);
    }

    private static LongDistanceEndpoint endpoint(BlockPos position, LongDistancePort port, ResourceLocation tierId) {
        return new LongDistanceEndpoint(position, PhysicalNodeKey.MAIN_TERMINAL, port, tierId);
    }
}
