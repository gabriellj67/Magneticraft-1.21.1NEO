package committee.nova.mods.magneticraft.content.machine;

import committee.nova.mods.magneticraft.content.machine.framework.EnergyBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnergyBufferTest {
    @Test
    void receiveExtractAndSimulationRespectEveryBound() {
        EnergyBuffer buffer = new EnergyBuffer(1_000, 100, 40);

        assertEquals(100, buffer.receive(500, true));
        assertEquals(0, buffer.energy(), "Simulation must not mutate energy");
        assertEquals(100, buffer.receive(500, false));
        assertEquals(100, buffer.energy());
        assertEquals(40, buffer.extract(500, true));
        assertEquals(100, buffer.energy(), "Simulated extraction must not mutate energy");
        assertEquals(40, buffer.extract(500, false));
        assertEquals(60, buffer.energy());

        buffer.setEnergy(Integer.MAX_VALUE);
        assertEquals(1_000, buffer.energy());
        assertEquals(0, buffer.receive(1, false));
        buffer.setEnergy(Integer.MIN_VALUE);
        assertEquals(0, buffer.energy());
    }

    @Test
    void negativeLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EnergyBuffer(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EnergyBuffer(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new EnergyBuffer(1, 0, -1));
    }
}
