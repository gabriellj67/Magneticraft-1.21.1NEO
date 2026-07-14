package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Path sourceRoot = Path.of(manifest.get("source_root").getAsString());
        Map<String, String> sources = uniqueSources(manifest.getAsJsonArray("artifacts"));
        int mcxCount = 0;
        int gltfCount = 0;
        int gltfNodes = 0;
        int gltfPrimitives = 0;
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
                    gltfPrimitives += scene.nodes().stream().mapToInt(node -> node.primitives().size()).sum();
                    gltfAnimations += scene.animations().size();
                }
            }
            assertFalse(scene.nodes().isEmpty(), () -> source + " contains no parsed nodes");
        }

        assertEquals(31, mcxCount);
        assertEquals(14, gltfCount);
        assertEquals(511, gltfNodes);
        assertEquals(355, gltfPrimitives);
        assertEquals(23, gltfAnimations);
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
