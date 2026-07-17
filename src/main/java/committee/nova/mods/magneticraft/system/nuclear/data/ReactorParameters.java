package committee.nova.mods.magneticraft.system.nuclear.data;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Data-pack balance contract for PWR control, fuel evolution and bounded offline decay. */
public record ReactorParameters(
        ResourceLocation id,
        int stationInstrumentationJoulesPerTick,
        int stationRunningJoulesPerTick,
        int startupTicks,
        double minimumCoolantFraction,
        double fuelTemperatureResponsePerTick,
        double fuelTemperatureRiseAtFullPowerKelvin,
        double poisonBuildPerTick,
        double poisonDecayPerTick,
        double decayHeatResponsePerTick,
        double decayHeatLossPerTick,
        double claddingDamagePerKelvinTick,
        double forcedScramTemperatureKelvin,
        double safeUnloadTemperatureKelvin,
        double automaticRodStepPerTick,
        int maximumOfflineCatchupTicks
) {
    public static final int SCHEMA_VERSION = 1;
    public static final ResourceLocation PWR_ID = Magneticraft.id("pressurized_water_reactor");
    public static final ReactorParameters DEFAULT = new ReactorParameters(
            PWR_ID, 40, 80, 100, 0.85D, 0.003D, 900.0D,
            0.000020D, 0.000005D, 0.010D, 0.000015D, 0.000001D,
            1_550.0D, 373.15D, 0.005D, 72_000
    );

    public ReactorParameters {
        Objects.requireNonNull(id, "id");
        List<String> errors = validationErrors(
                stationInstrumentationJoulesPerTick, stationRunningJoulesPerTick, startupTicks,
                minimumCoolantFraction, fuelTemperatureResponsePerTick,
                fuelTemperatureRiseAtFullPowerKelvin, poisonBuildPerTick, poisonDecayPerTick,
                decayHeatResponsePerTick, decayHeatLossPerTick, claddingDamagePerKelvinTick,
                forcedScramTemperatureKelvin, safeUnloadTemperatureKelvin,
                automaticRodStepPerTick, maximumOfflineCatchupTicks
        );
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(id + ": " + String.join("; ", errors));
        }
    }

    public static List<String> validationErrors(
            int stationInstrumentationJoulesPerTick,
            int stationRunningJoulesPerTick,
            int startupTicks,
            double minimumCoolantFraction,
            double fuelTemperatureResponsePerTick,
            double fuelTemperatureRiseAtFullPowerKelvin,
            double poisonBuildPerTick,
            double poisonDecayPerTick,
            double decayHeatResponsePerTick,
            double decayHeatLossPerTick,
            double claddingDamagePerKelvinTick,
            double forcedScramTemperatureKelvin,
            double safeUnloadTemperatureKelvin,
            double automaticRodStepPerTick,
            int maximumOfflineCatchupTicks
    ) {
        ArrayList<String> errors = new ArrayList<>();
        positive("station_instrumentation_joules_per_tick", stationInstrumentationJoulesPerTick, errors);
        positive("station_running_joules_per_tick", stationRunningJoulesPerTick, errors);
        positive("startup_ticks", startupTicks, errors);
        fractionExclusive("minimum_coolant_fraction", minimumCoolantFraction, errors);
        fractionExclusive("fuel_temperature_response_per_tick", fuelTemperatureResponsePerTick, errors);
        positiveFinite("fuel_temperature_rise_at_full_power_kelvin", fuelTemperatureRiseAtFullPowerKelvin, errors);
        fractionExclusive("poison_build_per_tick", poisonBuildPerTick, errors);
        fractionExclusive("poison_decay_per_tick", poisonDecayPerTick, errors);
        fractionExclusive("decay_heat_response_per_tick", decayHeatResponsePerTick, errors);
        fractionExclusive("decay_heat_loss_per_tick", decayHeatLossPerTick, errors);
        fractionExclusive("cladding_damage_per_kelvin_tick", claddingDamagePerKelvinTick, errors);
        positiveFinite("forced_scram_temperature_kelvin", forcedScramTemperatureKelvin, errors);
        positiveFinite("safe_unload_temperature_kelvin", safeUnloadTemperatureKelvin, errors);
        if (Double.isFinite(forcedScramTemperatureKelvin)
                && Double.isFinite(safeUnloadTemperatureKelvin)
                && forcedScramTemperatureKelvin <= safeUnloadTemperatureKelvin) {
            errors.add("forced_scram_temperature_kelvin must exceed safe_unload_temperature_kelvin");
        }
        fractionExclusive("automatic_rod_step_per_tick", automaticRodStepPerTick, errors);
        positive("maximum_offline_catchup_ticks", maximumOfflineCatchupTicks, errors);
        return List.copyOf(errors);
    }

    private static void positive(String field, int value, List<String> errors) {
        if (value <= 0) {
            errors.add(field + " must be positive");
        }
    }

    private static void positiveFinite(String field, double value, List<String> errors) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            errors.add(field + " must be finite and positive");
        }
    }

    private static void fractionExclusive(String field, double value, List<String> errors) {
        if (!Double.isFinite(value) || value <= 0.0D || value > 1.0D) {
            errors.add(field + " must be finite and in (0, 1]");
        }
    }
}
