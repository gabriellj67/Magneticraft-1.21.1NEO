package committee.nova.mods.magneticraft.content.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TieredShapedRecipeTest {
    private static final Path RECIPE_ROOT =
            Path.of("src/generated/resources/data/magneticraft/recipes/crafting");
    private static final List<String> CONTENT = List.of(
            "battery_box",
            "electric_cable",
            "electric_connector",
            "electric_pole"
    );
    private static final List<String> TIERS = List.of(
            "low_voltage",
            "medium_voltage",
            "high_voltage"
    );

    @Test
    void allBuiltInTieredRecipesDeclareTheCustomSerializerAndVersionedResultPayload() throws IOException {
        int checked = 0;
        for (String content : CONTENT) {
            for (String tier : TIERS) {
                String suffix = tier.equals("low_voltage") ? "" : "_" + tier;
                Path recipePath = RECIPE_ROOT.resolve(content + suffix + ".json");
                assertTrue(Files.isRegularFile(recipePath), "Missing tiered recipe " + recipePath);
                JsonObject recipe = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
                assertEquals("magneticraft:tiered_shaped", recipe.get("type").getAsString(), recipePath.toString());
                JsonObject electrical = recipe.getAsJsonObject("electrical");
                assertEquals(1, electrical.get("schema_version").getAsInt(), recipePath.toString());
                assertEquals("magneticraft:" + tier, electrical.get("tier_id").getAsString(), recipePath.toString());
                checked++;
            }
        }
        assertEquals(12, checked);
    }
}
