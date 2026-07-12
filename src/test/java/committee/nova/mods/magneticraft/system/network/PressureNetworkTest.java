package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.pressure.PressureLink;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureNetworkTest {
    @Test
    void gasTransferIsConservativeWithoutLeak() {
        PressureNode source = new PressureNode(2.0, 1_000.0);
        PressureNode target = new PressureNode(1.0, 1_000.0);
        source.setPressureKpa(600.0);
        target.setPressureKpa(100.0);
        double before = source.gasKpaLiters() + target.gasKpaLiters();

        PressureLink.Transfer transfer = PressureLink.transfer(source, target, 1.0, 100.0, 0.0);

        assertTrue(transfer.moved());
        assertEquals(before, source.gasKpaLiters() + target.gasKpaLiters(), 1.0E-9);
        assertEquals(0.0, transfer.leaked(), 1.0E-9);
        assertTrue(source.pressureKpa() > target.pressureKpa());
    }

    @Test
    void explicitLeakAndCapacityAreAccountedFor() {
        PressureNode source = new PressureNode(1.0, 1_000.0);
        PressureNode target = new PressureNode(1.0, 150.0);
        source.setPressureKpa(900.0);
        double before = source.gasKpaLiters();

        PressureLink.Transfer transfer = PressureLink.transfer(source, target, 10.0, 1_000.0, 0.1);

        assertEquals(before - transfer.leaked(), source.gasKpaLiters() + target.gasKpaLiters(), 1.0E-9);
        assertTrue(target.pressureKpa() <= 150.0);
    }
}
