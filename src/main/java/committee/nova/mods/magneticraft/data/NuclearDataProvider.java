package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates built-in nuclear balance entries from the same fixed fuel-grade catalogue as the items. */
final class NuclearDataProvider implements DataProvider {
    private final PackOutput.PathProvider fuels;

    NuclearDataProvider(PackOutput output) {
        fuels = output.createPathProvider(PackOutput.Target.DATA_PACK, "magneticraft/nuclear_fuels");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            writes.add(DataProvider.saveStable(output, fuelJson(grade), fuels.json(grade.definitionId())));
        }
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
}
