package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Adds non-standard fluid textures to Minecraft's block atlas.
 */
final class ModSpriteSourceProvider extends SpriteSourceProvider {
    ModSpriteSourceProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void gather() {
        for (String textureId : java.util.Arrays.stream(FluidDefinition.values())
                .map(FluidDefinition::textureId)
                .distinct()
                .toList()) {
            atlas(BLOCKS_ATLAS)
                    .addSource(single("fluid/" + textureId + "_still"))
                    .addSource(single("fluid/" + textureId + "_flow"));
        }
    }

    private static SingleFile single(String path) {
        return new SingleFile(Magneticraft.id(path), Optional.empty());
    }
}
