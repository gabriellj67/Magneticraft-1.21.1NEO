package committee.nova.mods.magneticraft.system.network.electric.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataValidationError.RegistryKind.MACHINE_PROFILE;
import static committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataValidationError.RegistryKind.TRANSFORMER_PROFILE;
import static committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataValidationError.RegistryKind.VOLTAGE_TIER;

/**
 * Strict, loader-independent decoder for the three electrical data registries.
 */
public final class ElectricalDataParser {
    public static final int MAX_TRANSFORMER_PROFILES = 1_024;
    public static final int MAX_MACHINE_PROFILES = 4_096;
    private static final ResourceLocation VOLTAGE_REGISTRY = marker("voltage_tiers");
    private static final ResourceLocation TRANSFORMER_REGISTRY = marker("transformer_profiles");
    private static final ResourceLocation MACHINE_REGISTRY = marker("machine_electrical_profiles");

    private ElectricalDataParser() {
    }

    public static ElectricalDataLoadResult parse(
            Map<ResourceLocation, JsonElement> voltageTierJson,
            Map<ResourceLocation, JsonElement> transformerProfileJson,
            Map<ResourceLocation, JsonElement> machineProfileJson
    ) {
        return parse(voltageTierJson, transformerProfileJson, machineProfileJson, List.of());
    }

    static ElectricalDataLoadResult parse(
            Map<ResourceLocation, JsonElement> voltageTierJson,
            Map<ResourceLocation, JsonElement> transformerProfileJson,
            Map<ResourceLocation, JsonElement> machineProfileJson,
            List<ElectricalDataValidationError> seedErrors
    ) {
        ArrayList<ElectricalDataValidationError> errors = new ArrayList<>(seedErrors);
        LinkedHashMap<ResourceLocation, VoltageTier> tiers = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, TransformerProfile> transformers = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, MachineElectricalProfile> machines = new LinkedHashMap<>();

        validateRegistrySize(voltageTierJson, VoltageTier.MAX_SYNCED_TIERS, VOLTAGE_TIER, VOLTAGE_REGISTRY, errors);
        validateRegistrySize(
                transformerProfileJson,
                MAX_TRANSFORMER_PROFILES,
                TRANSFORMER_PROFILE,
                TRANSFORMER_REGISTRY,
                errors
        );
        validateRegistrySize(machineProfileJson, MAX_MACHINE_PROFILES, MACHINE_PROFILE, MACHINE_REGISTRY, errors);

        sorted(voltageTierJson).forEach(entry -> {
            VoltageTier tier = parseTier(entry.getKey(), entry.getValue(), errors);
            if (tier != null) {
                tiers.put(entry.getKey(), tier);
            }
        });
        sorted(transformerProfileJson).forEach(entry -> {
            TransformerProfile profile = parseTransformer(entry.getKey(), entry.getValue(), errors);
            if (profile != null) {
                transformers.put(entry.getKey(), profile);
            }
        });
        sorted(machineProfileJson).forEach(entry -> {
            MachineElectricalProfile profile = parseMachine(entry.getKey(), entry.getValue(), errors);
            if (profile != null) {
                machines.put(entry.getKey(), profile);
            }
        });

        if (voltageTierJson.isEmpty()) {
            errors.add(error(VOLTAGE_TIER, VOLTAGE_REGISTRY, "registry must not be empty"));
        }
        if (transformerProfileJson.isEmpty()) {
            errors.add(error(TRANSFORMER_PROFILE, TRANSFORMER_REGISTRY, "registry must not be empty"));
        }
        if (machineProfileJson.isEmpty()) {
            errors.add(error(MACHINE_PROFILE, MACHINE_REGISTRY, "registry must not be empty"));
        }

        transformers.values().forEach(profile -> {
            if (!tiers.containsKey(profile.inputTierId())) {
                errors.add(error(
                        TRANSFORMER_PROFILE,
                        profile.id(),
                        "missing input_tier reference " + profile.inputTierId()
                ));
            }
            if (!tiers.containsKey(profile.outputTierId())) {
                errors.add(error(
                        TRANSFORMER_PROFILE,
                        profile.id(),
                        "missing output_tier reference " + profile.outputTierId()
                ));
            }
        });
        machines.values().forEach(profile -> {
            if (!tiers.containsKey(profile.tierId())) {
                errors.add(error(MACHINE_PROFILE, profile.id(), "missing tier reference " + profile.tierId()));
            }
        });

        if (!errors.isEmpty()) {
            return new ElectricalDataLoadResult(Optional.empty(), errors);
        }
        return new ElectricalDataLoadResult(
                Optional.of(new ElectricalDataSnapshot(0L, tiers, transformers, machines)),
                List.of()
        );
    }

    private static VoltageTier parseTier(
            ResourceLocation id,
            JsonElement element,
            List<ElectricalDataValidationError> errors
    ) {
        JsonObject object = object(VOLTAGE_TIER, id, element, errors);
        if (object == null) {
            return null;
        }
        int before = errors.size();
        int schema = integer(VOLTAGE_TIER, id, object, "schema_version", errors);
        String translationKey = string(VOLTAGE_TIER, id, object, "translation_key", errors);
        double minimum = decimal(VOLTAGE_TIER, id, object, "minimum_operating_voltage", errors);
        double nominal = decimal(VOLTAGE_TIER, id, object, "nominal_voltage", errors);
        double maximum = decimal(VOLTAGE_TIER, id, object, "maximum_voltage", errors);
        double generator = decimal(VOLTAGE_TIER, id, object, "generator_voltage", errors);
        double machineCapacitance = decimal(VOLTAGE_TIER, id, object, "machine_capacitance_farads", errors);
        double conductorCapacitance = decimal(VOLTAGE_TIER, id, object, "conductor_capacitance_farads", errors);
        double resistance = decimal(VOLTAGE_TIER, id, object, "node_resistance_ohms", errors);
        double cableRating = decimal(VOLTAGE_TIER, id, object, "cable_rated_charge_per_tick", errors);
        double overheadRating = decimal(VOLTAGE_TIER, id, object, "overhead_rated_charge_per_tick", errors);
        double standardProtection = decimal(
                VOLTAGE_TIER,
                id,
                object,
                "standard_protection_charge_per_tick",
                errors
        );
        double heavyProtection = decimal(
                VOLTAGE_TIER,
                id,
                object,
                "heavy_protection_charge_per_tick",
                errors
        );
        double cableThermal = decimal(VOLTAGE_TIER, id, object, "cable_thermal_capacity", errors);
        double cableCooling = decimal(VOLTAGE_TIER, id, object, "cable_cooling_per_tick", errors);
        double overheadThermal = decimal(VOLTAGE_TIER, id, object, "overhead_thermal_capacity", errors);
        double overheadCooling = decimal(VOLTAGE_TIER, id, object, "overhead_cooling_per_tick", errors);
        double connectorConversion = optionalDecimal(
                VOLTAGE_TIER,
                id,
                object,
                "connector_conversion_joules_per_tick",
                VoltageTier.DEFAULT_CONNECTOR_CONVERSION_JOULES_PER_TICK,
                errors
        );
        int connectorRange = integer(VOLTAGE_TIER, id, object, "connector_range", errors);
        int poleRange = integer(VOLTAGE_TIER, id, object, "pole_range", errors);
        long batteryCapacity = longInteger(VOLTAGE_TIER, id, object, "battery_capacity_joules", errors);
        double batteryTransfer = decimal(VOLTAGE_TIER, id, object, "battery_transfer_joules_per_tick", errors);
        int color = color(VOLTAGE_TIER, id, object, "color", errors);

        if (schema != VoltageTier.SCHEMA_VERSION) {
            errors.add(error(VOLTAGE_TIER, id, "schema_version must be " + VoltageTier.SCHEMA_VERSION));
        }
        for (String validationError : VoltageTier.validationErrors(
                translationKey,
                minimum,
                nominal,
                maximum,
                generator,
                machineCapacitance,
                conductorCapacitance,
                resistance,
                cableRating,
                overheadRating,
                standardProtection,
                heavyProtection,
                cableThermal,
                cableCooling,
                overheadThermal,
                overheadCooling,
                connectorConversion,
                connectorRange,
                poleRange,
                batteryCapacity,
                batteryTransfer,
                color
        )) {
            errors.add(error(VOLTAGE_TIER, id, validationError));
        }
        if (errors.size() != before) {
            return null;
        }
        return new VoltageTier(
                id,
                translationKey,
                minimum,
                nominal,
                maximum,
                generator,
                machineCapacitance,
                conductorCapacitance,
                resistance,
                cableRating,
                overheadRating,
                standardProtection,
                heavyProtection,
                cableThermal,
                cableCooling,
                overheadThermal,
                overheadCooling,
                connectorConversion,
                connectorRange,
                poleRange,
                batteryCapacity,
                batteryTransfer,
                color
        );
    }

    private static TransformerProfile parseTransformer(
            ResourceLocation id,
            JsonElement element,
            List<ElectricalDataValidationError> errors
    ) {
        JsonObject object = object(TRANSFORMER_PROFILE, id, element, errors);
        if (object == null) {
            return null;
        }
        int before = errors.size();
        int schema = integer(TRANSFORMER_PROFILE, id, object, "schema_version", errors);
        ResourceLocation input = resourceLocation(TRANSFORMER_PROFILE, id, object, "input_tier", errors);
        ResourceLocation output = resourceLocation(TRANSFORMER_PROFILE, id, object, "output_tier", errors);
        double maximumTransfer = decimal(
                TRANSFORMER_PROFILE,
                id,
                object,
                "maximum_transfer_joules_per_tick",
                errors
        );
        double efficiency = decimal(TRANSFORMER_PROFILE, id, object, "efficiency", errors);
        if (schema != TransformerProfile.SCHEMA_VERSION) {
            errors.add(error(
                    TRANSFORMER_PROFILE,
                    id,
                    "schema_version must be " + TransformerProfile.SCHEMA_VERSION
            ));
        }
        if (input != null && input.equals(output)) {
            errors.add(error(TRANSFORMER_PROFILE, id, "input_tier and output_tier must differ"));
        }
        positiveFinite(TRANSFORMER_PROFILE, id, "maximum_transfer_joules_per_tick", maximumTransfer, errors);
        if (!Double.isFinite(efficiency) || efficiency <= 0.0D || efficiency > 1.0D) {
            errors.add(error(TRANSFORMER_PROFILE, id, "efficiency must be finite and in (0, 1]"));
        }
        if (errors.size() != before) {
            return null;
        }
        return new TransformerProfile(id, input, output, maximumTransfer, efficiency);
    }

    private static MachineElectricalProfile parseMachine(
            ResourceLocation id,
            JsonElement element,
            List<ElectricalDataValidationError> errors
    ) {
        JsonObject object = object(MACHINE_PROFILE, id, element, errors);
        if (object == null) {
            return null;
        }
        int before = errors.size();
        int schema = integer(MACHINE_PROFILE, id, object, "schema_version", errors);
        ResourceLocation tier = resourceLocation(MACHINE_PROFILE, id, object, "tier", errors);
        String roleName = string(MACHINE_PROFILE, id, object, "role", errors);
        ElectricalRole role = null;
        if (roleName != null) {
            try {
                role = ElectricalRole.valueOf(roleName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                errors.add(error(MACHINE_PROFILE, id, "unknown role " + roleName));
            }
        }
        double buffer = decimal(MACHINE_PROFILE, id, object, "buffer_capacity_joules", errors);
        double maximumTransfer = decimal(
                MACHINE_PROFILE,
                id,
                object,
                "maximum_transfer_joules_per_tick",
                errors
        );
        double terminalRating = decimal(
                MACHINE_PROFILE,
                id,
                object,
                "terminal_rated_charge_per_tick",
                errors
        );
        if (schema != MachineElectricalProfile.SCHEMA_VERSION) {
            errors.add(error(
                    MACHINE_PROFILE,
                    id,
                    "schema_version must be " + MachineElectricalProfile.SCHEMA_VERSION
            ));
        }
        positiveFinite(MACHINE_PROFILE, id, "buffer_capacity_joules", buffer, errors);
        positiveFinite(MACHINE_PROFILE, id, "maximum_transfer_joules_per_tick", maximumTransfer, errors);
        positiveFinite(MACHINE_PROFILE, id, "terminal_rated_charge_per_tick", terminalRating, errors);
        if (errors.size() != before) {
            return null;
        }
        return new MachineElectricalProfile(id, tier, role, buffer, maximumTransfer, terminalRating);
    }

    private static JsonObject object(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonElement element,
            List<ElectricalDataValidationError> errors
    ) {
        if (element == null || !element.isJsonObject()) {
            errors.add(error(kind, id, "root must be a JSON object"));
            return null;
        }
        return element.getAsJsonObject();
    }

    private static int integer(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        JsonPrimitive primitive = number(kind, id, object, field, errors);
        if (primitive == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return primitive.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            errors.add(error(kind, id, field + " must be an exact 32-bit integer"));
            return Integer.MIN_VALUE;
        }
    }

    private static long longInteger(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        JsonPrimitive primitive = number(kind, id, object, field, errors);
        if (primitive == null) {
            return Long.MIN_VALUE;
        }
        try {
            return primitive.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            errors.add(error(kind, id, field + " must be an exact 64-bit integer"));
            return Long.MIN_VALUE;
        }
    }

    private static double decimal(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        JsonPrimitive primitive = number(kind, id, object, field, errors);
        if (primitive == null) {
            return Double.NaN;
        }
        try {
            return primitive.getAsDouble();
        } catch (NumberFormatException exception) {
            errors.add(error(kind, id, field + " must be a number"));
            return Double.NaN;
        }
    }

    private static double optionalDecimal(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            double fallback,
            List<ElectricalDataValidationError> errors
    ) {
        return object.has(field) ? decimal(kind, id, object, field, errors) : fallback;
    }

    private static JsonPrimitive number(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            errors.add(error(kind, id, field + " must be a JSON number"));
            return null;
        }
        return value.getAsJsonPrimitive();
    }

    private static String string(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            errors.add(error(kind, id, field + " must be a JSON string"));
            return null;
        }
        String result = value.getAsString();
        if (result.isBlank() || result.length() > VoltageTier.MAX_TEXT_LENGTH) {
            errors.add(error(
                    kind,
                    id,
                    field + " must contain 1-" + VoltageTier.MAX_TEXT_LENGTH + " characters"
            ));
        }
        return result;
    }

    private static ResourceLocation resourceLocation(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        String value = string(kind, id, object, field, errors);
        if (value == null) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            errors.add(error(kind, id, field + " must be a valid ResourceLocation"));
        }
        return parsed;
    }

    private static int color(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            JsonObject object,
            String field,
            List<ElectricalDataValidationError> errors
    ) {
        String value = string(kind, id, object, field, errors);
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) {
            if (value != null) {
                errors.add(error(kind, id, field + " must use #RRGGBB format"));
            }
            return -1;
        }
        return Integer.parseInt(value.substring(1), 16);
    }

    private static void positiveFinite(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            String field,
            double value,
            List<ElectricalDataValidationError> errors
    ) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            errors.add(error(kind, id, field + " must be finite and positive"));
        }
    }

    private static void validateRegistrySize(
            Map<ResourceLocation, JsonElement> values,
            int maximum,
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation marker,
            List<ElectricalDataValidationError> errors
    ) {
        if (values.size() > maximum) {
            errors.add(error(kind, marker, "registry contains " + values.size() + " entries; maximum is " + maximum));
        }
    }

    private static List<Map.Entry<ResourceLocation, JsonElement>> sorted(
            Map<ResourceLocation, JsonElement> values
    ) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList();
    }

    private static ElectricalDataValidationError error(
            ElectricalDataValidationError.RegistryKind kind,
            ResourceLocation id,
            String message
    ) {
        return new ElectricalDataValidationError(kind, id, message);
    }

    private static ResourceLocation marker(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", "__registry__/" + path);
    }
}
