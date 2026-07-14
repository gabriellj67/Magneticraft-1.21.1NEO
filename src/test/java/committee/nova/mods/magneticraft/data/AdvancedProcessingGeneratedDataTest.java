package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedProcessingGeneratedDataTest {
    private static final Path GENERATED_RECIPES = Path.of(
            "src/generated/resources/data/magneticraft/recipes/advanced_processing"
    );
    private static final Map<String, FluidRecipeContract> OIL_PROCESSING_RECIPES = Map.ofEntries(
            Map.entry("oil_heater_water_to_steam", new FluidRecipeContract(
                    "oil_heater", true, "minecraft:water", 1,
                    List.of(output(0, "magneticraft:steam", 10)), 1, 373.15D
            )),
            Map.entry("oil_heater_crude_oil_to_heated_crude_oil", new FluidRecipeContract(
                    "oil_heater", false, "magneticraft:crude_oil", 10,
                    List.of(output(0, "magneticraft:heated_crude_oil", 100)), 2, 623.15D
            )),
            Map.entry("refinery_steam_to_water", new FluidRecipeContract(
                    "refinery", false, "magneticraft:steam", 10,
                    List.of(output(0, "minecraft:water", 1)), 2, 0.0D
            )),
            Map.entry("refinery_heated_crude_oil_fractionation", new FluidRecipeContract(
                    "refinery", false, "magneticraft:heated_crude_oil", 100,
                    List.of(
                            output(0, "magneticraft:heavy_oil", 4),
                            output(1, "magneticraft:light_oil", 3),
                            output(2, "magneticraft:lpg", 3)
                    ), 1, 0.0D
            )),
            Map.entry("refinery_heavy_oil_fractionation", new FluidRecipeContract(
                    "refinery", false, "magneticraft:heavy_oil", 10,
                    List.of(
                            output(0, "magneticraft:oil_residue", 4),
                            output(1, "magneticraft:fuel_oil", 5),
                            output(2, "magneticraft:lubricant", 1)
                    ), 1, 0.0D
            )),
            Map.entry("refinery_light_oil_fractionation", new FluidRecipeContract(
                    "refinery", false, "magneticraft:light_oil", 10,
                    List.of(
                            output(0, "magneticraft:diesel", 5),
                            output(1, "magneticraft:kerosene", 2),
                            output(2, "magneticraft:gasoline", 3)
                    ), 1, 0.0D
            )),
            Map.entry("refinery_lpg_fractionation", new FluidRecipeContract(
                    "refinery", false, "magneticraft:lpg", 10,
                    List.of(
                            output(0, "magneticraft:liquid_plastic", 5),
                            output(1, "magneticraft:naphtha", 2),
                            output(2, "magneticraft:natural_gas", 3)
                    ), 1, 0.0D
            ))
    );

    @Test
    void generatorEmitsExactlySevenAdvancedProcessingRecipePaths() throws IOException {
        assertEquals(OIL_PROCESSING_RECIPES.keySet(), generatedRecipes().keySet());
    }

    @Test
    void generatedOilProcessingRatiosTanksDurationsAndTemperaturesMatchNovaContracts() throws IOException {
        Map<String, JsonObject> generated = generatedRecipes();
        for (Map.Entry<String, FluidRecipeContract> entry : OIL_PROCESSING_RECIPES.entrySet()) {
            String name = entry.getKey();
            FluidRecipeContract expected = entry.getValue();
            JsonObject recipe = generated.get(name);

            assertEquals("magneticraft:" + expected.machine(), recipe.get("type").getAsString(), name);
            assertEquals(expected.machine(), recipe.get("machine").getAsString(), name);

            JsonObject input = recipe.getAsJsonObject("fluid_input");
            String inputKey = expected.taggedInput() ? "tag" : "fluid";
            String forbiddenInputKey = expected.taggedInput() ? "fluid" : "tag";
            assertEquals(expected.input(), input.get(inputKey).getAsString(), name);
            assertFalse(input.has(forbiddenInputKey), name);
            assertEquals(expected.inputAmount(), input.get("amount").getAsInt(), name);

            JsonArray outputs = recipe.getAsJsonArray("fluid_results");
            assertEquals(expected.outputs().size(), outputs.size(), name);
            for (int index = 0; index < expected.outputs().size(); index++) {
                FluidOutputContract expectedOutput = expected.outputs().get(index);
                JsonObject output = outputs.get(index).getAsJsonObject();
                assertEquals(expectedOutput.tank(), output.get("tank").getAsInt(), name + " tank " + index);
                assertEquals(expectedOutput.fluid(), output.get("fluid").getAsString(), name + " fluid " + index);
                assertEquals(expectedOutput.amount(), output.get("amount").getAsInt(), name + " amount " + index);
            }

            assertEquals(expected.duration(), recipe.get("duration").getAsInt(), name);
            if (expected.minimumTemperatureKelvin() == 0.0D) {
                assertFalse(recipe.has("minimum_temperature"), name);
            } else {
                assertTrue(recipe.has("minimum_temperature"), name);
                assertEquals(
                        expected.minimumTemperatureKelvin(),
                        recipe.get("minimum_temperature").getAsDouble(),
                        name
                );
            }
        }
    }

    private static Map<String, JsonObject> generatedRecipes() throws IOException {
        Map<String, JsonObject> generated = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(GENERATED_RECIPES)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                    .filter(AdvancedProcessingGeneratedDataTest::isOilProcessingRecipe)
                    .sorted()
                    .toList()) {
                String fileName = path.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - ".json".length());
                try (Reader reader = Files.newBufferedReader(path)) {
                    generated.put(name, JsonParser.parseReader(reader).getAsJsonObject());
                }
            }
        }
        return generated;
    }

    private static boolean isOilProcessingRecipe(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith("oil_heater_") || fileName.startsWith("refinery_");
    }

    private static FluidOutputContract output(int tank, String fluid, int amount) {
        return new FluidOutputContract(tank, fluid, amount);
    }

    private record FluidRecipeContract(
            String machine,
            boolean taggedInput,
            String input,
            int inputAmount,
            List<FluidOutputContract> outputs,
            int duration,
            double minimumTemperatureKelvin
    ) {
    }

    private record FluidOutputContract(int tank, String fluid, int amount) {
    }
}
