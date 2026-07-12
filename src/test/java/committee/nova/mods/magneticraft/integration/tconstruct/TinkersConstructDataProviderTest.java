package committee.nova.mods.magneticraft.integration.tconstruct;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinkersConstructDataProviderTest {
    @Test
    void tungstenDefinitionIsTierThreeAndConditionedOnTconstruct() {
        JsonObject definition = TinkersConstructDataProvider.definition();

        assertEquals("forge:mod_loaded", definition.getAsJsonObject("condition").get("type").getAsString());
        assertEquals("tconstruct", definition.getAsJsonObject("condition").get("modid").getAsString());
        assertTrue(definition.get("craftable").getAsBoolean());
        assertFalse(definition.get("hidden").getAsBoolean());
        assertEquals(3, definition.get("tier").getAsInt());
    }

    @Test
    void tungstenStatsCoverMeleeAndRangedPartsWithoutArmorInflation() {
        JsonObject stats = TinkersConstructDataProvider.stats().getAsJsonObject("stats");

        assertEquals(
                Set.of(
                        "tconstruct:binding",
                        "tconstruct:head",
                        "tconstruct:handle",
                        "tconstruct:limb",
                        "tconstruct:grip"
                ),
                stats.keySet()
        );
        assertEquals(950, stats.getAsJsonObject("tconstruct:head").get("durability").getAsInt());
        assertEquals("minecraft:diamond", stats.getAsJsonObject("tconstruct:head").get("mining_tier").getAsString());
        assertEquals(-0.2F, stats.getAsJsonObject("tconstruct:limb").get("draw_speed").getAsFloat());
    }

    @Test
    void materialRecipeUsesForgeTungstenTagAndLoadCondition() {
        JsonObject recipe = TinkersConstructDataProvider.ingotRecipe();

        assertEquals("tconstruct:material", recipe.get("type").getAsString());
        assertEquals("forge:ingots/tungsten", recipe.getAsJsonObject("ingredient").get("tag").getAsString());
        assertEquals("magneticraft:tungsten", recipe.get("material").getAsString());
        assertEquals("forge:mod_loaded", recipe.getAsJsonArray("conditions")
                .get(0)
                .getAsJsonObject()
                .get("type")
                .getAsString());
    }

    @Test
    void renderInfoCanGenerateEveryDeclaredToolPart() {
        JsonObject renderInfo = TinkersConstructDataProvider.renderInfo();

        assertEquals("metal", renderInfo.getAsJsonArray("fallbacks").get(0).getAsString());
        assertEquals(7, renderInfo.getAsJsonObject("generator")
                .getAsJsonArray("supported_stats")
                .size());
        assertEquals("tconstruct:recolor_sprite", renderInfo.getAsJsonObject("generator")
                .getAsJsonObject("transformer")
                .get("type")
                .getAsString());
    }
}
