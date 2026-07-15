package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalRuntimeModelContractTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/assets/magneticraft/models/block/gltf"
    );
    private static final Path TEXTURES = Path.of(
            "src/main/resources/assets/magneticraft/textures"
    );

    @Test
    void electricalGltfModelsAreStandardBoundedAndHaveRequiredNamedNodes() throws Exception {
        assertModel(
                "box_transformer",
                Set.of("housing", "tier_band", "input_terminal", "output_terminal", "arrow_forward", "arrow_reverse"),
                true
        );
        assertModel(
                "fuse_box",
                Set.of("housing", "tier_band", "input_terminal", "output_terminal", "fuse_intact", "fuse_blown"),
                true
        );
        assertModel(
                "circuit_breaker",
                Set.of("housing", "tier_band", "input_terminal", "output_terminal", "switch_closed", "switch_open"),
                true
        );
        assertModel("burnt_electric_cable", Set.of("charred_core", "stub_north", "stub_south"), false);
    }

    @Test
    void electricalProtectionAndTransformerTerminalsMatchTheirFrontBackTopology() throws Exception {
        assertOpposedTerminals("box_transformer");
        assertOpposedTerminals("fuse_box");
        assertOpposedTerminals("circuit_breaker");
    }

    @Test
    void transformerDirectionIndicatorsPointInOppositeDirections() throws Exception {
        ModelScene scene = readScene("box_transformer");
        ModelSceneBounds forward = ModelSceneBounds.calculate(
                scene,
                new ModelSceneSelection(Set.of(), Set.of("arrow_forward"), Set.of(), Set.of()).select(scene)
        );
        ModelSceneBounds reverse = ModelSceneBounds.calculate(
                scene,
                new ModelSceneSelection(Set.of(), Set.of("arrow_reverse"), Set.of(), Set.of()).select(scene)
        );

        assertTrue(forward.maxX() >= 0.699F, "forward arrow must reach its right-facing point");
        assertTrue(reverse.minX() <= 0.301F, "reverse arrow must reach its left-facing point");
        assertTrue(forward.minX() > reverse.minX(), "direction indicators must not overlap exactly");
        assertTrue(forward.maxX() > reverse.maxX(), "direction indicators must have opposed extents");
    }

    @Test
    void burntCableStubsStayInsideTheBlockAndCannotLookConnectedToNeighbours() throws Exception {
        ModelScene scene = readScene("burnt_electric_cable");
        Set<Integer> stubs = scene.nodes().stream()
                .filter(node -> node.name() != null && node.name().startsWith("stub_"))
                .map(ModelScene.Node::index)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        ModelSceneBounds bounds = ModelSceneBounds.calculate(scene, stubs);

        assertTrue(bounds.minX() > 0.1F && bounds.minY() > 0.1F && bounds.minZ() > 0.1F);
        assertTrue(bounds.maxX() < 0.9F && bounds.maxY() < 0.9F && bounds.maxZ() < 0.9F);
    }

    private static void assertModel(String name, Set<String> requiredNodes, boolean hasManifest) throws Exception {
        Path source = ROOT.resolve(name + ".gltf");
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        assertFalse(json.has("magneticraft"), () -> name + " adds a private glTF root field");
        assertFalse(json.has("extras"), () -> name + " stores renderer metadata inside glTF extras");
        assertDedicatedTextures(name, json);

        ModelScene scene = readScene(name);
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

    private static void assertOpposedTerminals(String name) throws Exception {
        ModelScene scene = readScene(name);
        ModelSceneBounds input = boundsOf(scene, "input_terminal");
        ModelSceneBounds output = boundsOf(scene, "output_terminal");

        assertEquals(1.0F, input.maxZ(), 0.001F, name + " input must touch the rear face");
        assertEquals(0.0F, output.minZ(), 0.001F, name + " output must touch the front face");
        assertTrue(input.minY() > 0.2F, name + " input must not be placed on the floor");
        assertTrue(output.minY() > 0.2F, name + " output must not be placed on the floor");
    }

    private static ModelSceneBounds boundsOf(ModelScene scene, String nodeName) {
        int index = scene.nodes().stream()
                .filter(node -> nodeName.equals(node.name()))
                .mapToInt(ModelScene.Node::index)
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing node " + nodeName));
        return ModelSceneBounds.calculate(scene, Set.of(index));
    }

    private static void assertDedicatedTextures(String modelName, JsonObject json) throws Exception {
        for (var image : json.getAsJsonArray("images")) {
            String uri = image.getAsJsonObject().get("uri").getAsString();
            assertTrue(
                    uri.startsWith("magneticraft:block/"),
                    () -> modelName + " still references a vanilla or external texture: " + uri
            );
            String relative = uri.substring("magneticraft:".length()) + ".png";
            Path texture = TEXTURES.resolve(relative);
            assertTrue(Files.isRegularFile(texture), texture.toString());
            BufferedImage imageFile = ImageIO.read(texture.toFile());
            assertNotNull(imageFile, texture.toString());
            assertEquals(16, imageFile.getWidth(), texture.toString());
            assertEquals(16, imageFile.getHeight(), texture.toString());
        }
    }

    private static ModelScene readScene(String name) throws Exception {
        Path source = ROOT.resolve(name + ".gltf");
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return GltfModelParser.parse(source.toString(), reader, uri -> {
                throw new AssertionError("electrical glTF buffers must remain embedded");
            });
        }
    }
}
