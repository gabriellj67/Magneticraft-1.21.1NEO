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
        int maximumOfflineCatchupTicks,
        int primaryCoolantEnthalpyJoulesPerMilliBucket,
        int mainPumpMaximumFlowMilliBucketsPerTick,
        double mainPumpJoulesPerMilliBucket,
        int steamGeneratorSteamPerWaterMilliBucket,
        int turbineJoulesPerSteamMilliBucket,
        double turbineVentingEfficiency,
        int condenserSteamPerWaterMilliBucket,
        double condenserHeatJoulesPerSteamMilliBucket,
        int coolingTowerHeatJoulesPerFillBlockTick,
        int coolingTowerHeatJoulesPerFanTick,
        double primaryBoilingTemperatureKelvin,
        double fuelMeltingTemperatureKelvin,
        double claddingAccidentThreshold,
        double pressureAlarmMegapascals,
        double vesselDesignPressureMegapascals,
        double vesselBurstPressureMegapascals,
        double pressureRiseMegapascalsPerKelvinTick,
        double pressureReliefMegapascalsPerTick,
        double vesselDamagePerMegapascalTick,
        double containmentDamagePerMegajoule,
        double accidentTerrainDamageEnergyJoules,
        double freshFuelDoseRateMillisievertsPerHour,
        double spentFuelDoseRateMillisievertsPerHour,
        double hotCoolantDoseRateMillisievertsPerHour,
        double coriumDoseRateMillisievertsPerHour,
        double contaminationDoseRateMillisievertsPerHour,
        double spentFuelCoolingKelvinPerTick,
        double spentFuelSafeDecayHeatJoules
) {
    public static final int SCHEMA_VERSION = 1;
    public static final ResourceLocation PWR_ID = Magneticraft.id("pressurized_water_reactor");
    public static final ReactorParameters DEFAULT = new ReactorParameters(
            PWR_ID, 40, 80, 100, 0.85D, 0.003D, 900.0D,
            0.000020D, 0.000005D, 0.010D, 0.000015D, 0.000001D,
            1_550.0D, 373.15D, 0.005D, 72_000,
            100, 1_000, 0.5D, 10, 2, 0.8D, 10, 4.0D, 25, 500,
            620.0D, 2_800.0D, 0.35D, 16.0D, 17.5D, 24.0D,
            0.00002D, 0.003D, 0.00002D, 0.000002D, 50_000_000.0D,
            0.002D, 250.0D, 0.05D, 2_000.0D, 20.0D, 0.025D, 5.0D
    );

    public ReactorParameters {
        Objects.requireNonNull(id, "id");
        List<String> errors = validationErrors(
                stationInstrumentationJoulesPerTick, stationRunningJoulesPerTick, startupTicks,
                minimumCoolantFraction, fuelTemperatureResponsePerTick,
                fuelTemperatureRiseAtFullPowerKelvin, poisonBuildPerTick, poisonDecayPerTick,
                decayHeatResponsePerTick, decayHeatLossPerTick, claddingDamagePerKelvinTick,
                forcedScramTemperatureKelvin, safeUnloadTemperatureKelvin,
                automaticRodStepPerTick, maximumOfflineCatchupTicks,
                primaryCoolantEnthalpyJoulesPerMilliBucket,
                mainPumpMaximumFlowMilliBucketsPerTick, mainPumpJoulesPerMilliBucket,
                steamGeneratorSteamPerWaterMilliBucket, turbineJoulesPerSteamMilliBucket,
                turbineVentingEfficiency, condenserSteamPerWaterMilliBucket,
                condenserHeatJoulesPerSteamMilliBucket,
                coolingTowerHeatJoulesPerFillBlockTick, coolingTowerHeatJoulesPerFanTick,
                primaryBoilingTemperatureKelvin, fuelMeltingTemperatureKelvin,
                claddingAccidentThreshold, pressureAlarmMegapascals,
                vesselDesignPressureMegapascals, vesselBurstPressureMegapascals,
                pressureRiseMegapascalsPerKelvinTick, pressureReliefMegapascalsPerTick,
                vesselDamagePerMegapascalTick, containmentDamagePerMegajoule,
                accidentTerrainDamageEnergyJoules, freshFuelDoseRateMillisievertsPerHour,
                spentFuelDoseRateMillisievertsPerHour, hotCoolantDoseRateMillisievertsPerHour,
                coriumDoseRateMillisievertsPerHour, contaminationDoseRateMillisievertsPerHour,
                spentFuelCoolingKelvinPerTick, spentFuelSafeDecayHeatJoules
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
            int maximumOfflineCatchupTicks,
            int primaryCoolantEnthalpyJoulesPerMilliBucket,
            int mainPumpMaximumFlowMilliBucketsPerTick,
            double mainPumpJoulesPerMilliBucket,
            int steamGeneratorSteamPerWaterMilliBucket,
            int turbineJoulesPerSteamMilliBucket,
            double turbineVentingEfficiency,
            int condenserSteamPerWaterMilliBucket,
            double condenserHeatJoulesPerSteamMilliBucket,
            int coolingTowerHeatJoulesPerFillBlockTick,
            int coolingTowerHeatJoulesPerFanTick,
            double primaryBoilingTemperatureKelvin,
            double fuelMeltingTemperatureKelvin,
            double claddingAccidentThreshold,
            double pressureAlarmMegapascals,
            double vesselDesignPressureMegapascals,
            double vesselBurstPressureMegapascals,
            double pressureRiseMegapascalsPerKelvinTick,
            double pressureReliefMegapascalsPerTick,
            double vesselDamagePerMegapascalTick,
            double containmentDamagePerMegajoule,
            double accidentTerrainDamageEnergyJoules,
            double freshFuelDoseRateMillisievertsPerHour,
            double spentFuelDoseRateMillisievertsPerHour,
            double hotCoolantDoseRateMillisievertsPerHour,
            double coriumDoseRateMillisievertsPerHour,
            double contaminationDoseRateMillisievertsPerHour,
            double spentFuelCoolingKelvinPerTick,
            double spentFuelSafeDecayHeatJoules
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
        positive("primary_coolant_enthalpy_joules_per_millibucket", primaryCoolantEnthalpyJoulesPerMilliBucket, errors);
        positive("main_pump_maximum_flow_millibuckets_per_tick", mainPumpMaximumFlowMilliBucketsPerTick, errors);
        positiveFinite("main_pump_joules_per_millibucket", mainPumpJoulesPerMilliBucket, errors);
        positive("steam_generator_steam_per_water_millibucket", steamGeneratorSteamPerWaterMilliBucket, errors);
        positive("turbine_joules_per_steam_millibucket", turbineJoulesPerSteamMilliBucket, errors);
        fractionExclusive("turbine_venting_efficiency", turbineVentingEfficiency, errors);
        positive("condenser_steam_per_water_millibucket", condenserSteamPerWaterMilliBucket, errors);
        positiveFinite("condenser_heat_joules_per_steam_millibucket", condenserHeatJoulesPerSteamMilliBucket, errors);
        positive("cooling_tower_heat_joules_per_fill_block_tick", coolingTowerHeatJoulesPerFillBlockTick, errors);
        positive("cooling_tower_heat_joules_per_fan_tick", coolingTowerHeatJoulesPerFanTick, errors);
        positiveFinite("primary_boiling_temperature_kelvin", primaryBoilingTemperatureKelvin, errors);
        positiveFinite("fuel_melting_temperature_kelvin", fuelMeltingTemperatureKelvin, errors);
        fractionExclusive("cladding_accident_threshold", claddingAccidentThreshold, errors);
        positiveFinite("pressure_alarm_megapascals", pressureAlarmMegapascals, errors);
        positiveFinite("vessel_design_pressure_megapascals", vesselDesignPressureMegapascals, errors);
        positiveFinite("vessel_burst_pressure_megapascals", vesselBurstPressureMegapascals, errors);
        if (fuelMeltingTemperatureKelvin <= primaryBoilingTemperatureKelvin) {
            errors.add("fuel_melting_temperature_kelvin must exceed primary_boiling_temperature_kelvin");
        }
        if (vesselDesignPressureMegapascals <= pressureAlarmMegapascals) {
            errors.add("vessel_design_pressure_megapascals must exceed pressure_alarm_megapascals");
        }
        if (vesselBurstPressureMegapascals <= vesselDesignPressureMegapascals) {
            errors.add("vessel_burst_pressure_megapascals must exceed vessel_design_pressure_megapascals");
        }
        positiveFinite("pressure_rise_megapascals_per_kelvin_tick", pressureRiseMegapascalsPerKelvinTick, errors);
        positiveFinite("pressure_relief_megapascals_per_tick", pressureReliefMegapascalsPerTick, errors);
        positiveFinite("vessel_damage_per_megapascal_tick", vesselDamagePerMegapascalTick, errors);
        positiveFinite("containment_damage_per_megajoule", containmentDamagePerMegajoule, errors);
        positiveFinite("accident_terrain_damage_energy_joules", accidentTerrainDamageEnergyJoules, errors);
        positiveFinite("fresh_fuel_dose_rate_millisieverts_per_hour", freshFuelDoseRateMillisievertsPerHour, errors);
        positiveFinite("spent_fuel_dose_rate_millisieverts_per_hour", spentFuelDoseRateMillisievertsPerHour, errors);
        positiveFinite("hot_coolant_dose_rate_millisieverts_per_hour", hotCoolantDoseRateMillisievertsPerHour, errors);
        positiveFinite("corium_dose_rate_millisieverts_per_hour", coriumDoseRateMillisievertsPerHour, errors);
        positiveFinite("contamination_dose_rate_millisieverts_per_hour", contaminationDoseRateMillisievertsPerHour, errors);
        positiveFinite("spent_fuel_cooling_kelvin_per_tick", spentFuelCoolingKelvinPerTick, errors);
        positiveFinite("spent_fuel_safe_decay_heat_joules", spentFuelSafeDecayHeatJoules, errors);
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
