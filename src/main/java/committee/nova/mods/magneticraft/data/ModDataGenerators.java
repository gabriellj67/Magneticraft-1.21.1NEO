package committee.nova.mods.magneticraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Registers only data providers that own resources in the current migration stage.
 */
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(
                event.includeClient(),
                new ModBlockStateProvider(output, existingFileHelper)
        );
        generator.addProvider(
                event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper)
        );
        generator.addProvider(
                event.includeClient(),
                new ModSpriteSourceProvider(output, existingFileHelper)
        );
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(output, "en_us", false)
        );
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(output, "zh_cn", true)
        );
        generator.addProvider(
                event.includeClient(),
                new AdvancedGuideDataProvider(output, AdvancedGuideDataProvider.computerOpcodes())
        );

        ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(
                event.includeServer(),
                new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper)
        );
        generator.addProvider(
                event.includeServer(),
                new ModFluidTagsProvider(output, lookupProvider, existingFileHelper)
        );
        generator.addProvider(event.includeServer(), ModLootTableProvider.create(output));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output));
        generator.addProvider(event.includeServer(), new AdvancedWorldgenProvider(output));
        generator.addProvider(event.includeServer(), new AdvancedGameTestStructureProvider(output));
    }
}
