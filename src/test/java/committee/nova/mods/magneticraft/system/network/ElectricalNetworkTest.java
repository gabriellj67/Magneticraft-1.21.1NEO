package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalNetworkTest {
    private static final double EPSILON = 1.0E-6;

    @Test
    void legacyRcTransferBalancesAdjacentCableNodesAndAccountsForEveryJoule() {
        ElectricalNode source = new ElectricalNode(0.25, 125.0, 0.001);
        ElectricalNode target = new ElectricalNode(0.25, 125.0, 0.001);
        source.setVoltage(120.0);
        double before = source.energyJoules() + target.energyJoules();

        source.beginNetworkTick();
        target.beginNetworkTick();
        ElectricalLink.Transfer transfer = ElectricalLink.transfer(source, target, 1.0);
        source.completeNetworkTick();
        target.completeNetworkTick();
        double after = source.energyJoules() + target.energyJoules();

        assertTrue(transfer.moved());
        assertEquals(60.0, source.voltage(), EPSILON);
        assertEquals(60.0, target.voltage(), EPSILON);
        assertEquals(15.0, transfer.currentAmps(), EPSILON);
        assertEquals(15.0, source.lastCompletedTickCurrentAmps(), EPSILON);
        assertEquals(15.0, target.lastCompletedTickCurrentAmps(), EPSILON);
        assertEquals(1_800.0, transfer.lostJoules(), EPSILON);
        assertEquals(transfer.withdrawnJoules(), transfer.deliveredJoules() + transfer.lostJoules(), EPSILON);
        assertEquals(before - after, transfer.lostJoules(), EPSILON);
    }

    @Test
    void simulationAndCapacityBoundariesDoNotMutate() {
        ElectricalNode node = new ElectricalNode(0.25, 125.0, 0.001);
        double accepted = node.addEnergy(10_000.0, true);
        assertEquals(node.maxEnergyJoules(), accepted, EPSILON);
        assertEquals(0.0, node.energyJoules(), EPSILON);
        assertEquals(1.0, node.applyCharge(1.0, true), EPSILON);
        assertEquals(0.0, node.energyJoules(), EPSILON);

        node.addEnergy(10_000.0, false);
        assertEquals(125.0, node.voltage(), EPSILON);
        double removed = node.removeEnergy(100.0, true);
        assertEquals(100.0, removed, EPSILON);
        assertEquals(node.maxEnergyJoules(), node.energyJoules(), EPSILON);

        double rejectedCharge = node.applyCharge(1.0, false);
        assertEquals(0.0, rejectedCharge, EPSILON);
        double drained = node.removeEnergy(Double.MAX_VALUE, false);
        assertEquals(node.maxEnergyJoules(), drained, EPSILON);
        assertEquals(0.0, node.energyJoules(), EPSILON);
        assertEquals(0.0, node.removeEnergy(1.0, false), EPSILON);
    }

    @Test
    void unlikeCapacitancesRemainBoundedAndCannotCreateEnergy() {
        ElectricalNode first = new ElectricalNode(0.25, 125.0, 0.001);
        ElectricalNode second = new ElectricalNode(1.0, 125.0, 0.001);
        first.setVoltage(120.0);
        double before = first.energyJoules() + second.energyJoules();

        ElectricalLink.Transfer transfer = ElectricalLink.transfer(first, second, 1.0);
        double after = first.energyJoules() + second.energyJoules();

        assertTrue(transfer.moved());
        assertTrue(after <= before + EPSILON);
        assertEquals(transfer.withdrawnJoules(), transfer.deliveredJoules() + transfer.lostJoules(), EPSILON);
        assertThrows(IllegalArgumentException.class, () -> ElectricalLink.transfer(first, second, 0.0));
    }

    @Test
    void completedTickCurrentIsAbsoluteThroughputFromChargeAndPowerChanges() {
        ElectricalNode node = new ElectricalNode(1.0, 125.0, 0.001);
        node.setVoltage(60.0);

        node.addEnergy(100.0, false);
        double expectedFromPower = Math.sqrt(3_700.0) - 60.0;
        node.beginNetworkTick();
        node.applyCharge(-0.25, false);
        node.applyCharge(0.10, false);
        node.completeNetworkTick();

        assertEquals(
                expectedFromPower + 0.35,
                node.lastCompletedTickCurrentAmps(),
                EPSILON
        );
        assertTrue(node.lastCompletedTickCurrentAmps() >= 0.0);

        node.beginNetworkTick();
        node.completeNetworkTick();
        assertEquals(0.0, node.lastCompletedTickCurrentAmps(), EPSILON);
    }
}
