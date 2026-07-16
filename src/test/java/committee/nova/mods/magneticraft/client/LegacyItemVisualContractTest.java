package committee.nova.mods.magneticraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.computer.FloppyDiskVisualVariant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyItemVisualContractTest {
    private static final Path MANIFEST = Path.of(
            "src/test/resources/magneticraft/legacy/legacy_item_texture_manifest.json"
    );
    private static final Path RUNTIME_ASSETS = Path.of("src/main/resources/assets/magneticraft");
    private static final Path GENERATED_MODELS =
            Path.of("src/generated/resources/assets/magneticraft/models/item");

    @Test
    void restoredTexturesAreByteIdenticalToThePinnedOriginalSnapshot() throws Exception {
        JsonObject manifest = readJson(MANIFEST);
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals("Magneticraft-Team/Magneticraft", manifest.get("source_repository").getAsString());
        assertEquals("4108ca9bb332d11965c30e0c592b310d0858f251", manifest.get("source_commit").getAsString());

        Set<String> runtimePaths = new HashSet<>();
        JsonArray artifacts = manifest.getAsJsonArray("artifacts");
        assertEquals(12, artifacts.size());
        for (JsonElement element : artifacts) {
            JsonObject artifact = element.getAsJsonObject();
            String source = artifact.get("source").getAsString();
            String runtime = artifact.get("runtime").getAsString();
            assertTrue(source.startsWith("textures/items/"), source);
            assertTrue(runtimePaths.add(runtime), runtime);
            Path runtimeFile = RUNTIME_ASSETS.resolve(runtime);
            assertTrue(Files.isRegularFile(runtimeFile), runtime);
            assertEquals(artifact.get("sha256").getAsString(), sha256(runtimeFile), runtime);
        }
    }

    @Test
    void repairedItemsUseTheOriginalGeneratedTransformAndMagneticraftTextures() throws Exception {
        assertGeneratedModel("guide_book", "magneticraft:item/guide_book");
        assertGeneratedModel("inserter_speed_upgrade", "magneticraft:item/inserter_speed_upgrade");
        assertGeneratedModel("inserter_stack_upgrade", "magneticraft:item/inserter_stack_upgrade");
        assertGeneratedModel("wrench", "magneticraft:item/wrench");
        assertGeneratedModel("copper_wire_coil", "magneticraft:item/copper_wire_coil");
        assertGeneratedModel("electric_drill", "magneticraft:item/electric_drill");
        assertGeneratedModel("electric_chainsaw", "magneticraft:item/electric_chainsaw");
        assertGeneratedModel("electric_piston", "magneticraft:item/electric_piston");
        assertGeneratedModel("voltmeter", "magneticraft:item/voltmeter");
        assertGeneratedModel("thermometer", "magneticraft:item/thermometer");
    }

    @Test
    void floppyPresetModelsPreserveAllSevenOriginalVariants() throws Exception {
        JsonObject root = readJson(GENERATED_MODELS.resolve("floppy_disk.json"));
        assertEquals("minecraft:item/generated", root.get("parent").getAsString());
        assertEquals("magneticraft:item/floppy_disk_0", layerZero(root));

        JsonArray overrides = root.getAsJsonArray("overrides");
        assertEquals(6, overrides.size());
        int overrideIndex = 0;
        for (FloppyDiskVisualVariant variant : FloppyDiskVisualVariant.values()) {
            if (variant == FloppyDiskVisualVariant.USER) {
                continue;
            }
            JsonObject override = overrides.get(overrideIndex++).getAsJsonObject();
            assertEquals(
                    variant.textureIndex(),
                    override.getAsJsonObject("predicate").get("magneticraft:floppy_variant").getAsInt()
            );
            assertEquals(
                    "magneticraft:item/floppy_disk_" + variant.preset(),
                    override.get("model").getAsString()
            );
            assertGeneratedModel(
                    "floppy_disk_" + variant.preset(),
                    "magneticraft:item/floppy_disk_" + variant.textureIndex()
            );
        }
    }

    private static void assertGeneratedModel(String model, String texture) throws Exception {
        JsonObject json = readJson(GENERATED_MODELS.resolve(model + ".json"));
        assertEquals("minecraft:item/generated", json.get("parent").getAsString(), model);
        assertEquals(texture, layerZero(json), model);
    }

    private static String layerZero(JsonObject model) {
        return model.getAsJsonObject("textures").get("layer0").getAsString();
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest);
    }

    private static JsonObject readJson(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
