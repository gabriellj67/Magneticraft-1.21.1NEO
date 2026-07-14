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
            "src/generated/resources/assets/magneticraft/models"
    );

    @Test
    void worldControllerAndInventorySceneUseSeparateModels() throws Exception {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            String id = definition.id();
            JsonObject worldModel = read("block/" + id + ".json");
            assertEquals("minecraft:block/cube_all", worldModel.get("parent").getAsString(), id);
            assertFalse(worldModel.has("loader"), id + " world model must not obscure the hologram");

            JsonObject itemModel = read("item/" + id + ".json");
            assertEquals("magneticraft:block/" + id + "_item",
                    itemModel.get("parent").getAsString(), id);

            JsonObject sceneModel = read("block/" + id + "_item.json");
            assertTrue(sceneModel.has("loader"), id + " inventory model lost its legacy scene");
            assertTrue(sceneModel.has("model"), id + " inventory model lost its source model");
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
