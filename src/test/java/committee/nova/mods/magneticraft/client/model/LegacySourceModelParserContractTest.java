package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacySourceModelParserContractTest {
    private static final Path MANIFEST = Path.of("scripts/legacy_model_manifest.json");

    @Test
    void parsesEveryUniqueHistoricalMcxAndGltfSource() throws Exception {
        JsonObject manifest;
        try (Reader reader = Files.newBufferedReader(MANIFEST, StandardCharsets.UTF_8)) {
            manifest = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Path sourceRoot = Path.of("src/main/resources/assets/magneticraft");
        Map<String, String> sources = uniqueSources(manifest.getAsJsonArray("artifacts"));
        int mcxCount = 0;
        int gltfCount = 0;
        int gltfNodes = 0;
        int gltfPrimitiveInstances = 0;
        Set<ModelScene.Primitive> gltfReferencedPrimitives = Collections.newSetFromMap(new IdentityHashMap<>());
        int gltfAnimations = 0;
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path source = sourceRoot.resolve(entry.getKey());
            ModelScene scene;
            try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
                if ("mcx".equals(entry.getValue())) {
                    scene = McxModelParser.parse(source.toString(), reader);
                    mcxCount++;
                } else {
                    Path directory = source.getParent();
                    scene = GltfModelParser.parse(
                            source.toString(),
                            reader,
                            uri -> Files.newInputStream(directory.resolve(uri))
                    );
                    gltfCount++;
                    gltfNodes += scene.nodes().size();
                    gltfPrimitiveInstances += scene.nodes().stream().mapToInt(node -> node.primitives().size()).sum();
                    scene.nodes().forEach(node -> gltfReferencedPrimitives.addAll(node.primitives()));
                    gltfAnimations += scene.animations().size();
                }
            }
            assertFalse(scene.nodes().isEmpty(), () -> source + " contains no parsed nodes");
        }

        assertEquals(31, mcxCount);
        assertEquals(14, gltfCount);
        assertEquals(511, gltfNodes);
        assertEquals(354, gltfReferencedPrimitives.size());
        assertEquals(473, gltfPrimitiveInstances);
        assertEquals(15, gltfAnimations);
    }

    private static Map<String, String> uniqueSources(JsonArray artifacts) {
        Map<String, String> result = new LinkedHashMap<>();
        artifacts.forEach(element -> {
            JsonObject artifact = element.getAsJsonObject();
            result.put(artifact.get("source").getAsString(), artifact.get("format").getAsString());
        });
        return result;
    }
}
