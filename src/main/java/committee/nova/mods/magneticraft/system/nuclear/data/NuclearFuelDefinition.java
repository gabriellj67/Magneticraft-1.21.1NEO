package committee.nova.mods.magneticraft.system.nuclear.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable, data-pack-defined physical parameters for one LEU fuel assembly. */
public record NuclearFuelDefinition(
        ResourceLocation id,
        double enrichmentPercent,
        double thermalPowerJoulesPerTick,
        int designLifeTicks,
        double initialReactivity,
        double temperatureCoefficientPerKelvin,
        double voidCoefficient,
        double decayHeatFraction,
        double claddingFailureTemperatureKelvin,
        double loadFollowRatePerTick
) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_DEFINITIONS = 256;

    public NuclearFuelDefinition {
        Objects.requireNonNull(id, "id");
        List<String> errors = validationErrors(
                enrichmentPercent,
                thermalPowerJoulesPerTick,
                designLifeTicks,
                initialReactivity,
                temperatureCoefficientPerKelvin,
                voidCoefficient,
                decayHeatFraction,
                claddingFailureTemperatureKelvin,
                loadFollowRatePerTick
        );
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(id + ": " + String.join("; ", errors));
        }
    }

    public double totalEnergyJoules() {
        return thermalPowerJoulesPerTick * designLifeTicks;
    }

    public static List<String> validationErrors(
            double enrichmentPercent,
            double thermalPowerJoulesPerTick,
            int designLifeTicks,
            double initialReactivity,
            double temperatureCoefficientPerKelvin,
            double voidCoefficient,
            double decayHeatFraction,
            double claddingFailureTemperatureKelvin,
            double loadFollowRatePerTick
    ) {
        ArrayList<String> errors = new ArrayList<>();
        if (!Double.isFinite(enrichmentPercent) || enrichmentPercent <= 0.0D || enrichmentPercent >= 20.0D) {
            errors.add("enrichment_percent must be finite and in (0, 20)");
        }
        positiveFinite("thermal_power_joules_per_tick", thermalPowerJoulesPerTick, errors);
        if (designLifeTicks <= 0) {
            errors.add("design_life_ticks must be positive");
        }
        positiveFinite("initial_reactivity", initialReactivity, errors);
        if (!Double.isFinite(temperatureCoefficientPerKelvin) || temperatureCoefficientPerKelvin >= 0.0D) {
            errors.add("temperature_coefficient_per_kelvin must be finite and negative");
        }
        if (!Double.isFinite(voidCoefficient) || voidCoefficient >= 0.0D) {
            errors.add("void_coefficient must be finite and negative");
        }
        if (!Double.isFinite(decayHeatFraction) || decayHeatFraction <= 0.0D || decayHeatFraction >= 0.25D) {
            errors.add("decay_heat_fraction must be finite and in (0, 0.25)");
        }
        if (!Double.isFinite(claddingFailureTemperatureKelvin)
                || claddingFailureTemperatureKelvin <= 373.15D) {
            errors.add("cladding_failure_temperature_kelvin must be finite and above boiling water");
        }
        if (!Double.isFinite(loadFollowRatePerTick)
                || loadFollowRatePerTick <= 0.0D
                || loadFollowRatePerTick > 1.0D) {
            errors.add("load_follow_rate_per_tick must be finite and in (0, 1]");
        }
        return List.copyOf(errors);
    }

    private static void positiveFinite(String field, double value, List<String> errors) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            errors.add(field + " must be finite and positive");
        }
    }
}
