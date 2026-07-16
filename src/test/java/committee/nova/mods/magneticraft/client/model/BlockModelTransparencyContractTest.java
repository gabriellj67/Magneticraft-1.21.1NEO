package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockModelTransparencyContractTest {
    private static final Path GENERATED_ASSETS = Path.of("src/generated/resources/assets");
    private static final Path SOURCE_ASSETS = Path.of("src/main/resources/assets");
    private static final Path BLOCKSTATES = GENERATED_ASSETS.resolve("magneticraft/blockstates");
    private static final List<Path> ASSET_ROOTS = List.of(GENERATED_ASSETS, SOURCE_ASSETS);

    @Test
    void everyGeneratedBlockModelUsesASafeLayerForItsTransparentTexels() throws Exception {
        List<Path> blockstates;
        try (Stream<Path> files = Files.list(BLOCKSTATES)) {
            blockstates = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        assertEquals(106, blockstates.size(), "all registered blocks must remain in the generated model audit");

        Set<String> rootModels = new TreeSet<>();
        for (Path blockstate : blockstates) {
            collectModelReferences(readJson(blockstate), rootModels);
        }
        assertFalse(rootModels.isEmpty());

        Set<String> auditedModels = new TreeSet<>();
        Set<String> auditedSources = new TreeSet<>();
        Map<Path, AlphaSummary> textureAlpha = new HashMap<>();
        Set<String> errors = new TreeSet<>();
        for (String model : rootModels) {
            auditModel(model, auditedModels, auditedSources, textureAlpha, errors);
        }
        auditAllPackagedScenes(auditedSources, textureAlpha, errors);

        assertTrue(auditedModels.size() >= blockstates.size(), "multipart and variant models must be included");
        assertEquals(65, auditedSources.size(), "every packaged MCX/glTF source must remain in the audit");
        assertTrue(errors.isEmpty(), () -> "unsafe block-model transparency:\n" + String.join("\n", errors));
    }

    private static void collectModelReferences(JsonElement element, Set<String> models) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement model = object.get("model");
            if (model != null && model.isJsonPrimitive() && model.getAsJsonPrimitive().isString()) {
                models.add(normalizeId(model.getAsString()));
            }
            object.entrySet().forEach(entry -> collectModelReferences(entry.getValue(), models));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectModelReferences(child, models));
        }
    }

    private static void auditModel(
            String modelId,
            Set<String> auditedModels,
            Set<String> auditedSources,
            Map<Path, AlphaSummary> textureAlpha,
            Set<String> errors
    ) throws Exception {
        String normalized = normalizeId(modelId);
        if (!normalized.startsWith("magneticraft:") || !auditedModels.add(normalized)) {
            return;
        }

        List<ModelFile> chain = modelChain(normalized, errors);
        if (chain.isEmpty()) {
            return;
        }
        ModelFile loaderModel = chain.stream()
                .filter(model -> model.json().has("loader"))
                .findFirst()
                .orElse(null);
        if (loaderModel != null) {
            auditLegacyScene(loaderModel, auditedSources, textureAlpha, errors);
            return;
        }

        Map<String, String> textures = mergedTextures(chain);
        Set<String> usedTextures = usedTextures(normalized, chain, textures, errors);
        int requiredLayer = 0;
        for (String textureId : usedTextures) {
            Path texture = findTexture(textureId);
            if (texture == null) {
                if (normalizeId(textureId).startsWith("magneticraft:")) {
                    errors.add(normalized + " references missing texture " + textureId);
                }
                continue;
            }
            AlphaSummary alpha = textureAlpha.computeIfAbsent(texture, BlockModelTransparencyContractTest::readAlpha);
            requiredLayer = Math.max(requiredLayer, alpha.transparent() ? 1 : 0);
        }

        String renderType = chain.stream()
                .map(ModelFile::json)
                .filter(json -> json.has("render_type"))
                .map(json -> json.get("render_type").getAsString())
                .findFirst()
                .orElse("minecraft:solid");
        if (layerRank(renderType) < requiredLayer) {
            errors.add(normalized + " requires " + layerName(requiredLayer) + " but uses " + renderType);
        }
    }

    private static void auditLegacyScene(
            ModelFile wrapper,
            Set<String> auditedSources,
            Map<Path, AlphaSummary> textureAlpha,
            Set<String> errors
    ) throws Exception {
        JsonObject json = wrapper.json();
        if (!"magneticraft:legacy_scene".equals(json.get("loader").getAsString())) {
            errors.add(wrapper.id() + " uses unsupported custom loader " + json.get("loader").getAsString());
            return;
        }
        String sourceId = normalizeId(json.get("model").getAsString());
        Path source = findResource(sourceId);
        if (source == null) {
            errors.add(wrapper.id() + " references missing scene " + sourceId);
            return;
        }
        auditedSources.add(sourceId);
        ModelScene scene = parseScene(sourceId, source, errors);
        if (scene == null) {
            return;
        }

        int requiredLayer = alphaModeRank(scene.alphaMode());
        if (json.has("render_type") && layerRank(json.get("render_type").getAsString()) < requiredLayer) {
            errors.add(wrapper.id() + " overrides " + scene.alphaMode() + " with " + json.get("render_type"));
        }
        auditSceneTextures(sourceId, source, scene, textureAlpha, errors);
    }

    private static void auditAllPackagedScenes(
            Set<String> auditedSources,
            Map<Path, AlphaSummary> textureAlpha,
            Set<String> errors
    ) throws Exception {
        Path namespaceRoot = SOURCE_ASSETS.resolve("magneticraft");
        Path sceneRoot = namespaceRoot.resolve("models/block");
        List<Path> sources;
        try (Stream<Path> files = Files.walk(sceneRoot)) {
            sources = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".mcx") || path.toString().endsWith(".gltf"))
                    .sorted()
                    .toList();
        }
        for (Path source : sources) {
            String sourceId = "magneticraft:" + namespaceRoot.relativize(source).toString().replace('\\', '/');
            if (!auditedSources.add(sourceId)) {
                continue;
            }
            ModelScene scene = parseScene(sourceId, source, errors);
            if (scene != null) {
                auditSceneTextures(sourceId, source, scene, textureAlpha, errors);
            }
        }
    }

    private static ModelScene parseScene(String sourceId, Path source, Set<String> errors) throws Exception {
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            if (sourceId.endsWith(".mcx")) {
                return McxModelParser.parse(source.toString(), reader);
            }
            if (sourceId.endsWith(".gltf")) {
                return GltfModelParser.parse(
                        source.toString(),
                        reader,
                        uri -> Files.newInputStream(source.getParent().resolve(uri))
                );
            }
        }
        errors.add("unsupported packaged scene " + sourceId);
        return null;
    }

    private static void auditSceneTextures(
            String sourceId,
            Path source,
            ModelScene scene,
            Map<Path, AlphaSummary> textureAlpha,
            Set<String> errors
    ) {
        for (ModelScene.Node node : scene.nodes()) {
            for (ModelScene.Primitive primitive : node.primitives()) {
                Path texture = findSceneTexture(primitive.texture(), source);
                if (texture == null) {
                    if (!primitive.texture().startsWith("data:")
                            && (!primitive.texture().contains(":")
                            || normalizeId(primitive.texture()).startsWith("magneticraft:"))) {
                        errors.add(sourceId + " references missing texture " + primitive.texture());
                    }
                    continue;
                }
                AlphaSummary alpha = textureAlpha.computeIfAbsent(
                        texture,
                        BlockModelTransparencyContractTest::readAlpha
                );
                if (primitive.alphaMode() == ModelScene.AlphaMode.OPAQUE && alpha.transparent()) {
                    errors.add(sourceId + " uses OPAQUE for transparent texture " + primitive.texture());
                }
            }
        }
    }

    private static List<ModelFile> modelChain(String rootId, Set<String> errors) throws IOException {
        List<ModelFile> chain = new ArrayList<>();
        Set<String> visited = new TreeSet<>();
        String current = rootId;
        while (current.startsWith("magneticraft:") && visited.add(current)) {
            Path path = findModel(current);
            if (path == null) {
                errors.add(rootId + " has missing model parent " + current);
                break;
            }
            JsonObject json = readJson(path);
            chain.add(new ModelFile(current, json));
            current = json.has("parent") ? normalizeId(json.get("parent").getAsString()) : "";
        }
        if (!current.isEmpty() && current.startsWith("magneticraft:") && !visited.add(current)) {
            errors.add(rootId + " has a cyclic model parent at " + current);
        }
        return chain;
    }

    private static Map<String, String> mergedTextures(List<ModelFile> chain) {
        Map<String, String> textures = new LinkedHashMap<>();
        for (int index = chain.size() - 1; index >= 0; index--) {
            JsonObject json = chain.get(index).json();
            if (json.has("textures")) {
                json.getAsJsonObject("textures").entrySet().forEach(entry ->
                        textures.put(entry.getKey(), entry.getValue().getAsString())
                );
            }
        }
        return textures;
    }

    private static Set<String> usedTextures(
            String modelId,
            List<ModelFile> chain,
            Map<String, String> textures,
            Set<String> errors
    ) {
        Set<String> used = new TreeSet<>();
        for (ModelFile model : chain) {
            JsonObject json = model.json();
            if (!json.has("elements")) {
                continue;
            }
            json.getAsJsonArray("elements").forEach(element -> {
                JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                faces.entrySet().forEach(face -> {
                    JsonObject definition = face.getValue().getAsJsonObject();
                    if (definition.has("texture")) {
                        String reference = definition.get("texture").getAsString();
                        String resolved = resolveTexture(reference, textures);
                        if (resolved != null) {
                            used.add(resolved);
                        } else {
                            errors.add(modelId + " has unresolved texture reference " + reference);
                        }
                    }
                });
            });
        }
        if (used.isEmpty()) {
            String vanillaParent = chain.stream()
                    .map(ModelFile::json)
                    .filter(json -> json.has("parent"))
                    .map(json -> normalizeId(json.get("parent").getAsString()))
                    .filter(parent -> parent.startsWith("minecraft:") && !parent.equals("minecraft:block/block"))
                    .findFirst()
                    .orElse(null);
            if (vanillaParent != null) {
                textures.values().stream()
                        .filter(value -> !value.startsWith("#"))
                        .forEach(used::add);
            }
        }
        return used;
    }

    private static String resolveTexture(String reference, Map<String, String> textures) {
        String current = reference;
        Set<String> visited = new TreeSet<>();
        while (current.startsWith("#") && visited.add(current)) {
            current = textures.get(current.substring(1));
            if (current == null) {
                return null;
            }
        }
        return current.startsWith("#") ? null : current;
    }

    private static Path findModel(String id) {
        String normalized = normalizeId(id);
        int separator = normalized.indexOf(':');
        String namespace = normalized.substring(0, separator);
        String path = normalized.substring(separator + 1);
        for (Path root : ASSET_ROOTS) {
            Path candidate = root.resolve(namespace).resolve("models").resolve(path + ".json");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path findResource(String id) {
        String normalized = normalizeId(id);
        int separator = normalized.indexOf(':');
        String namespace = normalized.substring(0, separator);
        String path = normalized.substring(separator + 1);
        for (Path root : ASSET_ROOTS) {
            Path candidate = root.resolve(namespace).resolve(path);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path findTexture(String id) {
        String normalized = normalizeId(id);
        int separator = normalized.indexOf(':');
        String namespace = normalized.substring(0, separator);
        String path = normalized.substring(separator + 1);
        for (Path root : ASSET_ROOTS) {
            Path candidate = root.resolve(namespace).resolve("textures").resolve(path + ".png");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path findSceneTexture(String id, Path source) {
        if (id.startsWith("data:")) {
            return null;
        }
        if (!id.contains(":")) {
            Path relative = source.getParent().resolve(id);
            return Files.isRegularFile(relative) ? relative : null;
        }
        return findTexture(id);
    }

    private static AlphaSummary readAlpha(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IOException("unreadable image");
            }
            boolean transparent = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    transparent |= alpha < 0xFF;
                }
            }
            return new AlphaSummary(transparent);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect texture " + path, exception);
        }
    }

    private static int alphaModeRank(ModelScene.AlphaMode alphaMode) {
        return switch (alphaMode) {
            case OPAQUE -> 0;
            case MASK -> 1;
            case BLEND -> 2;
        };
    }

    private static int layerRank(String renderType) {
        return switch (renderType) {
            case "minecraft:translucent" -> 2;
            case "minecraft:cutout", "minecraft:cutout_mipped" -> 1;
            default -> 0;
        };
    }

    private static String layerName(int rank) {
        return switch (rank) {
            case 2 -> "minecraft:translucent";
            case 1 -> "minecraft:cutout";
            default -> "minecraft:solid";
        };
    }

    private static String normalizeId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private record ModelFile(String id, JsonObject json) {
    }

    private record AlphaSummary(boolean transparent) {
    }
}
