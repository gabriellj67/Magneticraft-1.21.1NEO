package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatFluidGeneratedDataTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path RECIPES = GENERATED.resolve("data/magneticraft/recipes/crafting");
    private static final Path ASSETS = GENERATED.resolve("assets/magneticraft");

    @Test
    void transportRecipesPreserveNovaPatternsAndCounts() throws IOException {
        assertRecipe("heat_pipe", 5, " A ", "ABA", " A ");
        assertRecipe("insulated_heat_pipe", 5, "ABA", "BBB", "ABA");
        assertRecipe("insulated_heat_pipe_from_components", 5, "ABA", "BCB", "ABA");
        assertRecipe("heat_sink", 2, "AAA", "BBB");
        assertRecipe("iron_fluid_pipe", 12, " C ", "ABA", " C ");
        assertRecipe("small_tank", 1, "AAA", "ABA", "AAA");
    }

    @Test
    void generatedShapesCoverSixWaySinkAndLegacyPipeWidths() throws IOException {
        JsonObject variants = read(ASSETS.resolve("blockstates/heat_sink.json")).getAsJsonObject("variants");
        assertEquals(Set.of(
                "facing=down", "facing=up", "facing=north",
                "facing=south", "facing=west", "facing=east"
        ), variants.keySet());

        assertEquals(4, centerInset("heat_pipe"));
        assertEquals(3, centerInset("insulated_heat_pipe"));
        assertEquals(4, centerInset("iron_fluid_pipe"));
    }

    private static void assertRecipe(String name, int count, String... pattern) throws IOException {
        JsonObject recipe = read(RECIPES.resolve(name + ".json"));
        JsonArray generatedPattern = recipe.getAsJsonArray("pattern");
        assertEquals(pattern.length, generatedPattern.size(), name);
        for (int row = 0; row < pattern.length; row++) {
            assertEquals(pattern[row], generatedPattern.get(row).getAsString(), name + " row " + row);
        }
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(count, result.has("count") ? result.get("count").getAsInt() : 1, name);
    }

    private static int centerInset(String model) throws IOException {
        JsonArray from = read(ASSETS.resolve("models/block/" + model + ".json"))
                .getAsJsonArray("elements")
                .get(0)
                .getAsJsonObject()
                .getAsJsonArray("from");
        assertEquals(from.get(0).getAsInt(), from.get(1).getAsInt());
        assertEquals(from.get(1).getAsInt(), from.get(2).getAsInt());
        return from.get(0).getAsInt();
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
