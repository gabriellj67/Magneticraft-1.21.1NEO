package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockHologramModelContractTest {
    private static final Path ASSET_ROOT = Path.of(
            "src/generated/resources/assets/magneticraft"
    );
    private static final String UNMOUNTED_TEXTURE =
            "magneticraft:blocks/multiblocks/unmounted_multiblock";

    @Test
    void worldControllerAndInventorySceneUseSeparateModels() throws Exception {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            String id = definition.id();
            JsonObject worldModel = read("models/block/" + id + ".json");
            boolean polymerizer = definition == MultiblockDefinition.POLYMERIZER;
            assertEquals(polymerizer ? "minecraft:block/orientable" : "minecraft:block/cube_all",
                    worldModel.get("parent").getAsString(), id);
            assertFalse(worldModel.has("loader"), id + " world model must not obscure the hologram");
            if (polymerizer) {
                assertEquals("magneticraft:block/polymerizer_front",
                        worldModel.getAsJsonObject("textures").get("front").getAsString());
                assertEquals("magneticraft:block/polymerizer_side",
                        worldModel.getAsJsonObject("textures").get("side").getAsString());
            } else {
                assertEquals(UNMOUNTED_TEXTURE,
                        worldModel.getAsJsonObject("textures").get("all").getAsString(),
                        id + " unformed controller must use the original shared texture");
            }
            assertFalse(worldModel.has("render_type"), id + " opaque controller texture must stay solid");

            JsonObject formedModel = read("models/block/" + id + "_formed.json");
            assertEquals(UNMOUNTED_TEXTURE,
                    formedModel.getAsJsonObject("textures").get("particle").getAsString(), id);

            JsonObject blockstate = read("blockstates/" + id + ".json");
            JsonObject variants = blockstate.getAsJsonObject("variants");
            assertEquals(8, variants.size(), id + " must cover four facings and two formed states");
            variants.entrySet().forEach(entry -> {
                boolean formed = entry.getKey().contains("formed=true");
                String expected = "magneticraft:block/" + id + (formed ? "_formed" : "");
                assertEquals(expected, entry.getValue().getAsJsonObject().get("model").getAsString(),
                        id + " model mismatch for " + entry.getKey());
            });

            JsonObject itemModel = read("models/item/" + id + ".json");
            assertEquals("magneticraft:block/" + id + "_item",
                    itemModel.get("parent").getAsString(), id);

            JsonObject sceneModel = read("models/block/" + id + "_item.json");
            if (polymerizer) {
                assertEquals("minecraft:block/orientable", sceneModel.get("parent").getAsString());
                assertFalse(sceneModel.has("loader"));
                assertFalse(sceneModel.has("model"));
            } else {
                assertTrue(sceneModel.has("loader"), id + " inventory model lost its legacy scene");
                assertTrue(sceneModel.has("model"), id + " inventory model lost its source model");
                assertEquals(UNMOUNTED_TEXTURE,
                        sceneModel.getAsJsonObject("textures").get("particle").getAsString(), id);
            }
        }
    }

    private static JsonObject read(String relativePath) throws Exception {
        try (Reader reader = Files.newBufferedReader(
                ASSET_ROOT.resolve(relativePath), StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
