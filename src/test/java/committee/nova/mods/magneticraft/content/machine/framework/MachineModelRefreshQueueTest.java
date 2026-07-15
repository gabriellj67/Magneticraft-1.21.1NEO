package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineModelRefreshQueueTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");

    @AfterEach
    void clearQueue() {
        MachineModelRefreshQueue.clear();
    }

    @Test
    void coalescesSamePositionUntilNextClientTick() {
        MachineModelRefreshQueue.enqueue(OVERWORLD, new BlockPos(1, 2, 3));
        MachineModelRefreshQueue.enqueue(OVERWORLD, new BlockPos(1, 2, 3));

        assertTrue(MachineModelRefreshQueue.tickAndDrain().isEmpty());
        var requests = MachineModelRefreshQueue.tickAndDrain();

        assertEquals(1, requests.size());
        assertEquals(OVERWORLD, requests.get(0).dimension());
        assertEquals(new BlockPos(1, 2, 3), requests.get(0).position());
        assertTrue(MachineModelRefreshQueue.tickAndDrain().isEmpty());
    }

    @Test
    void keepsDimensionsAndPositionsDistinct() {
        MachineModelRefreshQueue.enqueue(OVERWORLD, BlockPos.ZERO);
        MachineModelRefreshQueue.enqueue(NETHER, BlockPos.ZERO);
        MachineModelRefreshQueue.enqueue(OVERWORLD, BlockPos.ZERO.above());

        assertTrue(MachineModelRefreshQueue.tickAndDrain().isEmpty());
        assertEquals(3, MachineModelRefreshQueue.tickAndDrain().size());
    }
}
