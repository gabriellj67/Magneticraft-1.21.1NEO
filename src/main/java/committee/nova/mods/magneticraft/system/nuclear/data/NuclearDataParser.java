package committee.nova.mods.magneticraft.system.nuclear.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict loader-independent parser that rejects bad fuel entries without hiding good entries. */
public final class NuclearDataParser {
    private NuclearDataParser() {
    }

    public static NuclearDataLoadResult parse(Map<ResourceLocation, JsonElement> fuelJson) {
        ArrayList<NuclearDataValidationError> errors = new ArrayList<>();
        LinkedHashMap<ResourceLocation, NuclearFuelDefinition> fuels = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> rejected = new LinkedHashSet<>();
        if (fuelJson.size() > NuclearFuelDefinition.MAX_DEFINITIONS) {
            ResourceLocation marker = ResourceLocation.fromNamespaceAndPath("magneticraft", "nuclear_fuels");
            errors.add(new NuclearDataValidationError(
                    marker,
                    "registry exceeds " + NuclearFuelDefinition.MAX_DEFINITIONS + " entries"
            ));
        }

        fuelJson.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    NuclearFuelDefinition parsed = parseFuel(entry.getKey(), entry.getValue(), errors);
                    if (parsed == null) {
                        rejected.add(entry.getKey());
                    } else {
                        fuels.put(entry.getKey(), parsed);
                    }
                });
        return new NuclearDataLoadResult(fuels, errors, rejected);
    }

    private static NuclearFuelDefinition parseFuel(
            ResourceLocation id,
            JsonElement element,
            List<NuclearDataValidationError> errors
    ) {
        if (element == null || !element.isJsonObject()) {
            errors.add(error(id, "root must be a JSON object"));
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        int before = errors.size();
        int schema = integer(id, object, "schema_version", errors);
        double enrichment = decimal(id, object, "enrichment_percent", errors);
        double thermalPower = decimal(id, object, "thermal_power_joules_per_tick", errors);
        int designLife = integer(id, object, "design_life_ticks", errors);
        double reactivity = decimal(id, object, "initial_reactivity", errors);
        double temperatureCoefficient = decimal(id, object, "temperature_coefficient_per_kelvin", errors);
        double voidCoefficient = decimal(id, object, "void_coefficient", errors);
        double decayHeat = decimal(id, object, "decay_heat_fraction", errors);
        double claddingFailure = decimal(id, object, "cladding_failure_temperature_kelvin", errors);
        double loadFollow = decimal(id, object, "load_follow_rate_per_tick", errors);
        if (schema != NuclearFuelDefinition.SCHEMA_VERSION) {
            errors.add(error(id, "schema_version must be " + NuclearFuelDefinition.SCHEMA_VERSION));
        }
        NuclearFuelDefinition.validationErrors(
                enrichment,
                thermalPower,
                designLife,
                reactivity,
                temperatureCoefficient,
                voidCoefficient,
                decayHeat,
                claddingFailure,
                loadFollow
        ).forEach(message -> errors.add(error(id, message)));
        if (errors.size() != before) {
            return null;
        }
        return new NuclearFuelDefinition(
                id,
                enrichment,
                thermalPower,
                designLife,
                reactivity,
                temperatureCoefficient,
                voidCoefficient,
                decayHeat,
                claddingFailure,
                loadFollow
        );
    }

    private static int integer(
            ResourceLocation id,
            JsonObject object,
            String field,
            List<NuclearDataValidationError> errors
    ) {
        JsonPrimitive value = number(id, object, field, errors);
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            errors.add(error(id, field + " must be an exact 32-bit integer"));
            return Integer.MIN_VALUE;
        }
    }

    private static double decimal(
            ResourceLocation id,
            JsonObject object,
            String field,
            List<NuclearDataValidationError> errors
    ) {
        JsonPrimitive value = number(id, object, field, errors);
        if (value == null) {
            return Double.NaN;
        }
        try {
            return value.getAsDouble();
        } catch (NumberFormatException exception) {
            errors.add(error(id, field + " must be a number"));
            return Double.NaN;
        }
    }

    private static JsonPrimitive number(
            ResourceLocation id,
            JsonObject object,
            String field,
            List<NuclearDataValidationError> errors
    ) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            errors.add(error(id, field + " must be a JSON number"));
            return null;
        }
        return value.getAsJsonPrimitive();
    }

    private static NuclearDataValidationError error(ResourceLocation id, String message) {
        return new NuclearDataValidationError(id, message);
    }
}
