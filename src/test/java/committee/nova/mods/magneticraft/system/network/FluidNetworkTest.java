package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.fluid.FluidLink;
import committee.nova.mods.magneticraft.system.network.fluid.FluidNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidNetworkTest {
    @Test
    void linkBalancesSameFluidWithoutChangingTotal() {
        FluidNode first = new FluidNode(160);
        FluidNode second = new FluidNode(160);
        first.fill("minecraft:water", 160, false);

        FluidLink.Transfer transfer = FluidLink.transfer(first, second, 160);

        assertTrue(transfer.moved());
        assertEquals(80, first.amount());
        assertEquals(80, second.amount());
        assertEquals("minecraft:water", second.fluidKey());
    }

    @Test
    void differentFluidsDoNotMixAndSimulationIsPure() {
        FluidNode water = new FluidNode(160);
        FluidNode lava = new FluidNode(160);
        water.fill("minecraft:water", 120, false);
        lava.fill("minecraft:lava", 10, false);

        assertFalse(FluidLink.transfer(water, lava, 160).moved());
        assertEquals(40, water.fill("minecraft:water", 100, true));
        assertEquals(120, water.amount());
        assertEquals(0, water.fill("minecraft:lava", 10, false));
        assertEquals("minecraft:water", water.fluidKey());
    }
}
