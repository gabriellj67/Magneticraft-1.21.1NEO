package committee.nova.mods.magneticraft.system.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortableEnergyStateTest {
    @Test
    void externalTransfersAreRateLimitedAndSimulationIsReadOnly() {
        PortableEnergyState state = new PortableEnergyState(2_500_000, 500);

        assertEquals(500, state.receive(10_000, true));
        assertEquals(0, state.energy());
        assertEquals(500, state.receive(10_000, false));
        assertEquals(500, state.energy());
        assertEquals(500, state.extract(10_000, true));
        assertEquals(500, state.energy());
        assertEquals(500, state.extract(10_000, false));
        assertEquals(0, state.energy());
        assertEquals(0, state.receive(-1, false));
        assertEquals(0, state.extract(-1, false));
    }

    @Test
    void internalConsumptionIsAtomicAndNotTransferLimited() {
        PortableEnergyState state = new PortableEnergyState(512_000, 500);
        state.load(4_000);

        assertFalse(state.consume(4_001));
        assertEquals(4_000, state.energy());
        assertTrue(state.consume(4_000));
        assertEquals(0, state.energy());
        assertTrue(state.consume(0));
        assertFalse(state.consume(-1));
    }

    @Test
    void loadedValuesAreClampedToTheValidRange() {
        PortableEnergyState state = new PortableEnergyState(250_000, 500);

        state.load(Integer.MAX_VALUE);
        assertEquals(250_000, state.energy());
        state.load(-1);
        assertEquals(0, state.energy());
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PortableEnergyState(0, 500));
        assertThrows(IllegalArgumentException.class, () -> new PortableEnergyState(1, 0));
    }
}
