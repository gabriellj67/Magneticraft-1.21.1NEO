package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.network.module.ConveyorBeltModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.LogisticsTubeModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseCandidateBudgetContractTest {
    @Test
    void simulationStorageAndVirtualMachineBudgetsAreFixed() {
        assertEquals(256, BoundedComputerVm.MAX_PROGRAM_LENGTH);
        assertEquals(64, BoundedComputerVm.MAX_INSTRUCTIONS_PER_TICK);
        assertEquals(8, BoundedComputerVm.REGISTER_COUNT);
        assertEquals(64, BoundedComputerVm.RAM_SIZE);
        assertEquals(16, ConveyorBeltModule.MAX_PARCELS);
        assertEquals(64, LogisticsTubeModule.MAX_PAYLOADS);
        assertEquals(5, AdvancedMultiblockMenu.MAX_TANKS);
    }

    @Test
    void historicalAnimationAndRendererWorkAreBounded() {
        assertEquals(12, AdvancedMultiblockRenderer.STEAM_TURBINE_BLADE_COUNT);
    }

    @Test
    void endpointSnapshotAndWireGeometryHaveAConstantWorkCeiling() {
        LongDistanceEndpointModule endpoint = endpointLoadedWithConnections(80);
        assertEquals(64, endpoint.clientConnections().size());
        assertEquals(8, LongDistanceWireRenderer.segmentCount(0.0F));
        assertEquals(8, LongDistanceWireRenderer.segmentCount(4.0F));
        assertEquals(32, LongDistanceWireRenderer.segmentCount(64.0F));
        assertEquals(32, LongDistanceWireRenderer.segmentCount(Float.MAX_VALUE));

        int maximumVertices = endpoint.clientConnections().size()
                * LongDistancePort.POLE.wireCount()
                * LongDistanceWireRenderer.MAX_SEGMENTS
                * 2;
        assertEquals(12_288, maximumVertices);
    }

    private static LongDistanceEndpointModule endpointLoadedWithConnections(int count) {
        LongDistanceEndpointModule endpoint = new LongDistanceEndpointModule(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "release_candidate_budget"),
                new TestEndpointHost()
        );
        ListTag connections = new ListTag();
        for (int index = 0; index < count; index++) {
            CompoundTag connection = new CompoundTag();
            connection.putInt("x", index + 1);
            connection.putInt("y", 64);
            connection.putInt("z", 0);
            connection.putString("terminal_id", PhysicalNodeKey.MAIN_TERMINAL.toString());
            connection.putString("port", LongDistancePort.POLE.serializedName());
            connection.putString("tier_id", VoltageTierIds.HIGH.toString());
            connections.add(connection);
        }
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("connections", connections);
        endpoint.loadClientData(snapshot);
        return endpoint;
    }

    private static final class TestEndpointHost implements LongDistanceEndpointHost {
        @Override
        public ElectricalNetworkModule electricity() {
            return null;
        }

        @Override
        public Set<LongDistancePort> longDistancePorts() {
            return Set.of(LongDistancePort.POLE);
        }

        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Override
        public Level level() {
            return null;
        }

        @Override
        public BlockPos position() {
            return BlockPos.ZERO;
        }
    }
}
