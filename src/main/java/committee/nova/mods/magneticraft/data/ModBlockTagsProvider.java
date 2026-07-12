package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModTags;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Generates mining, common-material and Magneticraft block tags.
 */
final class ModBlockTagsProvider extends BlockTagsProvider {
    ModBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            Block block = ModBlocks.get(definition).get();
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            switch (definition.miningTier()) {
                case STONE -> tag(BlockTags.NEEDS_STONE_TOOL).add(block);
                case IRON -> tag(BlockTags.NEEDS_IRON_TOOL).add(block);
                case NONE -> {
                }
            }
        }
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModMachineBlocks.CRUSHING_TABLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModMachineBlocks.BATTERY.get(),
                ModMachineBlocks.GRATE.get(),
                ModMachineBlocks.ELECTRIC_FURNACE.get()
        );
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModMachineBlocks.BATTERY.get(),
                ModMachineBlocks.GRATE.get(),
                ModMachineBlocks.ELECTRIC_FURNACE.get()
        );
        ModMachineBlocks.machines().forEach((definition, holder) -> {
            if (definition.isWooden()) {
                tag(BlockTags.MINEABLE_WITH_AXE).add(holder.get());
            } else {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
                tag(BlockTags.NEEDS_STONE_TOOL).add(holder.get());
            }
        });
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModMachineBlocks.TUBE_LIGHT.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                ModNetworkBlocks.HEAT_PIPE.get(),
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                ModNetworkBlocks.HEAT_SINK.get(),
                ModNetworkBlocks.IRON_PIPE.get(),
                ModNetworkBlocks.PNEUMATIC_TUBE.get(),
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(),
                ModNetworkBlocks.CONVEYOR_BELT.get()
        );
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                ModNetworkBlocks.HEAT_PIPE.get(),
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                ModNetworkBlocks.HEAT_SINK.get(),
                ModNetworkBlocks.IRON_PIPE.get(),
                ModNetworkBlocks.PNEUMATIC_TUBE.get(),
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(),
                ModNetworkBlocks.CONVEYOR_BELT.get()
        );

        addOre(BaseBlockDefinition.GALENA_ORE, "galena", "lead", "silver");
        addOre(BaseBlockDefinition.COBALT_ORE, "cobalt");
        addOre(BaseBlockDefinition.TUNGSTEN_ORE, "tungsten");
        addOre(BaseBlockDefinition.PYRITE_ORE, "pyrite", "sulfur");
        tag(ModTags.Blocks.ore("copper")).add(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);

        addStorage(BaseBlockDefinition.LEAD_BLOCK, "lead");
        addStorage(BaseBlockDefinition.COBALT_BLOCK, "cobalt");
        addStorage(BaseBlockDefinition.TUNGSTEN_BLOCK, "tungsten");
        addStorage(BaseBlockDefinition.SULFUR_BLOCK, "sulfur");
        tag(ModTags.Blocks.storageBlock("copper")).add(Blocks.COPPER_BLOCK);

        tag(ModTags.Blocks.LIMESTONE_BLOCKS).add(
                ModBlocks.get(BaseBlockDefinition.LIMESTONE).get(),
                ModBlocks.get(BaseBlockDefinition.LIMESTONE_BRICKS).get(),
                ModBlocks.get(BaseBlockDefinition.COBBLED_LIMESTONE).get()
        );
        tag(ModTags.Blocks.BURNT_LIMESTONE_BLOCKS).add(
                ModBlocks.get(BaseBlockDefinition.BURNT_LIMESTONE).get(),
                ModBlocks.get(BaseBlockDefinition.BURNT_LIMESTONE_BRICKS).get(),
                ModBlocks.get(BaseBlockDefinition.COBBLED_BURNT_LIMESTONE).get()
        );
        tag(ModTags.Blocks.LIMESTONE_TILES).add(
                ModBlocks.get(BaseBlockDefinition.LIMESTONE_TILES).get(),
                ModBlocks.get(BaseBlockDefinition.INVERTED_LIMESTONE_TILES).get()
        );
    }

    private void addOre(BaseBlockDefinition definition, String... materials) {
        Block block = ModBlocks.get(definition).get();
        tag(Tags.Blocks.ORES).add(block);
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(block);
        tag(Tags.Blocks.ORE_RATES_SINGULAR).add(block);
        for (String material : materials) {
            tag(ModTags.Blocks.ore(material)).add(block);
        }
    }

    private void addStorage(BaseBlockDefinition definition, String material) {
        Block block = ModBlocks.get(definition).get();
        tag(Tags.Blocks.STORAGE_BLOCKS).add(block);
        tag(ModTags.Blocks.storageBlock(material)).add(block);
    }
}
