package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.client.model.GltfModelParser;
import committee.nova.mods.magneticraft.client.model.McxModelParser;
import committee.nova.mods.magneticraft.client.model.ModelParseException;
import committee.nova.mods.magneticraft.client.model.ModelScene;
import committee.nova.mods.magneticraft.client.model.ModelSceneBounds;
import committee.nova.mods.magneticraft.client.model.ModelSceneSelection;
import committee.nova.mods.magneticraft.client.model.ModelTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.io.Reader;

/** Computes the same GUI fit transform directly from selected MCX/glTF source geometry. */
final class LegacyInventoryTransform {
    private LegacyInventoryTransform() {
    }

    static InventoryModelTransform load(
            ExistingFileHelper existingFileHelper,
            ResourceLocation source,
            ModelSceneSelection selection,
            ModelTransform sourceTransform
    ) {
        try (Reader reader = existingFileHelper
                .getResource(source, PackType.CLIENT_RESOURCES)
                .openAsReader()) {
            ModelScene scene;
            if (source.getPath().endsWith(".mcx")) {
                scene = McxModelParser.parse(source.toString(), reader);
            } else if (source.getPath().endsWith(".gltf")) {
                scene = GltfModelParser.parse(
                        source.toString(),
                        reader,
                        uri -> existingFileHelper
                                .getResource(resolveRelative(source, uri), PackType.CLIENT_RESOURCES)
                                .open()
                );
            } else {
                throw new IllegalArgumentException("Unsupported legacy model source " + source);
            }
            ModelSceneBounds bounds = ModelSceneBounds.calculate(scene, selection.select(scene), sourceTransform);
            return InventoryModelTransform.fromBounds(
                    bounds.minX(),
                    bounds.minY(),
                    bounds.minZ(),
                    bounds.maxX(),
                    bounds.maxY(),
                    bounds.maxZ()
            );
        } catch (IOException | ModelParseException exception) {
            throw new IllegalStateException("Unable to read legacy model " + source, exception);
        }
    }

    private static ResourceLocation resolveRelative(ResourceLocation source, String uri) {
        if (uri.indexOf(':') >= 0) {
            ResourceLocation absolute = ResourceLocation.tryParse(uri);
            if (absolute == null) {
                throw new IllegalArgumentException("Invalid model resource URI " + uri);
            }
            return absolute;
        }
        int slash = source.getPath().lastIndexOf('/');
        String base = slash < 0 ? "" : source.getPath().substring(0, slash + 1);
        return ResourceLocation.fromNamespaceAndPath(source.getNamespace(), base + uri);
    }
}
