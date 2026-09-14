package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.OreBlockDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.content.nuclear.material.NuclearMaterial;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModTags;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Generates material interoperability and Magneticraft-owned item tags.
 */
final class ModItemTagsProvider extends ItemTagsProvider {
    ModItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        addMaterialForms();
        addOreAndStorageTags();

        Item sulfur = ModItems.component(CraftingComponent.SULFUR).get();
        tag(ModTags.Items.SULFUR).add(sulfur);
        tag(ModTags.Items.SULFUR_DUST).add(sulfur);
        tag(Tags.Items.DUSTS).add(sulfur);
        tag(ModTags.Items.PLASTIC_SHEETS).add(ModItems.PLASTIC_SHEET.get());
        tag(ModTags.Items.RUBBER).add(ModItems.RUBBER.get());
        tag(ModTags.Items.URANIUM_DUSTS).add(ModNuclearItems.material(NuclearMaterial.URANIUM_DUST).get());
        tag(ModTags.Items.ZIRCONIUM_DUSTS).add(ModNuclearItems.material(NuclearMaterial.ZIRCONIUM_DUST).get());
        tag(ModTags.Items.BORON_DUSTS).add(ModNuclearItems.material(NuclearMaterial.BORON_DUST).get());
        ModNuclearItems.fuelAssemblies().forEach((grade, holder) ->
                tag(ModTags.Items.NUCLEAR_FUEL_ASSEMBLIES).add(holder.get()));

        tag(ModTags.Items.HAMMERS).add(
                ModItems.hammer(HammerType.STONE).get(),
                ModItems.hammer(HammerType.IRON).get(),
                ModItems.hammer(HammerType.STEEL).get()
        );
        tag(ModTags.Items.WRENCHES).add(ModNetworkItems.WRENCH.get());
    }

    private void addMaterialForms() {
        ModItems.materials().forEach((form, metals) -> metals.forEach((metal, holder) -> {
            Item item = holder.get();
            switch (form) {
                case INGOT -> addMaterial(item, ModTags.Items.ingot(metal), Tags.Items.INGOTS);
                case NUGGET -> addMaterial(item, ModTags.Items.nugget(metal), Tags.Items.NUGGETS);
                case DUST -> addMaterial(item, ModTags.Items.dust(metal), Tags.Items.DUSTS);
                case LIGHT_PLATE -> addMaterial(
                        item,
                        ModTags.Items.lightPlate(metal),
                        ModTags.Items.LIGHT_PLATES
                );
                case HEAVY_PLATE -> addMaterial(
                        item,
                        ModTags.Items.heavyPlate(metal),
                        ModTags.Items.HEAVY_PLATES
                );
                case CHUNK -> addMaterial(item, ModTags.Items.chunk(metal), ModTags.Items.CHUNKS);
                case ROCKY_CHUNK -> addMaterial(
                        item,
                        ModTags.Items.rockyChunk(metal),
                        ModTags.Items.ROCKY_CHUNKS
                );
            }
        }));

        tag(ModTags.Items.ingot(Metal.COPPER)).add(Items.COPPER_INGOT);
        tag(ModTags.Items.aluminiumAlias("ingots")).add(ModItems.material(MaterialForm.INGOT, Metal.ALUMINIUM).get());
        tag(ModTags.Items.aluminiumAlias("nuggets")).add(ModItems.material(MaterialForm.NUGGET, Metal.ALUMINIUM).get());
        tag(ModTags.Items.aluminiumAlias("dusts")).add(ModItems.material(MaterialForm.DUST, Metal.ALUMINIUM).get());
        tag(ModTags.Items.aluminiumChunkAlias(false)).add(ModItems.material(MaterialForm.CHUNK, Metal.ALUMINIUM).get());
        tag(ModTags.Items.aluminiumChunkAlias(true)).add(ModItems.material(MaterialForm.ROCKY_CHUNK, Metal.ALUMINIUM).get());
    }

    private void addOreAndStorageTags() {
        for (OreBlockDefinition ore : OreBlockDefinition.values()) {
            addOre(ore.block(), ore.forgeMaterials());
        }
        tag(ModTags.Items.ore("copper")).add(Blocks.COPPER_ORE.asItem(), Blocks.DEEPSLATE_COPPER_ORE.asItem());

        addStorage(BaseBlockDefinition.LEAD_BLOCK, "lead");
        addStorage(BaseBlockDefinition.COBALT_BLOCK, "cobalt");
        addStorage(BaseBlockDefinition.TUNGSTEN_BLOCK, "tungsten");
        addStorage(BaseBlockDefinition.CARBIDE_BLOCK, "carbide");
        addStorage(BaseBlockDefinition.SULFUR_BLOCK, "sulfur");
        tag(ModTags.Items.storageBlock("copper")).add(Blocks.COPPER_BLOCK.asItem());
    }

    private void addOre(BaseBlockDefinition definition, Iterable<String> materials) {
        Item item = ModBlocks.get(definition).get().asItem();
        tag(Tags.Items.ORES).add(item);
        tag(Tags.Items.ORES_IN_GROUND_STONE).add(item);
        tag(Tags.Items.ORE_RATES_SINGULAR).add(item);
        for (String material : materials) {
            tag(ModTags.Items.ore(material)).add(item);
        }
    }

    private void addStorage(BaseBlockDefinition definition, String material) {
        Item item = ModBlocks.get(definition).get().asItem();
        tag(Tags.Items.STORAGE_BLOCKS).add(item);
        tag(ModTags.Items.storageBlock(material)).add(item);
    }

    private void addMaterial(Item item, TagKey<Item> materialTag, TagKey<Item> aggregateTag) {
        tag(materialTag).add(item);
        tag(aggregateTag).add(item);
    }
}
