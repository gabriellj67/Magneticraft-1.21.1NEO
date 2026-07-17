package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates built-in nuclear balance entries from the same fixed fuel-grade catalogue as the items. */
final class NuclearDataProvider implements DataProvider {
    private final PackOutput.PathProvider fuels;
    private final PackOutput.PathProvider reactorParameters;

    NuclearDataProvider(PackOutput output) {
        fuels = output.createPathProvider(PackOutput.Target.DATA_PACK, "magneticraft/nuclear_fuels");
        reactorParameters = output.createPathProvider(
                PackOutput.Target.DATA_PACK, "magneticraft/nuclear/reactor_parameters");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            writes.add(DataProvider.saveStable(output, fuelJson(grade), fuels.json(grade.definitionId())));
        }
        writes.add(DataProvider.saveStable(
                output, reactorParametersJson(ReactorParameters.DEFAULT),
                reactorParameters.json(ReactorParameters.PWR_ID)));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Magneticraft nuclear fuel data";
    }

    static JsonObject fuelJson(NuclearFuelGrade grade) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", NuclearFuelDefinition.SCHEMA_VERSION);
        root.addProperty("enrichment_percent", grade.enrichmentPercent());
        root.addProperty("thermal_power_joules_per_tick", grade.thermalPowerJoulesPerTick());
        root.addProperty("design_life_ticks", grade.designLifeTicks());
        root.addProperty("initial_reactivity", grade.initialReactivity());
        root.addProperty("temperature_coefficient_per_kelvin", grade.temperatureCoefficientPerKelvin());
        root.addProperty("void_coefficient", grade.voidCoefficient());
        root.addProperty("decay_heat_fraction", grade.decayHeatFraction());
        root.addProperty("cladding_failure_temperature_kelvin", grade.claddingFailureTemperatureKelvin());
        root.addProperty("load_follow_rate_per_tick", grade.loadFollowRatePerTick());
        return root;
    }

    static JsonObject reactorParametersJson(ReactorParameters value) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", ReactorParameters.SCHEMA_VERSION);
        root.addProperty("station_instrumentation_joules_per_tick", value.stationInstrumentationJoulesPerTick());
        root.addProperty("station_running_joules_per_tick", value.stationRunningJoulesPerTick());
        root.addProperty("startup_ticks", value.startupTicks());
        root.addProperty("minimum_coolant_fraction", value.minimumCoolantFraction());
        root.addProperty("fuel_temperature_response_per_tick", value.fuelTemperatureResponsePerTick());
        root.addProperty("fuel_temperature_rise_at_full_power_kelvin", value.fuelTemperatureRiseAtFullPowerKelvin());
        root.addProperty("poison_build_per_tick", value.poisonBuildPerTick());
        root.addProperty("poison_decay_per_tick", value.poisonDecayPerTick());
        root.addProperty("decay_heat_response_per_tick", value.decayHeatResponsePerTick());
        root.addProperty("decay_heat_loss_per_tick", value.decayHeatLossPerTick());
        root.addProperty("cladding_damage_per_kelvin_tick", value.claddingDamagePerKelvinTick());
        root.addProperty("forced_scram_temperature_kelvin", value.forcedScramTemperatureKelvin());
        root.addProperty("safe_unload_temperature_kelvin", value.safeUnloadTemperatureKelvin());
        root.addProperty("automatic_rod_step_per_tick", value.automaticRodStepPerTick());
        root.addProperty("maximum_offline_catchup_ticks", value.maximumOfflineCatchupTicks());
        return root;
    }
}
