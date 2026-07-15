package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalRuntimeModelContractTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/assets/magneticraft/models/block/gltf"
    );

    @Test
    void electricalGltfModelsAreStandardBoundedAndHaveRequiredNamedNodes() throws Exception {
        assertModel("box_transformer", Set.of("housing", "tier_band", "arrow_forward", "arrow_reverse"), true);
        assertModel("fuse_box", Set.of("housing", "tier_band", "fuse_intact", "fuse_blown"), true);
        assertModel("circuit_breaker", Set.of("housing", "tier_band", "switch_closed", "switch_open"), true);
        assertModel("burnt_electric_cable", Set.of("charred_core", "stub_north", "stub_south"), false);
    }

    private static void assertModel(String name, Set<String> requiredNodes, boolean hasManifest) throws Exception {
        Path source = ROOT.resolve(name + ".gltf");
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        assertFalse(json.has("magneticraft"), () -> name + " adds a private glTF root field");
        assertFalse(json.has("extras"), () -> name + " stores renderer metadata inside glTF extras");

        ModelScene scene;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            scene = GltfModelParser.parse(source.toString(), reader, uri -> {
                throw new AssertionError("electrical glTF buffers must remain embedded");
            });
        }
        Set<String> names = new HashSet<>();
        scene.nodes().forEach(node -> names.add(node.name()));
        assertTrue(names.containsAll(requiredNodes), () -> name + " is missing named state nodes");
        Set<Integer> allNodes = IntStream.range(0, scene.nodes().size()).collect(
                HashSet::new,
                HashSet::add,
                HashSet::addAll
        );
        ModelSceneBounds bounds = ModelSceneBounds.calculate(scene, allNodes);
        assertTrue(bounds.minX() >= -0.001F && bounds.minY() >= -0.001F && bounds.minZ() >= -0.001F);
        assertTrue(bounds.maxX() <= 1.001F && bounds.maxY() <= 1.001F && bounds.maxZ() <= 1.001F);
        assertTrue(bounds.largestSpan() <= 1.001F);

        Path manifestPath = ROOT.resolve(name + ".manifest.json");
        assertEquals(hasManifest, Files.exists(manifestPath));
        if (hasManifest) {
            try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
                ModelRenderManifest manifest = ModelRenderManifest.parse(manifestPath.toString(), reader);
                assertEquals(0, manifest.tintIndex("tier_band", "tier_band"));
            }
        }
    }
}
