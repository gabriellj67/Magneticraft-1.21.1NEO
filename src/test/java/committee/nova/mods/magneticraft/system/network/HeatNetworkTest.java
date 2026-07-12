package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.heat.HeatLink;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatNetworkTest {
    @Test
    void conductiveTransferConservesInternalEnergyAndCannotOvershoot() {
        HeatNode hot = new HeatNode(1.0, 73.0);
        HeatNode cold = new HeatNode(1.0, 73.0);
        hot.setTemperature(700.0);
        cold.setTemperature(300.0);
        double before = hot.internalEnergyJoules() + cold.internalEnergyJoules();

        HeatLink.Transfer transfer = HeatLink.transfer(hot, cold, 1.0, Double.MAX_VALUE);

        assertTrue(transfer.moved());
        assertEquals(before, hot.internalEnergyJoules() + cold.internalEnergyJoules(), 1.0E-6);
        assertTrue(hot.temperatureKelvin() >= cold.temperatureKelvin());
        assertTrue(hot.temperatureKelvin() < 700.0);
        assertTrue(cold.temperatureKelvin() > 300.0);
    }

    @Test
    void temperatureRoundTripsThroughLegacyMaterialFormula() {
        HeatNode node = new HeatNode(1.0, 73.0);
        node.setTemperature(523.15);
        assertEquals(523.15, node.temperatureKelvin(), 1.0E-9);
        assertTrue(node.internalEnergyJoules() > 0.0);
    }
}
