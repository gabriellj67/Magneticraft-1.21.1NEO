package committee.nova.mods.magneticraft.system.network.fluid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidComponentStorageTest {
    private static final String WATER = "minecraft:water";
    private static final String LAVA = "minecraft:lava";

    @Test
    void aggregateOperationsPreserveOrderingCapacityAndSimulationPurity() {
        FluidNode first = new FluidNode(160);
        FluidNode second = new FluidNode(160);
        FluidNode third = new FluidNode(160);
        FluidComponentStorage storage = new FluidComponentStorage(List.of(first, second, third));

        assertEquals(480, storage.fill(WATER, 600, true));
        assertEquals(0, first.amount());
        assertEquals(0, second.amount());
        assertEquals(0, third.amount());

        assertEquals(400, storage.fill(WATER, 400, false));
        assertEquals(160, first.amount());
        assertEquals(160, second.amount());
        assertEquals(80, third.amount());

        assertEquals(220, storage.drain(WATER, 220, true));
        assertEquals(400, first.amount() + second.amount() + third.amount());
        assertEquals(220, storage.drainAny(220, false).amount());
        assertEquals(0, first.amount());
        assertEquals(100, second.amount());
        assertEquals(80, third.amount());
    }

    @Test
    void mixedComponentsFollowLegacyConcatenationOrder() {
        FluidNode water = new FluidNode(160);
        FluidNode lava = new FluidNode(160);
        FluidNode empty = new FluidNode(160);
        water.fill(WATER, 80, false);
        lava.fill(LAVA, 40, false);
        FluidComponentStorage storage = new FluidComponentStorage(List.of(water, lava, empty));

        assertEquals(160, storage.fill(WATER, 160, true));
        assertEquals(160, storage.fill(WATER, 160, false));
        assertEquals(160, water.amount());
        assertEquals(40, lava.amount());
        assertEquals(80, empty.amount());
        assertEquals(WATER, empty.fluidKey());
        assertFalse(storage.isFluidValid(1, WATER));
        assertTrue(storage.isFluidValid(2, WATER));

        FluidComponentStorage.Drain drained = storage.drainAny(200, false);
        assertEquals(WATER, drained.fluidKey());
        assertEquals(200, drained.amount());
        assertEquals(40, water.amount() + empty.amount());
        assertEquals(40, lava.amount());
    }
}
