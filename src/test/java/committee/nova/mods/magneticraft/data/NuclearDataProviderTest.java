package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearDataParser;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearDataProviderTest {
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
}
