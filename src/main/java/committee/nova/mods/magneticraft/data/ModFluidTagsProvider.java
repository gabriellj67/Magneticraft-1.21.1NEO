package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Generates shared-process, Magneticraft-owned and gaseous fluid tags.
 */
final class ModFluidTagsProvider extends FluidTagsProvider {
    ModFluidTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (FluidDefinition definition : FluidDefinition.values()) {
            ModFluids.FluidFamily family = ModFluids.get(definition);
            tag(ModTags.Fluids.magneticraft(definition)).add(family.source().get(), family.flowing().get());
            tag(ModTags.Fluids.forge(definition)).add(family.source().get(), family.flowing().get());
            if (definition.isGaseous()) {
                tag(Tags.Fluids.GASEOUS).add(family.source().get(), family.flowing().get());
            }
        }
    }
}
