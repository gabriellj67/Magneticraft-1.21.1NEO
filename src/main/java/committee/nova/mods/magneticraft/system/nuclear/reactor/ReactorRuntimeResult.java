package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;

import java.util.Map;

/** One deterministic runtime step, including every durable per-column fuel state. */
public record ReactorRuntimeResult(
        Map<ReactorColumnCoordinate, FuelAssemblyState> fuelStates,
        double fissionPowerJoulesPerTick,
        double decayHeatJoulesPerTick,
        double totalThermalPowerJoulesPerTick,
        double averageTemperatureKelvin,
        double hottestTemperatureKelvin,
        double minimumCladdingIntegrity,
        double averageBurnupFraction,
        double averagePoisonFraction
) {
    public ReactorRuntimeResult {
        fuelStates = Map.copyOf(fuelStates);
    }

    public static ReactorRuntimeResult empty(Map<ReactorColumnCoordinate, FuelAssemblyState> states) {
        return new ReactorRuntimeResult(states, 0.0D, 0.0D, 0.0D,
                FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN,
                FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN, 1.0D, 0.0D, 0.0D);
    }
}
