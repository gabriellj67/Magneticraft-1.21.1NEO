package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reload-aware lazy cache for model sidecar manifests. */
public final class ModelRenderManifestRegistry implements ResourceManagerReloadListener {
    public static final ModelRenderManifestRegistry INSTANCE = new ModelRenderManifestRegistry();

    private volatile CacheState state;

    private ModelRenderManifestRegistry() {
    }

    public ModelRenderManifest load(ResourceLocation source) {
        CacheState current = currentState();
        try {
            return current.manifests.computeIfAbsent(source, location -> parse(current.manager, location));
        } catch (UncheckedManifestException exception) {
            throw new JsonParseException(
                    "Unable to load model render manifest for " + source + ": " + exception.getCause().getMessage(),
                    exception
            );
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        state = new CacheState(resourceManager, new ConcurrentHashMap<>());
    }

    static ResourceLocation manifestLocation(ResourceLocation source) {
        String path = source.getPath();
        int dot = path.lastIndexOf('.');
        String manifestPath = (dot < 0 ? path : path.substring(0, dot)) + ".manifest.json";
        return ResourceLocation.fromNamespaceAndPath(source.getNamespace(), manifestPath);
    }

    private CacheState currentState() {
        CacheState current = state;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = state;
            if (current == null) {
                current = new CacheState(Minecraft.getInstance().getResourceManager(), new ConcurrentHashMap<>());
                state = current;
            }
        }
        return current;
    }

    private static ModelRenderManifest parse(ResourceManager manager, ResourceLocation source) {
        ResourceLocation manifestLocation = manifestLocation(source);
        Resource resource = manager.getResource(manifestLocation).orElse(null);
        if (resource == null) {
            return ModelRenderManifest.EMPTY;
        }
        try (Reader reader = resource.openAsReader()) {
            return ModelRenderManifest.parse(manifestLocation.toString(), reader);
        } catch (IOException | ModelParseException exception) {
            throw new UncheckedManifestException(exception);
        }
    }

    private record CacheState(ResourceManager manager, Map<ResourceLocation, ModelRenderManifest> manifests) {
    }

    private static final class UncheckedManifestException extends RuntimeException {
        private UncheckedManifestException(Exception cause) {
            super(cause);
        }

        @Override
        public synchronized Exception getCause() {
            return (Exception) super.getCause();
        }
    }
}
