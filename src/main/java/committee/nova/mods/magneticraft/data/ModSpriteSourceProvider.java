package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;

import java.util.Optional;

/**
 * Adds legacy fluid textures to Minecraft's block atlas without relocating them.
 */
final class ModSpriteSourceProvider extends SpriteSourceProvider {
    ModSpriteSourceProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper, Magneticraft.MOD_ID);
    }

    @Override
    protected void addSources() {
        atlas(BLOCKS_ATLAS).addSource(new DirectoryLister("blocks", "blocks/"));
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
