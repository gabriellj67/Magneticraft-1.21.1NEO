package committee.nova.mods.magneticraft.system.nuclear.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearDataParserTest {
    @AfterEach
    void resetRegistry() {
        NuclearDataRegistry.INSTANCE.reset();
    }

    @Test
    void builtInFuelDataIsValidAndStandardLifeIsSixHours() {
        NuclearDataLoadResult result = NuclearDataParser.parse(builtIns());

        assertTrue(result.errors().isEmpty());
        assertEquals(3, result.fuelDefinitions().size());
        NuclearFuelDefinition standard = result.fuelDefinitions()
                .get(NuclearFuelGrade.STANDARD_ENRICHMENT.definitionId());
        assertEquals(432_000, standard.designLifeTicks());
        assertEquals(6.0D, standard.designLifeTicks() / 20.0D / 60.0D / 60.0D);
        assertEquals(standard.thermalPowerJoulesPerTick() * 432_000.0D, standard.totalEnergyJoules());
    }

    @Test
    void malformedEntryIsRejectedWithoutDiscardingIndependentValidEntries() {
        Map<ResourceLocation, JsonElement> resources = new LinkedHashMap<>(builtIns());
        ResourceLocation invalidId = ResourceLocation.fromNamespaceAndPath("example", "unsafe_fuel");
        JsonObject invalid = fuel(NuclearFuelGrade.STANDARD_ENRICHMENT);
        invalid.addProperty("enrichment_percent", 95.0D);
        resources.put(invalidId, invalid);

        NuclearDataLoadResult result = NuclearDataParser.parse(resources);

        assertFalse(result.errors().isEmpty());
        assertTrue(result.rejectedFuelIds().contains(invalidId));
        assertEquals(3, result.fuelDefinitions().size());
    }

    @Test
    void malformedOverrideFallsBackToPreviousDefinitionForThatIdOnly() {
        NuclearDataSnapshot first = NuclearDataRegistry.INSTANCE.apply(
                NuclearDataParser.parse(builtIns())
        ).current();
        Map<ResourceLocation, JsonElement> nextResources = new LinkedHashMap<>(builtIns());
        ResourceLocation standardId = NuclearFuelGrade.STANDARD_ENRICHMENT.definitionId();
        JsonObject invalidStandard = fuel(NuclearFuelGrade.STANDARD_ENRICHMENT);
        invalidStandard.addProperty("void_coefficient", 0.1D);
        nextResources.put(standardId, invalidStandard);

        NuclearDataSnapshot second = NuclearDataRegistry.INSTANCE.apply(
                NuclearDataParser.parse(nextResources)
        ).current();

        assertTrue(second.generation() > first.generation());
        assertEquals(first.fuel(standardId), second.fuel(standardId));
    }

    @Test
    void firstLoadFailsClosedWhenARequiredBuiltInIsUnavailable() {
        Map<ResourceLocation, JsonElement> incomplete = new LinkedHashMap<>(builtIns());
        incomplete.remove(NuclearFuelGrade.HIGH_ENRICHMENT.definitionId());

        assertThrows(
                NuclearDataReloadException.class,
                () -> NuclearDataRegistry.INSTANCE.apply(NuclearDataParser.parse(incomplete))
        );
    }

    @Test
    void resourcePathMapsToStableLogicalId() {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(
                "example",
                "magneticraft/nuclear_fuels/operator_pack.json"
        );
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("example", "operator_pack"),
                NuclearDataReloadListener.logicalId(file)
        );
    }

    private static Map<ResourceLocation, JsonElement> builtIns() {
        LinkedHashMap<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            result.put(grade.definitionId(), fuel(grade));
        }
        return result;
    }

    private static JsonObject fuel(NuclearFuelGrade grade) {
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
