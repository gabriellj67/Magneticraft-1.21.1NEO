package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearDataParser;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterParser;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NuclearDataProviderTest {
    @AfterEach
    void resetParameters() {
        ReactorParameterRegistry.INSTANCE.reset();
    }

    @Test
    void generatedCatalogueParsesAndKeepsEveryGradeBelowTwentyPercentEnrichment() {
        var resources = new LinkedHashMap<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            resources.put(grade.definitionId(), NuclearDataProvider.fuelJson(grade));
        }

        var parsed = NuclearDataParser.parse(resources);

        assertTrue(parsed.errors().isEmpty());
        assertEquals(NuclearFuelGrade.values().length, parsed.fuelDefinitions().size());
        assertTrue(parsed.fuelDefinitions().values().stream()
                .mapToDouble(NuclearFuelDefinition::enrichmentPercent)
                .allMatch(enrichment -> enrichment < 20.0D));
    }

    @Test
    void generatedReactorParametersRoundTripAndInvalidReloadCannotReplaceLiveBalance() {
        var generated = NuclearDataProvider.reactorParametersJson(ReactorParameters.DEFAULT);
        var parsed = ReactorParameterParser.parse(ReactorParameters.PWR_ID, generated);

        assertTrue(parsed.errors().isEmpty());
        assertEquals(ReactorParameters.DEFAULT, parsed.parameters().orElseThrow());
        var applied = ReactorParameterRegistry.INSTANCE.apply(parsed.parameters().orElseThrow());

        generated.addProperty("minimum_coolant_fraction", 1.5D);
        var invalid = ReactorParameterParser.parse(ReactorParameters.PWR_ID, generated);

        assertFalse(invalid.errors().isEmpty());
        assertTrue(invalid.parameters().isEmpty());
        assertEquals(applied, ReactorParameterRegistry.INSTANCE.current());
    }
}
