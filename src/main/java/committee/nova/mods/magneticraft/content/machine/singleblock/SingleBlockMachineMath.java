package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.system.network.heat.HeatNode;

/**
 * Loader-independent numerical contracts shared by machine runtime and datagen.
 */
public final class SingleBlockMachineMath {
    public static final double AMBIENT_TEMPERATURE_KELVIN = HeatNode.AMBIENT_TEMPERATURE_KELVIN;
    public static final double INTERNAL_ENGINE_FULL_POWER_TEMPERATURE_KELVIN = 423.15D;
    public static final double INTERNAL_ENGINE_CUTOFF_TEMPERATURE_KELVIN = 773.15D;

    private SingleBlockMachineMath() {
    }

    public static int thermalSpeed(double temperatureKelvin, double minimumTemperatureKelvin, int scale) {
        if (scale <= 0) {
            return 0;
        }
        double difference = Math.max(0.0D, Math.min(1.0D, temperatureKelvin - minimumTemperatureKelvin));
        return (int) Math.floor(difference * scale);
    }

    public static double fluidFuelEnergy(int durationTicks, double powerPerTick) {
        return Math.max(0, durationTicks) * Math.max(0.0D, powerPerTick);
    }

    public static double internalCombustionThrottle(double temperatureKelvin) {
        if (!Double.isFinite(temperatureKelvin)
                || temperatureKelvin >= INTERNAL_ENGINE_CUTOFF_TEMPERATURE_KELVIN) {
            return 0.0D;
        }
        if (temperatureKelvin <= INTERNAL_ENGINE_FULL_POWER_TEMPERATURE_KELVIN) {
            return 1.0D;
        }
        return (INTERNAL_ENGINE_CUTOFF_TEMPERATURE_KELVIN - temperatureKelvin)
                / (INTERNAL_ENGINE_CUTOFF_TEMPERATURE_KELVIN
                - INTERNAL_ENGINE_FULL_POWER_TEMPERATURE_KELVIN);
    }

    public static double balancedConductivity(double temperatureKelvin) {
        double difference = Math.max(
                0.000001D,
                Math.abs(AMBIENT_TEMPERATURE_KELVIN - temperatureKelvin)
        );
        return temperatureKelvin < AMBIENT_TEMPERATURE_KELVIN
                ? 2_000.0D / difference
                : 1_000.0D / difference;
    }
}
