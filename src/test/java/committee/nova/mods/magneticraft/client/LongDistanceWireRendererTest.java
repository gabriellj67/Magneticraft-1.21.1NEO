package committee.nova.mods.magneticraft.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongDistanceWireRendererTest {
    @Test
    void singleLoadedEndpointAlwaysRendersItsWireSnapshot() {
        BlockPos lower = new BlockPos(0, 64, 0);
        BlockPos higher = new BlockPos(12, 64, 0);

        assertTrue(LongDistanceWireRenderer.shouldRenderFrom(higher, lower, false));
    }

    @Test
    void twoLoadedWireHostsRenderFromOnlyOneCanonicalEndpoint() {
        BlockPos lower = new BlockPos(0, 64, 0);
        BlockPos higher = new BlockPos(12, 64, 0);

        assertTrue(LongDistanceWireRenderer.shouldRenderFrom(lower, higher, true));
        assertFalse(LongDistanceWireRenderer.shouldRenderFrom(higher, lower, true));
    }
}
