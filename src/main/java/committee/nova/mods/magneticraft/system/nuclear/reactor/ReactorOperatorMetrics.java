package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;

import java.util.Collection;

/** Compact operator summary derived from the same detailed layout and runtime state. */
public record ReactorOperatorMetrics(
        double outputPercent,
        double efficiencyPercent,
        double safetyPercent,
        double responsePercent
) {
    public static ReactorOperatorMetrics calculate(
            ReactorLayoutEstimate estimate,
            double currentThermalPowerJoulesPerTick,
            Collection<NuclearReactorColumnType> columns,
            double vesselIntegrity,
            double containmentIntegrity,
            boolean interlocked
    ) {
        int activeCells = Math.max(1, columns.size());
        double designPower = estimate.powerDensityJoulesPerTick() * activeCells;
        double output = designPower <= 0.0D
                ? 0.0D : currentThermalPowerJoulesPerTick / designPower * 100.0D;

        double baseFuelEnergy = columns.stream()
                .filter(NuclearReactorColumnType::isFuel)
                .map(NuclearReactorColumnType::fuelGrade)
                .mapToDouble(grade -> grade.thermalPowerJoulesPerTick() * grade.designLifeTicks())
                .average().orElse(0.0D);
        double efficiency = baseFuelEnergy <= 0.0D
                ? 0.0D : estimate.fuelEnergyJoulesPerColumn() / baseFuelEnergy * 100.0D;

        double safety = Math.min(estimate.safetyMarginPercent(),
                Math.min(fractionPercent(vesselIntegrity), fractionPercent(containmentIntegrity)));
        if (interlocked) {
            safety = Math.min(safety, 49.0D);
        }
        return new ReactorOperatorMetrics(
                clamp(output, 0.0D, 150.0D),
                clamp(efficiency, 0.0D, 135.0D),
                clamp(safety, 0.0D, 100.0D),
                clamp(estimate.loadFollowingPercent(), 0.0D, 100.0D)
        );
    }

    private static double fractionPercent(double value) {
        return Double.isFinite(value) ? clamp(value, 0.0D, 1.0D) * 100.0D : 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }
}
