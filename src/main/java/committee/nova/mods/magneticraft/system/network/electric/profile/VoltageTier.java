package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Complete immutable electrical contract for one voltage tier.
 */
public record VoltageTier(
        ResourceLocation id,
        String translationKey,
        double minimumOperatingVoltage,
        double nominalVoltage,
        double maximumVoltage,
        double generatorVoltage,
        double machineCapacitanceFarads,
        double conductorCapacitanceFarads,
        double nodeResistanceOhms,
        double cableRatedChargePerTick,
        double overheadRatedChargePerTick,
        double standardProtectionChargePerTick,
        double heavyProtectionChargePerTick,
        double cableThermalCapacity,
        double cableCoolingPerTick,
        double overheadThermalCapacity,
        double overheadCoolingPerTick,
        double connectorConversionJoulesPerTick,
        int connectorRange,
        int poleRange,
        long batteryCapacityJoules,
        double batteryTransferJoulesPerTick,
        int colorRgb
) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_SYNCED_TIERS = 256;
    public static final int MAX_TEXT_LENGTH = 128;
    public static final double DEFAULT_CONNECTOR_CONVERSION_JOULES_PER_TICK = 400.0D;
    private static final double VOLTAGE_COMPARISON_EPSILON = 1.0E-9D;

    public VoltageTier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        List<String> errors = validationErrors(
                translationKey,
                minimumOperatingVoltage,
                nominalVoltage,
                maximumVoltage,
                generatorVoltage,
                machineCapacitanceFarads,
                conductorCapacitanceFarads,
                nodeResistanceOhms,
                cableRatedChargePerTick,
                overheadRatedChargePerTick,
                standardProtectionChargePerTick,
                heavyProtectionChargePerTick,
                cableThermalCapacity,
                cableCoolingPerTick,
                overheadThermalCapacity,
                overheadCoolingPerTick,
                connectorConversionJoulesPerTick,
                connectorRange,
                poleRange,
                batteryCapacityJoules,
                batteryTransferJoulesPerTick,
                colorRgb
        );
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    public static List<String> validationErrors(
            String translationKey,
            double minimumOperatingVoltage,
            double nominalVoltage,
            double maximumVoltage,
            double generatorVoltage,
            double machineCapacitanceFarads,
            double conductorCapacitanceFarads,
            double nodeResistanceOhms,
            double cableRatedChargePerTick,
            double overheadRatedChargePerTick,
            double standardProtectionChargePerTick,
            double heavyProtectionChargePerTick,
            double cableThermalCapacity,
            double cableCoolingPerTick,
            double overheadThermalCapacity,
            double overheadCoolingPerTick,
            double connectorConversionJoulesPerTick,
            int connectorRange,
            int poleRange,
            long batteryCapacityJoules,
            double batteryTransferJoulesPerTick,
            int colorRgb
    ) {
        ArrayList<String> errors = new ArrayList<>();
        if (translationKey == null || translationKey.isBlank() || translationKey.length() > MAX_TEXT_LENGTH) {
            errors.add("translation_key must contain 1-" + MAX_TEXT_LENGTH + " characters");
        }
        positiveFinite(errors, "minimum_operating_voltage", minimumOperatingVoltage);
        positiveFinite(errors, "nominal_voltage", nominalVoltage);
        positiveFinite(errors, "maximum_voltage", maximumVoltage);
        positiveFinite(errors, "generator_voltage", generatorVoltage);
        positiveFinite(errors, "machine_capacitance_farads", machineCapacitanceFarads);
        positiveFinite(errors, "conductor_capacitance_farads", conductorCapacitanceFarads);
        positiveFinite(errors, "node_resistance_ohms", nodeResistanceOhms);
        positiveFinite(errors, "cable_rated_charge_per_tick", cableRatedChargePerTick);
        positiveFinite(errors, "overhead_rated_charge_per_tick", overheadRatedChargePerTick);
        positiveFinite(errors, "standard_protection_charge_per_tick", standardProtectionChargePerTick);
        positiveFinite(errors, "heavy_protection_charge_per_tick", heavyProtectionChargePerTick);
        positiveFinite(errors, "cable_thermal_capacity", cableThermalCapacity);
        positiveFinite(errors, "cable_cooling_per_tick", cableCoolingPerTick);
        positiveFinite(errors, "overhead_thermal_capacity", overheadThermalCapacity);
        positiveFinite(errors, "overhead_cooling_per_tick", overheadCoolingPerTick);
        positiveFinite(errors, "connector_conversion_joules_per_tick", connectorConversionJoulesPerTick);
        positiveFinite(errors, "battery_transfer_joules_per_tick", batteryTransferJoulesPerTick);
        if (Double.isFinite(minimumOperatingVoltage)
                && Double.isFinite(nominalVoltage)
                && minimumOperatingVoltage >= nominalVoltage) {
            errors.add("minimum_operating_voltage must be lower than nominal_voltage");
        }
        if (Double.isFinite(nominalVoltage)
                && Double.isFinite(maximumVoltage)
                && nominalVoltage >= maximumVoltage) {
            errors.add("nominal_voltage must be lower than maximum_voltage");
        }
        if (Double.isFinite(generatorVoltage)
                && Double.isFinite(nominalVoltage)
                && Double.isFinite(maximumVoltage)
                && (generatorVoltage < nominalVoltage || generatorVoltage > maximumVoltage)) {
            errors.add("generator_voltage must be between nominal_voltage and maximum_voltage");
        }
        if (connectorRange <= 0) {
            errors.add("connector_range must be positive");
        }
        if (poleRange <= 0) {
            errors.add("pole_range must be positive");
        }
        if (batteryCapacityJoules <= 0L) {
            errors.add("battery_capacity_joules must be positive");
        }
        if (colorRgb < 0 || colorRgb > 0xFF_FFFF) {
            errors.add("color must be a 24-bit RGB value");
        }
        return List.copyOf(errors);
    }

    /** Tolerant threshold check for voltages reconstructed from capacitor energy. */
    public boolean meetsMinimumOperatingVoltage(double voltage) {
        return Double.isFinite(voltage)
                && voltage + voltageTolerance() >= minimumOperatingVoltage;
    }

    /** Linear minimum-to-nominal rate fraction with exact nominal saturation. */
    public double operatingRateFraction(double voltage) {
        if (!meetsMinimumOperatingVoltage(voltage)) {
            return 0.0D;
        }
        if (voltage + voltageTolerance() >= nominalVoltage) {
            return 1.0D;
        }
        return Math.min(
                1.0D,
                Math.max(0.0D, (voltage - minimumOperatingVoltage)
                        / (nominalVoltage - minimumOperatingVoltage))
        );
    }

    private double voltageTolerance() {
        return VOLTAGE_COMPARISON_EPSILON * Math.max(1.0D, nominalVoltage);
    }

    private static void positiveFinite(List<String> errors, String field, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            errors.add(field + " must be finite and positive");
        }
    }
}
