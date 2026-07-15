package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalNetworkTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void energyUsesHalfCapacitanceVoltageSquaredAndAllowsRatedOvervoltage() {
        ElectricalNode node = new ElectricalNode(2.0D, 125.0D, 0.005D);
        node.setVoltage(120.0D);

        assertEquals(14_400.0D, node.energyJoules(), EPSILON);
        assertEquals(15_625.0D, node.ratedMaximumEnergyJoules(), EPSILON);
        assertEquals(500.0D, node.absoluteMaximumVoltage(), EPSILON);
        assertEquals(250_000.0D, node.maxEnergyJoules(), EPSILON);

        double accepted = node.addEnergy(Double.MAX_VALUE, false);
        assertEquals(235_600.0D, accepted, EPSILON);
        assertEquals(500.0D, node.voltage(), EPSILON);
        assertEquals(0.0D, node.addEnergy(1.0D, false), EPSILON);
    }

    @Test
    void exactRcTransferBalancesFastAdjacentNodesAndAccountsForEveryJoule() {
        ElectricalNode source = new ElectricalNode(0.25D, 125.0D, 0.001D);
        ElectricalNode target = new ElectricalNode(0.25D, 125.0D, 0.001D);
        source.setVoltage(120.0D);
        double before = source.energyJoules() + target.energyJoules();

        source.beginNetworkTick();
        target.beginNetworkTick();
        ElectricalLink.Transfer transfer = ElectricalLink.transfer(source, target, 1.0D);
        source.completeNetworkTick();
        target.completeNetworkTick();
        double after = source.energyJoules() + target.energyJoules();

        assertTrue(transfer.moved());
        assertEquals(60.0D, source.voltage(), EPSILON);
        assertEquals(60.0D, target.voltage(), EPSILON);
        assertEquals(15.0D, transfer.chargeCoulombsPerTick(), EPSILON);
        assertEquals(300.0D, transfer.currentAmps(), EPSILON);
        assertEquals(15.0D, source.lastCompletedTickChargeCoulombs(), EPSILON);
        assertEquals(300.0D, source.lastCompletedTickCurrentAmps(), EPSILON);
        assertEquals(900.0D, transfer.lostJoules(), EPSILON);
        assertEquals(transfer.withdrawnJoules(), transfer.deliveredJoules() + transfer.lostJoules(), EPSILON);
        assertEquals(before - after, transfer.lostJoules(), EPSILON);
    }

    @Test
    void exactRcKnownResultUsesTickSecondsTotalResistanceAndDistance() {
        ElectricalNode source = new ElectricalNode(2.0D, 500.0D, 1.0D);
        ElectricalNode target = new ElectricalNode(1.0D, 500.0D, 1.0D);
        source.setVoltage(100.0D);

        double equivalentCapacitance = 2.0D / 3.0D;
        double expectedCharge = equivalentCapacitance * 100.0D
                * (1.0D - Math.exp(-0.05D / (6.0D * equivalentCapacitance)));
        ElectricalLink.Transfer transfer = ElectricalLink.transfer(source, target, 3.0D, true);

        assertEquals(expectedCharge, transfer.chargeCoulombsPerTick(), EPSILON);
        assertEquals(100.0D, source.voltage(), EPSILON);
        assertEquals(0.0D, target.voltage(), EPSILON);
        assertEquals(0.0D, source.lastCompletedTickChargeCoulombs(), EPSILON);
    }

    @Test
    void simulationAndSafetyBoundariesDoNotMutateStateOrTelemetry() {
        ElectricalNode node = new ElectricalNode(0.25D, 125.0D, 0.001D);
        double accepted = node.addEnergy(Double.MAX_VALUE, true);
        assertEquals(node.maxEnergyJoules(), accepted, EPSILON);
        assertEquals(0.0D, node.energyJoules(), EPSILON);
        assertEquals(1.0D, node.applyCharge(1.0D, true), EPSILON);

        node.setVoltage(500.0D);
        node.beginNetworkTick();
        assertEquals(0.0D, node.applyCharge(1.0D, false), EPSILON);
        assertEquals(100.0D, node.removeEnergy(100.0D, true), EPSILON);
        node.completeNetworkTick();
        assertEquals(0.0D, node.lastCompletedTickChargeCoulombs(), EPSILON);
        assertEquals(0.0D, node.lastCompletedTickJoules(), EPSILON);
    }

    @Test
    void unlikeCapacitancesAndLongerDistanceRemainBoundedAndConserveEnergy() {
        ElectricalNode first = new ElectricalNode(0.25D, 125.0D, 0.5D);
        ElectricalNode second = new ElectricalNode(1.0D, 125.0D, 0.5D);
        first.setVoltage(120.0D);
        double before = first.energyJoules() + second.energyJoules();

        ElectricalLink.Transfer nearby = ElectricalLink.transfer(first, second, 1.0D, true);
        ElectricalLink.Transfer distant = ElectricalLink.transfer(first, second, 8.0D, true);
        ElectricalLink.Transfer committed = ElectricalLink.transfer(first, second, 8.0D);
        double after = first.energyJoules() + second.energyJoules();

        assertTrue(nearby.chargeCoulombsPerTick() > distant.chargeCoulombsPerTick());
        assertTrue(after <= before + EPSILON);
        assertEquals(committed.withdrawnJoules(), committed.deliveredJoules() + committed.lostJoules(), EPSILON);
        assertEquals(before - after, committed.lostJoules(), EPSILON);
        assertThrows(IllegalArgumentException.class, () -> ElectricalLink.transfer(first, second, 0.0D));
    }

    @Test
    void profileReconfigurationPreservesJoulesEvenAboveNewAbsoluteBoundary() {
        ElectricalNode node = new ElectricalNode(1.0D, 500.0D, 0.005D);
        node.setVoltage(1_000.0D);
        double stored = node.energyJoules();

        node.reconfigure(0.25D, 125.0D, 0.01D);

        assertEquals(stored, node.energyJoules(), EPSILON);
        assertTrue(node.voltage() > node.absoluteMaximumVoltage());
        assertEquals(0.0D, node.addEnergy(1.0D, false), EPSILON);
        assertEquals(1.0D, node.removeEnergy(1.0D, false), EPSILON);
    }

    @Test
    void completedTelemetryUsesCoulombsPerTickAmpsJoulesPerTickAndWatts() {
        ElectricalNode node = new ElectricalNode(1.0D, 125.0D, 0.001D);
        node.setVoltage(60.0D);
        node.beginNetworkTick();
        node.applyCharge(-0.25D, false);
        node.applyCharge(0.10D, false);
        node.completeNetworkTick();

        assertEquals(0.35D, node.lastCompletedTickChargeCoulombs(), EPSILON);
        assertEquals(7.0D, node.lastCompletedTickCurrentAmps(), EPSILON);
        assertEquals(node.lastCompletedTickJoules() * 20.0D, node.lastCompletedTickPowerWatts(), EPSILON);
        assertTrue(node.lastCompletedTickJoules() > 0.0D);

        node.beginNetworkTick();
        node.completeNetworkTick();
        assertEquals(0.0D, node.lastCompletedTickCurrentAmps(), EPSILON);
    }

    @Test
    void invalidPhysicalParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ElectricalNode(0.0D, 125.0D, 0.1D));
        assertThrows(IllegalArgumentException.class, () -> new ElectricalNode(1.0D, Double.NaN, 0.1D));
        assertThrows(IllegalArgumentException.class, () -> new ElectricalNode(1.0D, 125.0D, -0.1D));
    }
}
