package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LongDistanceRenderBoundsTest {
    private static final BlockPos LOCAL = new BlockPos(10, 64, 10);
    private static final BlockPos REMOTE = new BlockPos(-22, 70, 42);
    private static final LongDistanceEndpointModule.WireView WIRE =
            new LongDistanceEndpointModule.WireView(
                    REMOTE,
                    PhysicalNodeKey.MAIN_TERMINAL,
                    LongDistancePort.POLE,
                    VoltageTierIds.HIGH
            );

    @Test
    void connectorBoundsContainBothEndpointsAndWireSag() {
        AABB bounds = ElectricConnectorBlockEntity.renderBounds(LOCAL, List.of(WIRE));
        assertContains(bounds, LOCAL);
        assertContains(bounds, REMOTE);
        assertTrue(bounds.minY <= LOCAL.getY() - 2.0D,
                "Connector bounds do not include the rendered wire sag");
    }

    @Test
    void poleBoundsContainPoleBodyAndRemoteWireEndpoint() {
        AABB bounds = ElectricPoleBlockEntity.renderBounds(LOCAL, List.of(WIRE));
        assertTrue(bounds.minY <= LOCAL.getY() - 4.0D, "Pole body was clipped from its render bounds");
        assertContainsCenter(bounds, REMOTE);
    }

    private static void assertContains(AABB bounds, BlockPos position) {
        assertTrue(bounds.minX <= position.getX() && bounds.maxX >= position.getX() + 1.0D,
                "Render bounds do not span endpoint X");
        assertTrue(bounds.minY <= position.getY() && bounds.maxY >= position.getY() + 1.0D,
                "Render bounds do not span endpoint Y");
        assertTrue(bounds.minZ <= position.getZ() && bounds.maxZ >= position.getZ() + 1.0D,
                "Render bounds do not span endpoint Z");
    }

    private static void assertContainsCenter(AABB bounds, BlockPos position) {
        assertTrue(bounds.contains(
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D
                ),
                "Render bounds do not contain the rendered wire endpoint");
    }
}
