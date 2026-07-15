package committee.nova.mods.magneticraft.system.network.electric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalStressStateTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void sustainedDoubleCurrentMatchesProtectionAndCableTimingContracts() {
        assertFailsOnTick(42.0D, 14);
        assertFailsOnTick(60.0D, 20);
        assertFailsOnTick(400.0D, 134);
    }

    @Test
    void oneTickInrushDoesNotFailAndSuppressedDamageDoesNotAccumulate() {
        ElectricalStressState state = new ElectricalStressState();

        assertFalse(state.update(320.0D, 160.0D, 120.0D, 125.0D, 42.0D, 0.0025D, true));
        assertEquals(3.0D / 42.0D, state.stress(), EPSILON);
        double before = state.stress();

        assertFalse(state.update(640.0D, 160.0D, 500.0D, 125.0D, 42.0D, 0.0025D, false));
        assertEquals(before, state.stress(), EPSILON);
    }

    @Test
    void voltageAndCurrentAccumulateTogetherAndCoolingRequiresBothHealthy() {
        ElectricalStressState state = new ElectricalStressState();
        state.restore(0.5D);

        state.update(80.0D, 160.0D, 250.0D, 125.0D, 400.0D, 0.1D, true);
        assertEquals(0.53D, state.stress(), EPSILON);

        state.update(80.0D, 160.0D, 120.0D, 125.0D, 400.0D, 0.1D, true);
        assertEquals(0.43D, state.stress(), EPSILON);
    }

    @Test
    void invalidPhysicalRatingsAreRejectedAndCorruptPersistenceResetsSafe() {
        ElectricalStressState state = new ElectricalStressState();
        state.restore(Double.NaN);
        assertEquals(0.0D, state.stress(), EPSILON);
        assertThrows(IllegalArgumentException.class, () ->
                state.update(1.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D, true));
        assertThrows(IllegalArgumentException.class, () ->
                state.update(1.0D, 1.0D, 1.0D, Double.POSITIVE_INFINITY, 1.0D, 1.0D, true));
    }

    private static void assertFailsOnTick(double thermalCapacity, int expectedTick) {
        ElectricalStressState state = new ElectricalStressState();
        for (int tick = 1; tick < expectedTick; tick++) {
            assertFalse(state.update(
                    320.0D, 160.0D, 120.0D, 125.0D, thermalCapacity, 0.0025D, true
            ), "failed early at tick " + tick);
        }
        assertTrue(state.update(
                320.0D, 160.0D, 120.0D, 125.0D, thermalCapacity, 0.0025D, true
        ));
        assertTrue(state.stress() >= ElectricalStressState.FAILURE_THRESHOLD);
    }
}
