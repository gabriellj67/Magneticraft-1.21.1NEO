package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedDataContractTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ASSETS = GENERATED.resolve("assets/magneticraft");
    private static final Path DATA = GENERATED.resolve("data/magneticraft");
    private static final Path SOURCE_TEXTURES = Path.of("src/main/resources/assets/magneticraft/textures");

    @Test
    void everyGeneratedJsonFileIsValid() throws IOException {
        try (Stream<Path> paths = Files.walk(GENERATED)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
                assertNotNull(readJson(path), path.toString());
            }
        }
    }

    @Test
    void generatedModelsTranslationsAndSourceTexturesCoverTheCatalogues() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        JsonObject blockAtlas = readObject(GENERATED.resolve("assets/minecraft/atlases/blocks.json"));
        Set<String> atlasSprites = new HashSet<>();
        blockAtlas.getAsJsonArray("sources").forEach(source ->
                atlasSprites.add(source.getAsJsonObject().get("resource").getAsString())
        );
        assertEquals(english.keySet(), chinese.keySet(), "Language keys must remain symmetric");

        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            assertFile(ASSETS.resolve("blockstates/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/block/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/item/" + definition.id() + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + definition.id() + ".json"));
            assertPng(SOURCE_TEXTURES.resolve("block/" + definition.id() + ".png"));
            assertTrue(english.has("block.magneticraft." + definition.id()), definition.id());
        }

        for (MaterialForm form : MaterialForm.values()) {
            for (Metal metal : Metal.values()) {
                if (form.appliesTo(metal)) {
                    assertItemAssetsAndTranslation(form.id(metal), english);
                }
            }
        }
        for (CraftingComponent component : CraftingComponent.values()) {
            assertItemAssetsAndTranslation(component.id(), english);
        }
        for (HammerType type : HammerType.values()) {
            assertItemAssetsAndTranslation(type.id(), english);
        }

        for (FluidDefinition definition : FluidDefinition.values()) {
            assertFile(ASSETS.resolve("blockstates/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/block/" + definition.id() + ".json"));
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_still.png"));
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_flow.png"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_still.png.mcmeta"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_flow.png.mcmeta"));
            JsonObject bucketModel = readObject(ASSETS.resolve("models/item/" + definition.id() + "_bucket.json"));
            assertEquals("forge:fluid_container", bucketModel.get("loader").getAsString());
            assertEquals("magneticraft:" + definition.id(), bucketModel.get("fluid").getAsString());
            assertTrue(english.has(definition.translationKey()), definition.translationKey());
            assertTrue(english.has("item.magneticraft." + definition.id() + "_bucket"), definition.id());
            assertFile(GENERATED.resolve("data/magneticraft/tags/fluids/" + definition.id() + ".json"));
            assertFile(GENERATED.resolve("data/forge/tags/fluids/" + definition.id() + ".json"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.id() + "_still"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.id() + "_flow"));
        }
        assertEquals(FluidDefinition.values().length * 2, atlasSprites.size());
    }

    @Test
    void generatedRecipesCoverEveryLegacyBaseRecipe() throws IOException {
        Set<Path> expectedCrafting = new HashSet<>();
        for (Metal metal : Metal.values()) {
            if (MaterialForm.NUGGET.appliesTo(metal)) {
                expectedCrafting.add(recipe("crafting/" + metal.id() + "_ingot_from_material"));
                expectedCrafting.add(recipe("crafting/" + metal.id() + "_ingot_to_material"));
            }
        }
        for (String storage : Set.of("lead_block", "cobalt_block", "tungsten_block", "sulfur_block")) {
            expectedCrafting.add(recipe("crafting/" + storage + "_from_material"));
            expectedCrafting.add(recipe("crafting/" + storage + "_to_material"));
        }
        for (String recipe : Set.of(
                "limestone_bricks",
                "burnt_limestone_bricks",
                "limestone_tiles",
                "inverted_limestone_tiles",
                "alternator",
                "motor",
                "fine_copper_wire",
                "magnet",
                "iron_mesh",
                "fabric_mesh",
                "stone_hammer",
                "iron_hammer",
                "steel_hammer"
        )) {
            expectedCrafting.add(recipe("crafting/" + recipe));
        }
        assertEquals(45, expectedCrafting.size());
        expectedCrafting.forEach(GeneratedDataContractTest::assertFile);

        Set<Path> expectedSmelting = new HashSet<>();
        for (String ore : Set.of("galena_ore", "cobalt_ore", "tungsten_ore")) {
            expectedSmelting.add(recipe("smelting/" + ore));
        }
        for (Metal metal : Metal.values()) {
            if (MaterialForm.DUST.appliesTo(metal)) {
                expectedSmelting.add(recipe("smelting/" + metal.id() + "_dust"));
            }
            if (MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                expectedSmelting.add(recipe("smelting/" + metal.id() + "_rocky_chunk"));
            }
            if (MaterialForm.CHUNK.appliesTo(metal)) {
                Path recipe = recipe("smelting/" + metal.id() + "_chunk");
                expectedSmelting.add(recipe);
                assertEquals(2, readObject(recipe).getAsJsonObject("result").get("count").getAsInt(), recipe.toString());
            }
        }
        expectedSmelting.add(recipe("smelting/limestone"));
        expectedSmelting.add(recipe("smelting/cobbled_limestone"));
        assertEquals(46, expectedSmelting.size());
        expectedSmelting.forEach(GeneratedDataContractTest::assertFile);

        JsonObject galena = readObject(recipe("smelting/galena_rocky_chunk"));
        assertEquals(2, galena.getAsJsonObject("result").get("count").getAsInt());
        for (Path path : expectedSmelting) {
            assertEquals("minecraft:smelting", readObject(path).get("type").getAsString(), path.toString());
            assertFalse(path.toString().contains("blasting"), path.toString());
        }
    }

    private static void assertItemAssetsAndTranslation(String id, JsonObject language) throws IOException {
        assertPng(SOURCE_TEXTURES.resolve("item/" + id + ".png"));
        assertFile(ASSETS.resolve("models/item/" + id + ".json"));
        assertTrue(language.has("item.magneticraft." + id), id);
    }

    private static Path recipe(String path) {
        return DATA.resolve("recipes/" + path + ".json");
    }

    private static JsonElement readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        assertFile(path);
        return readJson(path).getAsJsonObject();
    }

    private static void assertPng(Path path) throws IOException {
        assertFile(path);
        assertNotNull(ImageIO.read(path.toFile()), "Unreadable PNG: " + path);
    }

    private static void assertFile(Path path) {
        assertTrue(Files.isRegularFile(path), "Missing file: " + path);
    }
}
