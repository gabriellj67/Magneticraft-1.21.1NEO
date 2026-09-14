package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.integration.tconstruct.TinkersConstructDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Registers only data providers that own resources in the current migration stage.
 */
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        if (event.includeClient()) {
            event.addProvider(new ModBlockStateProvider(output, existingFileHelper));
            event.addProvider(new ModItemModelProvider(output, existingFileHelper));
            event.addProvider(new ModSpriteSourceProvider(output, lookupProvider, existingFileHelper));
            event.addProvider(new ModLanguageProvider(output, "en_us", false));
            event.addProvider(new ModLanguageProvider(output, "zh_cn", true));
            event.addProvider(new AdvancedGuideDataProvider(output, AdvancedGuideDataProvider.computerOpcodes()));
        }

        if (event.includeServer()) {
            ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
            event.addProvider(blockTags);
            event.addProvider(new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
            event.addProvider(new ModFluidTagsProvider(output, lookupProvider, existingFileHelper));
            event.addProvider(ModLootTableProvider.create(output, lookupProvider));
            event.addProvider(new ModRecipeProvider(output, lookupProvider));
            event.addProvider(new NuclearDataProvider(output));
            event.addProvider(new AdvancedWorldgenProvider(output));
            event.addProvider(new AdvancedGameTestStructureProvider(output));
        }

        if (event.includeClient() || event.includeServer()) {
            event.addProvider(new TinkersConstructDataProvider(output));
        }
    }
}
