package committee.nova.mods.magneticraft.system.nuclear.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Strict loader-independent parser for one reactor balance definition. */
public final class ReactorParameterParser {
    private ReactorParameterParser() {
    }

    public static Result parse(ResourceLocation id, JsonElement element) {
        ArrayList<String> errors = new ArrayList<>();
        if (element == null || !element.isJsonObject()) {
            return new Result(Optional.empty(), List.of("root must be a JSON object"));
        }
        JsonObject object = element.getAsJsonObject();
        int schema = integer(object, "schema_version", errors);
        int instrumentation = integer(object, "station_instrumentation_joules_per_tick", errors);
        int running = integer(object, "station_running_joules_per_tick", errors);
        int startup = integer(object, "startup_ticks", errors);
        double minimumCoolant = decimal(object, "minimum_coolant_fraction", errors);
        double temperatureResponse = decimal(object, "fuel_temperature_response_per_tick", errors);
        double temperatureRise = decimal(object, "fuel_temperature_rise_at_full_power_kelvin", errors);
        double poisonBuild = decimal(object, "poison_build_per_tick", errors);
        double poisonDecay = decimal(object, "poison_decay_per_tick", errors);
        double decayResponse = decimal(object, "decay_heat_response_per_tick", errors);
        double decayLoss = decimal(object, "decay_heat_loss_per_tick", errors);
        double claddingDamage = decimal(object, "cladding_damage_per_kelvin_tick", errors);
        double scramTemperature = decimal(object, "forced_scram_temperature_kelvin", errors);
        double unloadTemperature = decimal(object, "safe_unload_temperature_kelvin", errors);
        double rodStep = decimal(object, "automatic_rod_step_per_tick", errors);
        int offlineTicks = integer(object, "maximum_offline_catchup_ticks", errors);
        int coolantEnthalpy = integer(object, "primary_coolant_enthalpy_joules_per_millibucket", errors);
        int pumpFlow = integer(object, "main_pump_maximum_flow_millibuckets_per_tick", errors);
        double pumpEnergy = decimal(object, "main_pump_joules_per_millibucket", errors);
        int steamRatio = integer(object, "steam_generator_steam_per_water_millibucket", errors);
        int turbineEnergy = integer(object, "turbine_joules_per_steam_millibucket", errors);
        double ventingEfficiency = decimal(object, "turbine_venting_efficiency", errors);
        int condenserRatio = integer(object, "condenser_steam_per_water_millibucket", errors);
        double condenserHeat = decimal(object, "condenser_heat_joules_per_steam_millibucket", errors);
        int towerFillHeat = integer(object, "cooling_tower_heat_joules_per_fill_block_tick", errors);
        int towerFanHeat = integer(object, "cooling_tower_heat_joules_per_fan_tick", errors);
        if (schema != ReactorParameters.SCHEMA_VERSION) {
            errors.add("schema_version must be " + ReactorParameters.SCHEMA_VERSION);
        }
        errors.addAll(ReactorParameters.validationErrors(
                instrumentation, running, startup, minimumCoolant, temperatureResponse,
                temperatureRise, poisonBuild, poisonDecay, decayResponse, decayLoss,
                claddingDamage, scramTemperature, unloadTemperature, rodStep, offlineTicks,
                coolantEnthalpy, pumpFlow, pumpEnergy, steamRatio, turbineEnergy,
                ventingEfficiency, condenserRatio, condenserHeat, towerFillHeat, towerFanHeat));
        if (!errors.isEmpty()) {
            return new Result(Optional.empty(), errors);
        }
        return new Result(Optional.of(new ReactorParameters(
                id, instrumentation, running, startup, minimumCoolant, temperatureResponse,
                temperatureRise, poisonBuild, poisonDecay, decayResponse, decayLoss,
                claddingDamage, scramTemperature, unloadTemperature, rodStep, offlineTicks,
                coolantEnthalpy, pumpFlow, pumpEnergy, steamRatio, turbineEnergy,
                ventingEfficiency, condenserRatio, condenserHeat, towerFillHeat, towerFanHeat)), List.of());
    }

    private static int integer(JsonObject object, String field, List<String> errors) {
        JsonPrimitive value = number(object, field, errors);
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            errors.add(field + " must be an exact 32-bit integer");
            return Integer.MIN_VALUE;
        }
    }

    private static double decimal(JsonObject object, String field, List<String> errors) {
        JsonPrimitive value = number(object, field, errors);
        if (value == null) {
            return Double.NaN;
        }
        try {
            return value.getAsDouble();
        } catch (NumberFormatException exception) {
            errors.add(field + " must be a number");
            return Double.NaN;
        }
    }

    private static JsonPrimitive number(JsonObject object, String field, List<String> errors) {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            errors.add(field + " must be a JSON number");
            return null;
        }
        return value.getAsJsonPrimitive();
    }

    public record Result(Optional<ReactorParameters> parameters, List<String> errors) {
        public Result {
            parameters = parameters == null ? Optional.empty() : parameters;
            errors = List.copyOf(errors);
        }
    }
}
