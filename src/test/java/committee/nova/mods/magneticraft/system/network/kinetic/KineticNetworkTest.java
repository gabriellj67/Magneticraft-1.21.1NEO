package committee.nova.mods.magneticraft.system.network.kinetic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KineticNetworkTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void transferConservesEnergyAndEqualisesAngularVelocity() {
        KineticNode light = new KineticNode(1.0D, 1_000.0D);
        KineticNode heavy = new KineticNode(3.0D, 1_000.0D);
        light.insertJoules(400.0D, false);

        KineticLink.Transfer transfer = KineticLink.transfer(light, heavy, 1_000.0D);

        assertTrue(transfer.moved());
        assertEquals(400.0D, light.energyJoules() + heavy.energyJoules(), EPSILON);
        assertEquals(light.angularVelocityRadiansPerSecond(),
                heavy.angularVelocityRadiansPerSecond(), EPSILON);
        assertEquals(100.0D, light.energyJoules(), EPSILON);
        assertEquals(300.0D, heavy.energyJoules(), EPSILON);
    }

    @Test
    void insertionExtractionAndSimulationAreCapacitySafe() {
        KineticNode node = new KineticNode(2.0D, 100.0D);
        assertEquals(100.0D, node.insertJoules(150.0D, true), EPSILON);
        assertEquals(0.0D, node.energyJoules(), EPSILON);
        assertEquals(100.0D, node.insertJoules(150.0D, false), EPSILON);
        assertEquals(40.0D, node.extractJoules(40.0D, true), EPSILON);
        assertEquals(100.0D, node.energyJoules(), EPSILON);
        assertEquals(100.0D, node.extractJoules(150.0D, false), EPSILON);
        assertEquals(0.0D, node.energyJoules(), EPSILON);
    }
}
