package committee.nova.mods.magneticraft.system.network.heat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatLegacyContractTest {
    @Test
    void ironDefaultsAndAmbientTemperatureMatchNovaSnapshot() {
        HeatNode node = new HeatNode(1.0D, 73.0D);

        assertEquals(298.15D, HeatNode.AMBIENT_TEMPERATURE_KELVIN, 0.0D);
        assertEquals(8.3144598D, HeatNode.IDEAL_GAS_CONSTANT, 0.0D);
        assertEquals(55.845D, HeatNode.IRON_MOLAR_MASS_GRAMS, 0.0D);
        assertEquals(298.15D, node.temperatureKelvin(), 1.0E-9D);
    }

    @Test
    void equalIronNodesTransferExactlyLegacyConductiveAmount() {
        HeatNode hot = new HeatNode(1.0D, 73.0D);
        HeatNode cold = new HeatNode(1.0D, 73.0D);
        hot.setTemperature(698.15D);
        cold.setTemperature(298.15D);
        double before = hot.internalEnergyJoules() + cold.internalEnergyJoules();

        HeatLink.Transfer transfer = HeatLink.transfer(hot, cold, 1.0D, Double.MAX_VALUE);

        assertEquals(14_600.0D, transfer.movedJoules(), 1.0E-9D);
        assertEquals(before, hot.internalEnergyJoules() + cold.internalEnergyJoules(), 1.0E-9D);
    }

    @Test
    void rawPipeContactPreservesReleasedBoundaryBehavior() {
        assertEquals(0.0F, HeatPipeContactDamage.atCelsius(79.999D));
        assertEquals(20.0F, HeatPipeContactDamage.atCelsius(80.0D));
        assertEquals(2.0F, HeatPipeContactDamage.atCelsius(80.001D));
        assertEquals(2.0F, HeatPipeContactDamage.atCelsius(2_500.0D));
    }
}
