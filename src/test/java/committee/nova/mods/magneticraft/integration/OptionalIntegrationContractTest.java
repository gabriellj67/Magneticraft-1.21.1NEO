package committee.nova.mods.magneticraft.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalIntegrationContractTest {
    private static final Path JAVA = Path.of("src/main/java");
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void optionalVersionsAreFixedAndDevelopmentRuntimesAreOptIn() throws IOException {
        Map<String, String> properties = Files.readAllLines(Path.of("gradle.properties")).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#") && line.contains("="))
                .map(line -> line.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(parts -> parts[0], parts -> parts[1]));

        assertEquals("15.20.0.133", properties.get("jei_version"));
        assertEquals("14.0.60", properties.get("crafttweaker_version"));
        assertEquals("1.11.97", properties.get("mantle_version"));
        assertEquals("3.11.2.166", properties.get("tconstruct_version"));
        assertEquals("false", properties.get("enable_jei_runtime"));
        assertEquals("false", properties.get("enable_crafttweaker_runtime"));
        assertEquals("false", properties.get("enable_tconstruct_runtime"));
        assertFalse(properties.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_version"))
                .anyMatch(entry -> entry.getValue().contains("+") || entry.getValue().equals("latest")));
    }

    @Test
    void commonCodeDoesNotLinkOptionalJavaApis() throws IOException {
        try (Stream<Path> paths = Files.walk(JAVA)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(this::isCommonJavaSource).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("mezz.jei."), path.toString());
                assertFalse(source.contains("com.blamejared.crafttweaker."), path.toString());
                assertFalse(source.contains("org.openzen.zencode."), path.toString());
                assertFalse(source.contains("slimeknights.tconstruct."), path.toString());
            }
        }
    }

    @Test
    void metadataDeclaresAllIntegrationsAsOptional() throws IOException {
        String metadata = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));

        for (String modId : List.of("jei", "crafttweaker", "tconstruct")) {
            int declaration = metadata.indexOf("modId = \"" + modId + "\"");
            assertTrue(declaration >= 0, modId);
            int nextDeclaration = metadata.indexOf("[[dependencies.", declaration + 1);
            String dependency = nextDeclaration < 0
                    ? metadata.substring(declaration)
                    : metadata.substring(declaration, nextDeclaration);
            assertTrue(dependency.contains("mandatory = false"), modId);
        }
    }

    @Test
    void jeiPluginAndCraftTweakerFacadeCoverAllRecipeTypes() throws IOException {
        String jei = Files.readString(JAVA.resolve(
                "committee/nova/mods/magneticraft/integration/jei/MagneticraftJeiPlugin.java"
        ));
        String craftTweaker = Files.readString(JAVA.resolve(
                "committee/nova/mods/magneticraft/integration/crafttweaker/MagneticraftCraftTweakerRecipes.java"
        ));

        for (String type : List.of(
                "crushing_table",
                "sluice_box",
                "gasification_unit",
                "thermopile",
                "fluid_fuel",
                "advanced_processing"
        )) {
            assertTrue(jei.contains("\"" + type + "\""), type);
            assertTrue(craftTweaker.contains("\"" + type + "\""), type);
        }
        assertTrue(Files.isRegularFile(Path.of(
                "src/integrationTest/crafttweaker/scripts/magneticraft_recipes.zs"
        )));
    }

    @Test
    void tconstructDataAddsOnlyTungstenAndReusesLeadSteelTags() throws IOException {
        JsonObject materialRecipe = readJson(GENERATED.resolve(
                "data/magneticraft/recipes/integration/tconstruct/tungsten_ingot_material.json"
        ));
        assertEquals("forge:ingots/tungsten", materialRecipe.getAsJsonObject("ingredient").get("tag").getAsString());

        assertTagContains("lead", "magneticraft:lead_ingot");
        assertTagContains("steel", "magneticraft:steel_ingot");
        assertTagContains("tungsten", "magneticraft:tungsten_ingot");

        Path definitionRoot = GENERATED.resolve("data/magneticraft/tinkering/materials/definition");
        try (Stream<Path> definitions = Files.list(definitionRoot)) {
            assertEquals(List.of("tungsten.json"), definitions.map(path -> path.getFileName().toString()).sorted().toList());
        }
    }

    private boolean isCommonJavaSource(Path path) {
        if (!path.toString().endsWith(".java")) {
            return false;
        }
        String normalized = path.toString().replace('\\', '/');
        return !normalized.contains("/integration/jei/")
                && !normalized.contains("/integration/crafttweaker/");
    }

    private static void assertTagContains(String metal, String item) throws IOException {
        JsonObject tag = readJson(GENERATED.resolve("data/forge/tags/items/ingots/" + metal + ".json"));
        assertTrue(tag.getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals(item)), metal);
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
