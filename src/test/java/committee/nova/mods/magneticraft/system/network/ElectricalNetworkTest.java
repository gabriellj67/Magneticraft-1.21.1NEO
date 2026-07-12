package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalNetworkTest {
    private static final double EPSILON = 1.0E-6;

    @Test
    void resistiveTransferAccountsForEveryJoule() {
        ElectricalNode source = new ElectricalNode(1.0, 125.0, 0.001);
        ElectricalNode target = new ElectricalNode(1.0, 125.0, 0.001);
        source.setVoltage(120.0);
        double before = source.energyJoules() + target.energyJoules();

        ElectricalLink.Transfer transfer = ElectricalLink.transfer(source, target, 0.001, 8.0);
        double after = source.energyJoules() + target.energyJoules();

        assertTrue(transfer.moved());
        assertEquals(transfer.withdrawnJoules(), transfer.deliveredJoules() + transfer.lostJoules(), EPSILON);
        assertEquals(before - after, transfer.lostJoules(), EPSILON);
        assertTrue(source.voltage() <= 125.0);
        assertTrue(target.voltage() <= 125.0);
    }

    @Test
    void simulationAndCapacityBoundariesDoNotMutate() {
        ElectricalNode node = new ElectricalNode(0.25, 125.0, 0.001);
        double accepted = node.addEnergy(10_000.0, true);
        assertEquals(node.maxEnergyJoules(), accepted, EPSILON);
        assertEquals(0.0, node.energyJoules(), EPSILON);

        node.addEnergy(10_000.0, false);
        assertEquals(125.0, node.voltage(), EPSILON);
        double removed = node.removeEnergy(100.0, true);
        assertEquals(100.0, removed, EPSILON);
        assertEquals(node.maxEnergyJoules(), node.energyJoules(), EPSILON);
    }
}
