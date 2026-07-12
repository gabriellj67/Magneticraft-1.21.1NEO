package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
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
        for (FluidDefinition definition : FluidDefinition.values()) {
            atlas(BLOCKS_ATLAS)
                    .addSource(single("fluid/" + definition.id() + "_still"))
                    .addSource(single("fluid/" + definition.id() + "_flow"));
        }
    }

    private static SingleFile single(String path) {
        return new SingleFile(Magneticraft.id(path), Optional.empty());
    }
}
