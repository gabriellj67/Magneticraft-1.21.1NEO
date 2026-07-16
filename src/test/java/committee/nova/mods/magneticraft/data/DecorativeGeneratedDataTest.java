package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.block.DecorativeBlockFamily;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorativeGeneratedDataTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ASSETS = GENERATED.resolve("assets/magneticraft");
    private static final Path DATA = GENERATED.resolve("data/magneticraft");
    private static final Path SOURCE_ASSETS = Path.of("src/main/resources/assets/magneticraft");
    private static final Path LEGACY_MANIFEST = Path.of(
            "src/test/resources/magneticraft/legacy/legacy_1710_roof_tile_texture_manifest.json"
    );

    @Test
    void everyNewDecorativeShapeHasAssetsLootRecipesTagsTranslationsAndGuideEntry() throws IOException {
        JsonObject english = read(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = read(ASSETS.resolve("lang/zh_cn.json"));
        JsonArray guideItems = read(ASSETS.resolve("guide/items/portable_electric.json"))
                .getAsJsonArray("items");
        Set<String> guideIds = new HashSet<>();
        guideItems.forEach(element -> guideIds.add(element.getAsJsonObject().get("id").getAsString()));
        Set<String> mineable = new HashSet<>();
        read(GENERATED.resolve("data/minecraft/tags/blocks/mineable/pickaxe.json"))
                .getAsJsonArray("values")
                .forEach(element -> mineable.add(element.getAsString()));

        int newBlocks = 0;
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            if (family.registersBase()) {
                assertGeneratedBlock(family.baseId(), english, chinese, guideIds, mineable);
                newBlocks++;
            }
            assertGeneratedBlock(family.stairsId(), english, chinese, guideIds, mineable);
            assertGeneratedBlock(family.slabId(), english, chinese, guideIds, mineable);
            assertShapeRecipes(family);
            newBlocks += 2;
        }
        assertEquals(19, newBlocks);

        JsonObject roofRecipe = read(DATA.resolve("recipes/crafting/roof_tile.json"));
        assertEquals(2, roofRecipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("magneticraft:roof_tile",
                roofRecipe.getAsJsonObject("result").get("item").getAsString());
        assertEquals("minecraft:brick",
                roofRecipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("[\"B B\",\" B \",\"B B\"]", roofRecipe.getAsJsonArray("pattern").toString());
    }

    @Test
    void roofBlockStatesUseFourEqualLegacyTextureVariantsForEveryShape() throws IOException {
        assertFourEqualVariants(ASSETS.resolve("blockstates/roof_tile.json"), 1);
        assertFourEqualVariants(ASSETS.resolve("blockstates/roof_tile_slab.json"), 3);
        assertFourEqualVariants(ASSETS.resolve("blockstates/roof_tile_stairs.json"), 40);
    }

    @Test
    void roofTexturesRemainByteIdenticalToTheReviewedLegacySnapshot() throws Exception {
        JsonObject manifest = read(LEGACY_MANIFEST);
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals("ipl-adm/magneticraft-legacy-1.7.10",
                manifest.get("source_repository").getAsString());
        assertEquals("master", manifest.get("source_ref").getAsString());
        assertEquals("magneticraft-0.6.0-final", manifest.get("release_file").getAsString());

        JsonArray artifacts = manifest.getAsJsonArray("artifacts");
        assertEquals(4, artifacts.size());
        Set<String> runtimePaths = new HashSet<>();
        for (JsonElement element : artifacts) {
            JsonObject artifact = element.getAsJsonObject();
            assertTrue(artifact.get("source").getAsString().startsWith("textures/blocks/roofTile_"));
            String runtime = artifact.get("runtime").getAsString();
            assertTrue(runtimePaths.add(runtime), runtime);
            Path texture = SOURCE_ASSETS.resolve(runtime);
            assertTrue(Files.isRegularFile(texture), runtime);
            var image = ImageIO.read(texture.toFile());
            assertNotNull(image, runtime);
            assertEquals(16, image.getWidth(), runtime);
            assertEquals(16, image.getHeight(), runtime);
            assertEquals(artifact.get("sha256").getAsString(), sha256(texture), runtime);
        }
    }

    private static void assertGeneratedBlock(
            String id,
            JsonObject english,
            JsonObject chinese,
            Set<String> guideIds,
            Set<String> mineable
    ) {
        assertFile(ASSETS.resolve("blockstates/" + id + ".json"));
        assertFile(ASSETS.resolve("models/item/" + id + ".json"));
        assertFile(DATA.resolve("loot_tables/blocks/" + id + ".json"));
        String blockKey = "block.magneticraft." + id;
        String guideKey = "guide.magneticraft.item." + id + ".description";
        assertTrue(english.has(blockKey), blockKey);
        assertTrue(chinese.has(blockKey), blockKey);
        assertTrue(english.has(guideKey), guideKey);
        assertTrue(chinese.has(guideKey), guideKey);
        assertTrue(guideIds.contains("magneticraft:" + id), id);
        assertTrue(mineable.contains("magneticraft:" + id), id);
    }

    private static void assertShapeRecipes(DecorativeBlockFamily family) throws IOException {
        assertCraftingRecipe(family.slabId(), family.baseId(), 6, "[\"BBB\"]");
        assertCraftingRecipe(family.stairsId(), family.baseId(), 4, "[\"B  \",\"BB \",\"BBB\"]");
        assertStonecuttingRecipe(family.slabId(), family.baseId(), 2);
        assertStonecuttingRecipe(family.stairsId(), family.baseId(), 1);
        assertDoubleSlabDropsTwo(family.slabId());
    }

    private static void assertDoubleSlabDropsTwo(String slabId) throws IOException {
        JsonObject loot = read(DATA.resolve("loot_tables/blocks/" + slabId + ".json"));
        JsonArray functions = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonArray("functions");
        JsonObject setCount = functions.get(0).getAsJsonObject();
        assertEquals("minecraft:set_count", setCount.get("function").getAsString(), slabId);
        assertEquals(2.0D, setCount.get("count").getAsDouble(), slabId);
        JsonObject condition = setCount.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("double", condition.getAsJsonObject("properties").get("type").getAsString(), slabId);
    }

    private static void assertCraftingRecipe(String resultId, String inputId, int count, String pattern)
            throws IOException {
        JsonObject recipe = read(DATA.resolve("recipes/crafting/" + resultId + ".json"));
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), resultId);
        assertEquals("magneticraft:" + inputId,
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString(), resultId);
        assertEquals(pattern, recipe.getAsJsonArray("pattern").toString(), resultId);
        assertEquals("magneticraft:" + resultId,
                recipe.getAsJsonObject("result").get("item").getAsString(), resultId);
        assertEquals(count, recipe.getAsJsonObject("result").get("count").getAsInt(), resultId);
    }

    private static void assertStonecuttingRecipe(String resultId, String inputId, int count) throws IOException {
        JsonObject recipe = read(DATA.resolve("recipes/stonecutting/" + resultId + ".json"));
        assertEquals("minecraft:stonecutting", recipe.get("type").getAsString(), resultId);
        assertEquals("magneticraft:" + inputId,
                recipe.getAsJsonObject("ingredient").get("item").getAsString(), resultId);
        assertEquals("magneticraft:" + resultId, recipe.get("result").getAsString(), resultId);
        assertEquals(count, recipe.get("count").getAsInt(), resultId);
    }

    private static void assertFourEqualVariants(Path path, int expectedStateCount) throws IOException {
        JsonObject variants = read(path).getAsJsonObject("variants");
        assertEquals(expectedStateCount, variants.size(), path.toString());
        for (var entry : variants.entrySet()) {
            JsonArray models = entry.getValue().getAsJsonArray();
            assertEquals(4, models.size(), path + " " + entry.getKey());
            for (int variant = 0; variant < models.size(); variant++) {
                JsonObject model = models.get(variant).getAsJsonObject();
                assertTrue(model.get("model").getAsString().endsWith("_" + variant), model.toString());
                assertTrue(!model.has("weight") || model.get("weight").getAsInt() == 1, model.toString());
            }
        }
    }

    private static JsonObject read(Path path) throws IOException {
        assertFile(path);
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest);
    }

    private static void assertFile(Path path) {
        assertTrue(Files.isRegularFile(path), "Missing file: " + path);
    }
}
