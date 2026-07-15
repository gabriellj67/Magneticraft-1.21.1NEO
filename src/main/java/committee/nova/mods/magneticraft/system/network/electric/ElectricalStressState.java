package committee.nova.mods.magneticraft.system.network.electric;

/**
 * Loader-independent accumulated electrical stress. Current is expressed in amperes and
 * voltage in volts; both terms are dimensionless after division by their data-pack ratings.
 */
public final class ElectricalStressState {
    public static final double FAILURE_THRESHOLD = 1.0D;

    private double stress;

    public double stress() {
        return stress;
    }

    public void restore(double savedStress) {
        stress = finiteNonNegative(savedStress);
    }

    public void reset() {
        stress = 0.0D;
    }

    /**
     * Advances one tick and returns true only when this update crosses the failure threshold.
     * Damage-disabled/reload-grace ticks never add stress, while a healthy circuit may still cool.
     */
    public boolean update(
            double currentAmps,
            double ratedCurrentAmps,
            double voltage,
            double maximumVoltage,
            double thermalCapacity,
            double coolingPerTick,
            boolean accumulationEnabled
    ) {
        requirePositiveFinite("ratedCurrentAmps", ratedCurrentAmps);
        requirePositiveFinite("maximumVoltage", maximumVoltage);
        requirePositiveFinite("thermalCapacity", thermalCapacity);
        requirePositiveFinite("coolingPerTick", coolingPerTick);

        double safeCurrent = finiteNonNegative(currentAmps);
        double safeVoltage = finiteNonNegative(voltage);
        double currentRatio = safeCurrent / ratedCurrentAmps;
        double voltageRatio = safeVoltage / maximumVoltage;
        double currentStress = Math.max(0.0D, currentRatio * currentRatio - 1.0D) / thermalCapacity;
        double voltageStress = Math.max(0.0D, voltageRatio * voltageRatio - 1.0D) / 100.0D;
        boolean overloaded = currentStress > 0.0D || voltageStress > 0.0D;
        double before = stress;
        if (overloaded) {
            if (accumulationEnabled) {
                stress = finiteNonNegative(stress + currentStress + voltageStress);
                if (stress >= FAILURE_THRESHOLD - 1.0E-12D) {
                    stress = Math.max(stress, FAILURE_THRESHOLD);
                }
            }
        } else {
            stress = Math.max(0.0D, stress - coolingPerTick);
        }
        return before < FAILURE_THRESHOLD && stress >= FAILURE_THRESHOLD;
    }

    private static void requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
