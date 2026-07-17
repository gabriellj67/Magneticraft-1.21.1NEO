package committee.nova.mods.magneticraft.system.nuclear.thermal;

import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearThermalTransactionsTest {
    private static final ReactorParameters PARAMETERS = ReactorParameters.DEFAULT;

    @Test
    void stateConversionNeverConsumesMoreThanDestinationCanReceive() {
        var result = NuclearThermalTransactions.stateConversion(500, 120, 400);
        assertEquals(120, result.consumed());
        assertEquals(result.consumed(), result.produced());
    }

    @Test
    void pumpIsSimultaneouslyLimitedByFluidOutputEnergyAndSetting() {
        var result = NuclearThermalTransactions.pump(900, 700, 100, 0.5D, PARAMETERS);
        assertEquals(200, result.coolantMoved());
        assertEquals(100, result.energyConsumedJoules());
    }

    @Test
    void steamGeneratorPreservesPrimaryCoolantAndBlocksOnEitherOutput() {
        var result = NuclearThermalTransactions.steamGenerator(500, 500, 500, 1_230, 500, PARAMETERS);
        assertEquals(123, result.hotCoolantConsumed());
        assertEquals(result.hotCoolantConsumed(), result.coldCoolantProduced());
        assertEquals(123, result.waterConsumed());
        assertEquals(1_230, result.steamProduced());

        var blocked = NuclearThermalTransactions.steamGenerator(500, 0, 500, 5_000, 500, PARAMETERS);
        assertEquals(0, blocked.hotCoolantConsumed());
        assertEquals(0, blocked.steamProduced());
    }

    @Test
    void turbineClosedLoopBackpressuresWhileLegacyVentingLosesTwentyPercent() {
        var closed = NuclearThermalTransactions.turbine(600, 200, 1_200, 600, false, PARAMETERS);
        assertEquals(200, closed.steamConsumed());
        assertEquals(200, closed.exhaustProduced());
        assertEquals(400, closed.energyGeneratedJoules());
        assertFalse(closed.vented());

        var vented = NuclearThermalTransactions.turbine(600, 0, 1_200, 600, true, PARAMETERS);
        assertEquals(600, vented.steamConsumed());
        assertEquals(0, vented.exhaustProduced());
        assertEquals(960, vented.energyGeneratedJoules());
        assertTrue(vented.vented());
    }

    @Test
    void condenserRoundsDownToWholeWaterUnitsAndHonorsHeatAcceptance() {
        var result = NuclearThermalTransactions.condenser(999, 50, 1_000, 999, PARAMETERS);
        assertEquals(250, result.exhaustConsumed());
        assertEquals(25, result.waterProduced());
        assertEquals(1_000L, result.heatRejectedJoules());
    }

    @Test
    void coolingTowerNeedsFillFansAndMakeupAndDeratesInHotAir() {
        long normal = NuclearThermalTransactions.coolingTowerCapacity(100, 5, 298.15D, 100, PARAMETERS);
        long hot = NuclearThermalTransactions.coolingTowerCapacity(100, 5, 318.15D, 100, PARAMETERS);
        assertTrue(normal > hot);
        assertEquals(0L, NuclearThermalTransactions.coolingTowerCapacity(100, 0, 298.15D, 100, PARAMETERS));
    }
}
