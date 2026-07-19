package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReactorOperatorMetricsTest {
    @Test
    void derivesReadableMetricsWithoutReplacingTheEngineeringModel() {
        NuclearReactorColumnType fuel = NuclearReactorColumnType.FUEL_STANDARD;
        double baseFuelEnergy = fuel.fuelGrade().thermalPowerJoulesPerTick()
                * fuel.fuelGrade().designLifeTicks();
        ReactorLayoutEstimate estimate = new ReactorLayoutEstimate(
                Map.of(), 100.0D, baseFuelEnergy * 1.1D,
                80.0D, 75.0D, 60.0D, 40.0D, List.of()
        );
        List<NuclearReactorColumnType> columns = List.of(
                fuel,
                NuclearReactorColumnType.COOLANT_CHANNEL,
                NuclearReactorColumnType.CONTROL_ROD_A,
                NuclearReactorColumnType.INSTRUMENTATION
        );

        ReactorOperatorMetrics normal = ReactorOperatorMetrics.calculate(
                estimate, 200.0D, columns, 0.9D, 0.8D, false
        );
        assertEquals(50.0D, normal.outputPercent(), 1.0E-9D);
        assertEquals(110.0D, normal.efficiencyPercent(), 1.0E-9D);
        assertEquals(80.0D, normal.safetyPercent(), 1.0E-9D);
        assertEquals(75.0D, normal.responsePercent(), 1.0E-9D);

        ReactorOperatorMetrics interlocked = ReactorOperatorMetrics.calculate(
                estimate, 200.0D, columns, 0.9D, 0.8D, true
        );
        assertEquals(49.0D, interlocked.safetyPercent(), 1.0E-9D);
    }
}
