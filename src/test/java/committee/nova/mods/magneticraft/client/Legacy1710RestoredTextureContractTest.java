package committee.nova.mods.magneticraft.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Legacy1710RestoredTextureContractTest {
    private static final Path MANIFEST = Path.of(
            "src/test/resources/magneticraft/legacy/legacy_1710_restored_texture_manifest.json"
    );
    private static final Path RUNTIME_ASSETS = Path.of("src/main/resources/assets/magneticraft");

    @Test
    void restoredMaterialTexturesRemainByteIdenticalToTheReviewedLegacySnapshot() throws Exception {
        JsonObject manifest = JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals("ipl-adm/magneticraft-legacy-1.7.10", manifest.get("source_repository").getAsString());
        assertEquals("master", manifest.get("source_ref").getAsString());
        assertEquals("magneticraft-0.6.0-final", manifest.get("release_file").getAsString());

        Set<String> runtimePaths = new HashSet<>();
        assertEquals(8, manifest.getAsJsonArray("artifacts").size());
        for (JsonElement element : manifest.getAsJsonArray("artifacts")) {
            JsonObject artifact = element.getAsJsonObject();
            String source = artifact.get("source").getAsString();
            String runtime = artifact.get("runtime").getAsString();
            assertTrue(source.startsWith("textures/"), source);
            assertTrue(runtimePaths.add(runtime), runtime);
            Path runtimeFile = RUNTIME_ASSETS.resolve(runtime);
            assertTrue(Files.isRegularFile(runtimeFile), runtime);
            assertEquals(artifact.get("sha256").getAsString(), sha256(runtimeFile), runtime);
        }
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest);
    }
}
