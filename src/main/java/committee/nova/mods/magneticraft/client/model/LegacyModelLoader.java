package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Shared reload-aware source cache for baked and block-entity legacy scene rendering. */
public final class LegacyModelLoader implements IGeometryLoader<LegacySceneGeometry>, ResourceManagerReloadListener {
    public static final LegacyModelLoader INSTANCE = new LegacyModelLoader();

    private volatile CacheState state;

    private LegacyModelLoader() {
        state = new CacheState(Minecraft.getInstance().getResourceManager(), new ConcurrentHashMap<>());
    }

    @Override
    public LegacySceneGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
        JsonElement modelElement = json.get("model");
        if (modelElement == null || !modelElement.isJsonPrimitive() || !modelElement.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Legacy scene loader requires a string 'model' resource");
        }
        ResourceLocation source = ResourceLocation.tryParse(modelElement.getAsString());
        if (source == null) {
            throw new JsonParseException("Invalid legacy scene model resource: " + modelElement.getAsString());
        }
        ModelSceneSelection selection = ModelSceneSelection.of(
                strings(json, "include_nodes"),
                strings(json, "include_subtrees"),
                strings(json, "exclude_nodes"),
                strings(json, "exclude_subtrees")
        );
        float[] translation = floats(json, "translation", 3, new float[]{0.0F, 0.0F, 0.0F});
        ModelTransform sourceTransform = ModelTransform.IDENTITY.withTranslation(
                translation[0],
                translation[1],
                translation[2]
        );
        return new LegacySceneGeometry(
                load(source),
                selection,
                sourceTransform,
                ModelRenderManifestRegistry.INSTANCE.load(source)
        );
    }

    public ModelScene load(ResourceLocation source) {
        CacheState current = state;
        try {
            return current.models.computeIfAbsent(source, location -> parse(current.manager, location));
        } catch (UncheckedModelException exception) {
            throw new JsonParseException("Unable to load legacy scene " + source + ": " + exception.getCause().getMessage(), exception);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        state = new CacheState(resourceManager, new ConcurrentHashMap<>());
        LegacySceneRenderer.clearCaches();
    }

    private static ModelScene parse(ResourceManager manager, ResourceLocation source) {
        Resource resource = manager.getResource(source)
                .orElseThrow(() -> new UncheckedModelException(new IOException("resource not found")));
        try (Reader reader = resource.openAsReader()) {
            String path = source.getPath();
            if (path.endsWith(".mcx")) {
                return McxModelParser.parse(source.toString(), reader);
            }
            if (path.endsWith(".gltf")) {
                return GltfModelParser.parse(
                        source.toString(),
                        reader,
                        uri -> openRelative(manager, source, uri)
                );
            }
            throw new ModelParseException(source.toString(), "$", "expected .mcx or .gltf source");
        } catch (IOException | ModelParseException exception) {
            throw new UncheckedModelException(exception);
        }
    }

    private static InputStream openRelative(ResourceManager manager, ResourceLocation source, String uri)
            throws IOException {
        ResourceLocation target;
        if (uri.indexOf(':') >= 0) {
            target = ResourceLocation.tryParse(uri);
            if (target == null) {
                throw new IOException("invalid resource URI " + uri);
            }
        } else {
            int slash = source.getPath().lastIndexOf('/');
            String base = slash < 0 ? "" : source.getPath().substring(0, slash + 1);
            target = ResourceLocation.fromNamespaceAndPath(source.getNamespace(), normalize(base + uri));
        }
        return manager.getResource(target)
                .orElseThrow(() -> new IOException("resource not found: " + target))
                .open();
    }

    private static String normalize(String path) throws IOException {
        List<String> segments = new ArrayList<>();
        for (String segment : path.replace('\\', '/').split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new IOException("relative URI escapes the namespace root: " + path);
                }
                segments.remove(segments.size() - 1);
            } else {
                segments.add(segment);
            }
        }
        return String.join("/", segments);
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        JsonElement element = json.get(key);
        if (!element.isJsonArray()) {
            throw new JsonParseException("Legacy scene loader field '" + key + "' must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        List<String> values = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            JsonElement value = array.get(index);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Legacy scene loader field '" + key + "' must contain strings");
            }
            values.add(value.getAsString());
        }
        return List.copyOf(values);
    }

    private static float[] floats(JsonObject json, String key, int width, float[] fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
        if (!element.isJsonArray() || element.getAsJsonArray().size() != width) {
            throw new JsonParseException("Legacy scene loader field '" + key + "' must contain " + width + " numbers");
        }
        float[] values = new float[width];
        for (int index = 0; index < width; index++) {
            JsonElement value = element.getAsJsonArray().get(index);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new JsonParseException("Legacy scene loader field '" + key + "' must contain numbers");
            }
            values[index] = value.getAsFloat();
        }
        return values;
    }

    private record CacheState(ResourceManager manager, Map<ResourceLocation, ModelScene> models) {
    }

    private static final class UncheckedModelException extends RuntimeException {
        private UncheckedModelException(Exception cause) {
            super(cause);
        }

        @Override
        public synchronized Exception getCause() {
            return (Exception) super.getCause();
        }
    }
}
