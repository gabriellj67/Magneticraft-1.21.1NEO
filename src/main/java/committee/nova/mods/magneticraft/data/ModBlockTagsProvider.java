package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.OreBlockDefinition;
import committee.nova.mods.magneticraft.content.block.DecorativeBlockFamily;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
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
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            ModBlocks.DecorativeFamilyBlocks blocks = ModBlocks.decorativeFamily(family);
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(blocks.stairs().get(), blocks.slab().get());
            if (family.registersBase()) {
                tag(BlockTags.MINEABLE_WITH_PICKAXE).add(blocks.base().get());
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
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModMachineBlocks.TUBE_LIGHT.get(),
                ModMachineBlocks.GEOTHERMAL_DRILL_PIPE.get()
        );
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                ModNetworkBlocks.BURNT_ELECTRIC_CABLE.get(),
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                ModNetworkBlocks.BOX_TRANSFORMER.get(),
                ModNetworkBlocks.FUSE_BOX.get(),
                ModNetworkBlocks.CIRCUIT_BREAKER.get(),
                ModNetworkBlocks.ELECTRIC_SWITCH.get(),
                ModNetworkBlocks.DIODE.get(),
                ModNetworkBlocks.RESISTOR.get(),
                ModNetworkBlocks.TESLA_TOWER.get(),
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(),
                ModNetworkBlocks.WIND_TURBINE.get(),
                ModNetworkBlocks.HEAT_PIPE.get(),
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                ModNetworkBlocks.HEAT_SINK.get(),
                ModNetworkBlocks.IRON_PIPE.get(),
                ModNetworkBlocks.PNEUMATIC_TUBE.get(),
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(),
                ModNetworkBlocks.BRASS_PRESSURE_PIPE.get(),
                ModNetworkBlocks.PRESSURE_TANK.get(),
                ModNetworkBlocks.CONVEYOR_BELT.get()
        );
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                ModNetworkBlocks.BURNT_ELECTRIC_CABLE.get(),
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                ModNetworkBlocks.BOX_TRANSFORMER.get(),
                ModNetworkBlocks.FUSE_BOX.get(),
                ModNetworkBlocks.CIRCUIT_BREAKER.get(),
                ModNetworkBlocks.ELECTRIC_SWITCH.get(),
                ModNetworkBlocks.DIODE.get(),
                ModNetworkBlocks.RESISTOR.get(),
                ModNetworkBlocks.TESLA_TOWER.get(),
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(),
                ModNetworkBlocks.WIND_TURBINE.get(),
                ModNetworkBlocks.HEAT_PIPE.get(),
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                ModNetworkBlocks.HEAT_SINK.get(),
                ModNetworkBlocks.IRON_PIPE.get(),
                ModNetworkBlocks.PNEUMATIC_TUBE.get(),
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(),
                ModNetworkBlocks.BRASS_PRESSURE_PIPE.get(),
                ModNetworkBlocks.PRESSURE_TANK.get(),
                ModNetworkBlocks.CONVEYOR_BELT.get()
        );
        tag(BlockTags.MINEABLE_WITH_AXE).add(
                ModNetworkBlocks.ELECTRIC_POLE.get(),
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get()
        );
        ModAdvancedBlocks.blockItems().forEach(item -> {
            Block block = Block.byItem(item.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            tag(BlockTags.NEEDS_STONE_TOOL).add(block);
        });
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModComputerContent.COMPUTER.get(),
                ModComputerContent.MINING_ROBOT.get()
        );
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModComputerContent.COMPUTER.get(),
                ModComputerContent.MINING_ROBOT.get()
        );

        for (OreBlockDefinition ore : OreBlockDefinition.values()) {
            addOre(ore.block(), ore.forgeMaterials());
        }
        tag(ModTags.Blocks.ore("copper")).add(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);

        addStorage(BaseBlockDefinition.LEAD_BLOCK, "lead");
        addStorage(BaseBlockDefinition.COBALT_BLOCK, "cobalt");
        addStorage(BaseBlockDefinition.TUNGSTEN_BLOCK, "tungsten");
        addStorage(BaseBlockDefinition.CARBIDE_BLOCK, "carbide");
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

    private void addOre(BaseBlockDefinition definition, Iterable<String> materials) {
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
