package committee.nova.mods.magneticraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyVisualAssetContractTest {
    private static final Path MANIFEST = Path.of(
            "src/test/resources/magneticraft/legacy/legacy_model_manifest.json"
    );
    private static final Path RUNTIME_ASSETS = Path.of("src/main/resources/assets/magneticraft");
    private static final Set<String> ROLES = Set.of(
            "static_block",
            "static_structure",
            "inventory_model",
            "dynamic_part",
            "reference_baked"
    );

    @Test
    void manifestPinsHistoricalSnapshotAndEverySourceIsPackagedInItsOriginalFormat() throws IOException {
        JsonObject manifest = readJson(MANIFEST);
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals("4108ca9bb332d11965c30e0c592b310d0858f251", manifest.get("source_commit").getAsString());
        assertEquals(
                "bc3937d85016780665a0fad9f4657da90c5e8747a44d379b2510c0d21f99711b",
                manifest.get("source_tree_sha256").getAsString()
        );

        Set<String> identifiers = new HashSet<>();
        Set<String> sources = new HashSet<>();
        for (JsonElement element : manifest.getAsJsonArray("artifacts")) {
            JsonObject artifact = element.getAsJsonObject();
            String identifier = artifact.get("id").getAsString();
            String source = artifact.get("source").getAsString();
            assertTrue(identifiers.add(identifier), identifier);
            assertTrue(ROLES.contains(artifact.get("role").getAsString()), identifier);
            assertTrue(Set.of("mcx", "gltf").contains(artifact.get("format").getAsString()), identifier);
            assertTrue(Files.isRegularFile(RUNTIME_ASSETS.resolve(source)), source);
            sources.add(source);
            if (source.endsWith(".gltf")) {
                assertTrue(Files.isRegularFile(RUNTIME_ASSETS.resolve(source.replace(".gltf", ".bin"))), source);
            }
        }
        assertEquals(61, identifiers.size(), "历史视觉清单不应被静默缩减");
        assertEquals(47, sources.size(), "共享源模型应保持去重后的固定数量");
    }

    @Test
    void fullOriginalBlockModelDirectoriesAreRetainedBeyondTheInitialConversionManifest() throws IOException {
        JsonArray exclusions = readJson(MANIFEST).getAsJsonArray("excluded_sources");
        assertEquals(8, exclusions.size());
        for (JsonElement element : exclusions) {
            String source = element.getAsJsonObject().get("source").getAsString();
            if (source.startsWith("models/block/")) {
                assertTrue(Files.isRegularFile(RUNTIME_ASSETS.resolve(source)), source);
            }
        }
    }

    @Test
    void runtimeResourcesContainMcxGltfAndBuffersButNoUnsupportedGlb() throws IOException {
        int mcx = 0;
        int gltf = 0;
        int gltfBuffers = 0;
        try (Stream<Path> resources = Files.walk(RUNTIME_ASSETS.resolve("models/block"))) {
            for (Path path : resources.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                assertFalse(name.endsWith(".glb"), path.toString());
                mcx += name.endsWith(".mcx") ? 1 : 0;
                gltf += name.endsWith(".gltf") ? 1 : 0;
                gltfBuffers += name.endsWith(".bin") ? 1 : 0;
            }
        }
        assertEquals(40, mcx);
        assertEquals(27, gltf);
        assertEquals(20, gltfBuffers);
    }

    @Test
    void generatedStaticModelsUseTheRuntimeSceneLoader() throws IOException {
        assertRuntimeModel("battery_box", "magneticraft:models/block/mcx/battery.mcx");
        assertRuntimeModel("pneumatic_tube", "magneticraft:models/block/gltf/pneumatic_tube_inv.gltf");
        assertRuntimeModel("computer", "magneticraft:models/block/mcx/computer.mcx");
        assertRuntimeModel("sluice_box_inventory", "magneticraft:models/block/mcx/sluice_box_inv.mcx");
        assertRuntimeModel("feeding_trough_inventory", "magneticraft:models/block/mcx/feeding_trough_inv.mcx");
        assertRuntimeModel("small_tank_inventory", "magneticraft:models/block/mcx/small_tank.mcx");
        assertRuntimeModel("combustion_chamber_inventory", "magneticraft:models/block/mcx/combustion_chamber.mcx");
        assertRuntimeModel("steam_boiler_inventory", "magneticraft:models/block/mcx/steam_boiler.mcx");
        assertRuntimeModel("gasification_unit_inventory", "magneticraft:models/block/mcx/gasification_unit.mcx");
        assertRuntimeModel("inserter_inventory", "magneticraft:models/block/gltf/inserter.gltf");
        assertRuntimeModel("electric_engine_inventory", "magneticraft:models/block/gltf/electric_engine.gltf");
        assertRuntimeModel("box_transformer", "magneticraft:models/block/gltf/box_transformer.gltf");
        assertRuntimeModel("fuse_box", "magneticraft:models/block/gltf/fuse_box.gltf");
        assertRuntimeModel("circuit_breaker", "magneticraft:models/block/gltf/circuit_breaker.gltf");
        assertRuntimeModel("burnt_electric_cable", "magneticraft:models/block/gltf/burnt_electric_cable.gltf");
    }

    private static void assertRuntimeModel(String name, String source) throws IOException {
        Path path = Path.of("src/generated/resources/assets/magneticraft/models/block/" + name + ".json");
        JsonObject model = readJson(path);
        assertEquals("magneticraft:legacy_scene", model.get("loader").getAsString(), path.toString());
        assertEquals(source, model.get("model").getAsString(), path.toString());
        assertTrue(model.has("display"), path.toString());
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
