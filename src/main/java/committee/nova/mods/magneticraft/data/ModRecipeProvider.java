package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMath;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.HydraulicPressMode;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.data.recipe.CountedCookingRecipeBuilder;
import committee.nova.mods.magneticraft.data.recipe.CrushingRecipeBuilder;
import committee.nova.mods.magneticraft.data.recipe.SingleBlockRecipeBuilder;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Generates every vanilla crafting and furnace recipe owned by the base-content task.
 */
final class ModRecipeProvider extends RecipeProvider {
    private static final float LEGACY_SMELTING_EXPERIENCE = 0.1F;
    private static final int SMELTING_TIME_TICKS = 200;

    ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        addMaterialConversions(consumer);
        addStorageConversions(consumer);
        addDecorationRecipes(consumer);
        addComponentRecipes(consumer);
        addHammerRecipes(consumer);
        addSmeltingRecipes(consumer);
        addMachineCraftingRecipes(consumer);
        addPortableElectricRecipes(consumer);
        addSingleBlockCraftingRecipes(consumer);
        addCrushingRecipes(consumer);
        addSluiceRecipes(consumer);
        addGasificationRecipes(consumer);
        addThermopileRecipes(consumer);
        addFluidFuelRecipes(consumer);
        addAdvancedCraftingRecipes(consumer);
        addAdvancedProcessingRecipes(consumer);
    }

    private void addAdvancedCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.MULTIBLOCK_BASE.get(), 4)
                .pattern("III")
                .pattern("ISI")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', Tags.Items.STONE)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer, id("crafting/multiblock_base"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.CORRUGATED_IRON.get(), 4)
                .pattern("III")
                .define('I', ModTags.Items.lightPlate(Metal.IRON))
                .unlockedBy("has_iron_light_plate", has(ModTags.Items.lightPlate(Metal.IRON)))
                .save(consumer, id("crafting/corrugated_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.COPPER_COIL.get(), 2)
                .pattern("WWW")
                .pattern("W I")
                .pattern("WWW")
                .define('W', component(CraftingComponent.FINE_COPPER_WIRE))
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_fine_copper_wire", has(component(CraftingComponent.FINE_COPPER_WIRE)))
                .save(consumer, id("crafting/copper_coil"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.MULTIBLOCK_COLUMN.get(), 4)
                .pattern("I")
                .pattern("B")
                .pattern("I")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', ModAdvancedBlocks.MULTIBLOCK_BASE.get())
                .unlockedBy("has_multiblock_base", has(ModAdvancedBlocks.MULTIBLOCK_BASE.get()))
                .save(consumer, id("crafting/multiblock_column"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get(), 4)
                .pattern("YBY")
                .define('Y', Tags.Items.DYES_YELLOW)
                .define('B', ModAdvancedBlocks.MULTIBLOCK_BASE.get())
                .unlockedBy("has_multiblock_base", has(ModAdvancedBlocks.MULTIBLOCK_BASE.get()))
                .save(consumer, id("crafting/striped_multiblock_part"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get(), 4)
                .pattern("RWR")
                .pattern("WBW")
                .pattern("RWR")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('W', component(CraftingComponent.FINE_COPPER_WIRE))
                .define('B', ModAdvancedBlocks.MULTIBLOCK_BASE.get())
                .unlockedBy("has_multiblock_base", has(ModAdvancedBlocks.MULTIBLOCK_BASE.get()))
                .save(consumer, id("crafting/electric_multiblock_part"));

        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            ShapedRecipeBuilder.shaped(
                            RecipeCategory.REDSTONE,
                            ModAdvancedBlocks.controller(definition).get()
                    )
                    .pattern("BBB")
                    .pattern("CMC")
                    .pattern("BBB")
                    .define('B', ModAdvancedBlocks.MULTIBLOCK_BASE.get())
                    .define('C', ModAdvancedBlocks.COPPER_COIL.get())
                    .define('M', controllerMarker(definition))
                    .unlockedBy("has_multiblock_base", has(ModAdvancedBlocks.MULTIBLOCK_BASE.get()))
                    .save(consumer, id("crafting/" + definition.id()));
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModComputerContent.COMPUTER.get())
                .pattern("IRI")
                .pattern("QCQ")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('C', ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get())
                .unlockedBy("has_electric_multiblock_part", has(ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get()))
                .save(consumer, id("crafting/computer"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModComputerContent.MINING_ROBOT.get())
                .pattern("IPI")
                .pattern("ICI")
                .pattern("IHI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', Blocks.PISTON)
                .define('C', ModComputerContent.COMPUTER.get())
                .define('H', Tags.Items.CHESTS_WOODEN)
                .unlockedBy("has_computer", has(ModComputerContent.COMPUTER.get()))
                .save(consumer, id("crafting/mining_robot"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModComputerContent.FLOPPY_DISK.get())
                .pattern("IRI")
                .pattern("PPP")
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', Items.PAPER)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(consumer, id("crafting/floppy_disk"));
    }

    private void addAdvancedProcessingRecipes(Consumer<FinishedRecipe> consumer) {
        for (Metal metal : Metal.values()) {
            if (MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                grinder(
                        consumer,
                        metal.id() + "_ore",
                        Ingredient.of(ModTags.Items.ore(metal.id())),
                        output(ModItems.material(MaterialForm.ROCKY_CHUNK, metal).get(), 1, 1.0F),
                        output(Blocks.GRAVEL, 1, 0.15F),
                        50
                );
            }
            if (!metal.isComposite() && MaterialForm.DUST.appliesTo(metal)) {
                grinder(
                        consumer,
                        metal.id() + "_ingot",
                        Ingredient.of(ModTags.Items.ingot(metal)),
                        output(ModItems.material(MaterialForm.DUST, metal).get(), 1, 1.0F),
                        null,
                        50
                );
            }
        }

        grinder(consumer, "redstone_ore", Ingredient.of(Blocks.REDSTONE_ORE),
                output(Items.REDSTONE, 4, 1.0F), output(Blocks.GRAVEL, 1, 0.15F), 50);
        grinder(consumer, "lapis_ore", Ingredient.of(Blocks.LAPIS_ORE),
                output(Items.LAPIS_LAZULI, 6, 1.0F), output(Blocks.GRAVEL, 1, 0.15F), 50);
        grinder(consumer, "nether_quartz_ore", Ingredient.of(Blocks.NETHER_QUARTZ_ORE),
                output(Items.QUARTZ, 3, 1.0F), output(Items.QUARTZ, 1, 0.5F), 60);
        grinder(consumer, "emerald_ore", Ingredient.of(Blocks.EMERALD_ORE),
                output(Items.EMERALD, 2, 1.0F), output(Blocks.GRAVEL, 1, 0.15F), 50);
        grinder(consumer, "diamond_ore", Ingredient.of(Blocks.DIAMOND_ORE),
                output(Items.DIAMOND, 1, 1.0F), output(Items.DIAMOND, 1, 0.75F), 50);
        grinder(consumer, "coal_ore", Ingredient.of(Blocks.COAL_ORE),
                output(Items.COAL, 1, 1.0F), output(Items.COAL, 1, 0.5F), 50);
        grinder(consumer, "glowstone", Ingredient.of(Blocks.GLOWSTONE),
                output(Items.GLOWSTONE_DUST, 4, 1.0F), null, 40);
        grinder(consumer, "sandstone", Ingredient.of(Blocks.SANDSTONE),
                output(Blocks.SAND, 4, 1.0F), null, 40);
        grinder(consumer, "red_sandstone", Ingredient.of(Blocks.RED_SANDSTONE),
                output(Blocks.RED_SAND, 4, 1.0F), null, 40);
        grinder(consumer, "blaze_rod", Ingredient.of(Items.BLAZE_ROD),
                output(Items.BLAZE_POWDER, 4, 1.0F), output(component(CraftingComponent.SULFUR), 1, 0.5F), 50);
        grinder(consumer, "wool", Ingredient.of(ItemTags.WOOL),
                output(Items.STRING, 4, 1.0F), null, 40);
        grinder(consumer, "bone", Ingredient.of(Items.BONE),
                output(Items.BONE_MEAL, 5, 1.0F), output(Items.BONE_MEAL, 3, 0.5F), 40);
        grinder(consumer, "sugar_cane", Ingredient.of(Items.SUGAR_CANE),
                output(Items.SUGAR, 1, 1.0F), output(Items.SUGAR, 2, 0.5F), 40);
        grinder(consumer, "cobblestone", Ingredient.of(Blocks.COBBLESTONE),
                output(Blocks.GRAVEL, 1, 1.0F), output(Blocks.SAND, 1, 0.5F), 60);
        grinder(consumer, "quartz_block", Ingredient.of(Blocks.QUARTZ_BLOCK),
                output(Items.QUARTZ, 4, 1.0F), null, 50);
        grinder(consumer, "limestone", Ingredient.of(block(BaseBlockDefinition.LIMESTONE)),
                output(block(BaseBlockDefinition.COBBLED_LIMESTONE), 1, 1.0F), null, 20);
        grinder(consumer, "burnt_limestone", Ingredient.of(block(BaseBlockDefinition.BURNT_LIMESTONE)),
                output(block(BaseBlockDefinition.COBBLED_BURNT_LIMESTONE), 1, 1.0F), null, 20);
        grinder(consumer, "pyrite_ore", Ingredient.of(ModTags.Items.ore("pyrite")),
                output(component(CraftingComponent.SULFUR), 4, 1.0F),
                output(ModItems.material(MaterialForm.DUST, Metal.IRON).get(), 1, 0.01F), 40);

        for (Metal metal : Metal.values()) {
            if (!MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                continue;
            }
            List<AdvancedProcessingRecipe.ChanceResult> outputs = new ArrayList<>();
            if (metal == Metal.GALENA) {
                outputs.add(output(ModItems.material(MaterialForm.CHUNK, Metal.LEAD).get(), 1, 1.0F));
                outputs.add(output(ModItems.material(MaterialForm.CHUNK, Metal.SILVER).get(), 1, 1.0F));
            } else if (MaterialForm.CHUNK.appliesTo(metal)) {
                outputs.add(output(ModItems.material(MaterialForm.CHUNK, metal).get(), 1, 1.0F));
                for (Metal subProduct : sluiceSubProducts(metal).stream().limit(2).toList()) {
                    outputs.add(output(ModItems.material(MaterialForm.DUST, subProduct).get(), 1, 0.15F));
                }
            }
            if (!outputs.isEmpty()) {
                advancedProcessing(
                        consumer,
                        "sieve_" + metal.id() + "_rocky_chunk",
                        MultiblockDefinition.SIEVE,
                        Ingredient.of(ModTags.Items.rockyChunk(metal)),
                        1,
                        outputs,
                        null,
                        50,
                        40
                );
            }
        }
        sieve(consumer, "gravel", Ingredient.of(Blocks.GRAVEL), List.of(
                output(Items.FLINT, 1, 1.0F),
                output(Items.FLINT, 1, 0.15F),
                output(Items.FLINT, 1, 0.05F)
        ), 50);
        sieve(consumer, "sand", Ingredient.of(Blocks.SAND), List.of(
                output(Items.GOLD_NUGGET, 1, 0.04F),
                output(Items.GOLD_NUGGET, 1, 0.02F),
                output(Items.QUARTZ, 1, 0.01F)
        ), 80);
        sieve(consumer, "soul_sand", Ingredient.of(Blocks.SOUL_SAND), List.of(
                output(Items.QUARTZ, 1, 0.15F),
                output(Items.QUARTZ, 1, 0.10F),
                output(Items.QUARTZ, 1, 0.05F)
        ), 80);

        Map<Metal, Integer> pressDurations = Map.of(
                Metal.IRON, 120,
                Metal.GOLD, 50,
                Metal.COPPER, 100,
                Metal.LEAD, 50,
                Metal.TUNGSTEN, 250,
                Metal.STEEL, 140
        );
        pressDurations.forEach((metal, duration) -> {
            press(consumer, metal.id() + "_heavy_plate", Ingredient.of(ModTags.Items.ingot(metal)), 4,
                    ModItems.material(MaterialForm.HEAVY_PLATE, metal).get(), HydraulicPressMode.HEAVY, duration);
            press(consumer, metal.id() + "_light_plate", Ingredient.of(ModTags.Items.ingot(metal)), 1,
                    ModItems.material(MaterialForm.LIGHT_PLATE, metal).get(), HydraulicPressMode.MEDIUM, duration);
        });
        press(consumer, "stone", Ingredient.of(Blocks.STONE), 1, Blocks.COBBLESTONE,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "polished_andesite", Ingredient.of(Blocks.POLISHED_ANDESITE), 1, Blocks.ANDESITE,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "polished_diorite", Ingredient.of(Blocks.POLISHED_DIORITE), 1, Blocks.DIORITE,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "polished_granite", Ingredient.of(Blocks.POLISHED_GRANITE), 1, Blocks.GRANITE,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "mossy_stone_bricks", Ingredient.of(Blocks.MOSSY_STONE_BRICKS), 1, Blocks.MOSSY_COBBLESTONE,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "stone_bricks", Ingredient.of(Blocks.STONE_BRICKS), 1, Blocks.CRACKED_STONE_BRICKS,
                HydraulicPressMode.LIGHT, 55);
        press(consumer, "end_stone_bricks", Ingredient.of(Blocks.END_STONE_BRICKS), 1, Blocks.END_STONE,
                HydraulicPressMode.LIGHT, 100);
        press(consumer, "smooth_red_sandstone", Ingredient.of(Blocks.SMOOTH_RED_SANDSTONE), 1, Blocks.RED_SANDSTONE,
                HydraulicPressMode.LIGHT, 40);
        press(consumer, "smooth_sandstone", Ingredient.of(Blocks.SMOOTH_SANDSTONE), 1, Blocks.SANDSTONE,
                HydraulicPressMode.LIGHT, 40);
        press(consumer, "prismarine_bricks", Ingredient.of(Blocks.PRISMARINE_BRICKS), 1, Blocks.PRISMARINE,
                HydraulicPressMode.LIGHT, 50);
        press(consumer, "ice", Ingredient.of(Blocks.ICE), 1, Blocks.PACKED_ICE,
                HydraulicPressMode.LIGHT, 200);
    }

    private void grinder(
            Consumer<FinishedRecipe> consumer,
            String name,
            Ingredient input,
            AdvancedProcessingRecipe.ChanceResult primary,
            @Nullable AdvancedProcessingRecipe.ChanceResult secondary,
            int duration
    ) {
        List<AdvancedProcessingRecipe.ChanceResult> outputs = secondary == null
                ? List.of(primary)
                : List.of(primary, secondary);
        advancedProcessing(consumer, "grinder_" + name, MultiblockDefinition.GRINDER,
                input, 1, outputs, null, duration, 40);
    }

    private void sieve(
            Consumer<FinishedRecipe> consumer,
            String name,
            Ingredient input,
            List<AdvancedProcessingRecipe.ChanceResult> outputs,
            int duration
    ) {
        advancedProcessing(consumer, "sieve_" + name, MultiblockDefinition.SIEVE,
                input, 1, outputs, null, duration, 40);
    }

    private void press(
            Consumer<FinishedRecipe> consumer,
            String name,
            Ingredient input,
            int inputCount,
            ItemLike output,
            HydraulicPressMode mode,
            int duration
    ) {
        advancedProcessing(consumer, "hydraulic_press_" + name, MultiblockDefinition.HYDRAULIC_PRESS,
                input, inputCount, List.of(output(output, 1, 1.0F)), mode, duration, 60);
    }

    private static AdvancedProcessingRecipe.ChanceResult output(
            ItemLike item,
            int count,
            float chance
    ) {
        return new AdvancedProcessingRecipe.ChanceResult(new ItemStack(item, count), chance);
    }

    private void advancedProcessing(
            Consumer<FinishedRecipe> consumer,
            String name,
            MultiblockDefinition machine,
            Ingredient input,
            int inputCount,
            List<AdvancedProcessingRecipe.ChanceResult> outputs,
            @Nullable HydraulicPressMode pressMode,
            int duration,
            int energyPerTick
    ) {
        consumer.accept(new AdvancedProcessingResult(
                id("advanced_processing/" + name),
                machine,
                input,
                inputCount,
                outputs,
                pressMode,
                duration,
                energyPerTick
        ));
    }

    private static ItemLike controllerMarker(MultiblockDefinition definition) {
        return switch (definition) {
            case BIG_COMBUSTION_CHAMBER -> Items.COAL;
            case BIG_ELECTRIC_FURNACE -> Blocks.BLAST_FURNACE;
            case BIG_STEAM_BOILER -> Items.CAULDRON;
            case CONTAINER -> Items.CHEST;
            case GRINDER -> Items.DIAMOND;
            case HYDRAULIC_PRESS -> Blocks.PISTON;
            case OIL_HEATER -> Items.MAGMA_CREAM;
            case PUMPJACK -> Items.BUCKET;
            case REFINERY -> Items.BREWING_STAND;
            case SHELVING_UNIT -> Blocks.BOOKSHELF;
            case SIEVE -> Blocks.IRON_BARS;
            case SOLAR_MIRROR -> Items.GLASS_PANE;
            case SOLAR_PANEL -> Blocks.DAYLIGHT_DETECTOR;
            case SOLAR_TOWER -> Blocks.GLOWSTONE;
            case STEAM_ENGINE -> Items.MINECART;
            case STEAM_TURBINE -> Items.LIGHTNING_ROD;
        };
    }

    private void addMachineCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModMachineBlocks.CRUSHING_TABLE.get())
                .pattern("AAA")
                .pattern("BCB")
                .pattern("CDC")
                .define('A', Blocks.STONE_SLAB)
                .define('B', Tags.Items.RODS_WOODEN)
                .define('C', ItemTags.PLANKS)
                .define('D', ItemTags.LOGS)
                .unlockedBy("has_stone_slab", has(Blocks.STONE_SLAB))
                .save(consumer, id("crafting/crushing_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachineItems.LOW_BATTERY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("CDC")
                .define('A', Tags.Items.NUGGETS_IRON)
                .define('B', ModTags.Items.ingot(Metal.COPPER))
                .define('C', ModTags.Items.ingot(Metal.LEAD))
                .define('D', ModTags.Items.SULFUR_DUST)
                .unlockedBy("has_sulfur", has(ModTags.Items.SULFUR_DUST))
                .save(consumer, id("crafting/battery_item_low"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModMachineBlocks.GRATE.get(), 4)
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Blocks.IRON_BARS)
                .define('B', Tags.Items.STONE)
                .unlockedBy("has_iron_bars", has(Blocks.IRON_BARS))
                .save(consumer, id("crafting/grate"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachineBlocks.BATTERY.get())
                .pattern("AAA")
                .pattern("DCD")
                .pattern("DBD")
                .define('A', ModMachineItems.LOW_BATTERY.get())
                .define('B', ModTags.Items.lightPlate(Metal.IRON))
                .define('C', ModMachineBlocks.GRATE.get())
                .define('D', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_battery_item_low", has(ModMachineItems.LOW_BATTERY.get()))
                .save(consumer, id("crafting/battery"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachineBlocks.ELECTRIC_FURNACE.get())
                .pattern("ABA")
                .pattern("CCC")
                .define('A', ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get())
                .define('B', machine(SingleBlockMachineDefinition.BRICK_FURNACE))
                .define('C', ModTags.Items.ingot(Metal.COPPER))
                .unlockedBy("has_fine_copper_wire", has(ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get()))
                .save(consumer, id("crafting/electric_furnace"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModNetworkItems.WRENCH.get())
                .pattern(" I ")
                .pattern(" SI")
                .pattern("S  ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer, id("crafting/wrench"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModNetworkBlocks.ELECTRIC_CABLE.get(), 8)
                .pattern("CCC")
                .pattern("WWW")
                .pattern("CCC")
                .define('C', ModTags.Items.ingot(Metal.COPPER))
                .define('W', ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get())
                .unlockedBy("has_fine_copper_wire", has(ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get()))
                .save(consumer, id("crafting/electric_cable"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModNetworkBlocks.HEAT_PIPE.get(), 8)
                .pattern("III")
                .pattern("   ")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer, id("crafting/heat_pipe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), 8)
                .pattern("WWW")
                .pattern("PPP")
                .pattern("WWW")
                .define('W', ItemTags.WOOL)
                .define('P', ModNetworkBlocks.HEAT_PIPE.get())
                .unlockedBy("has_heat_pipe", has(ModNetworkBlocks.HEAT_PIPE.get()))
                .save(consumer, id("crafting/insulated_heat_pipe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModNetworkBlocks.HEAT_SINK.get())
                .pattern("III")
                .pattern("CGC")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', ModTags.Items.ingot(Metal.COPPER))
                .define('G', ModMachineBlocks.GRATE.get())
                .unlockedBy("has_grate", has(ModMachineBlocks.GRATE.get()))
                .save(consumer, id("crafting/heat_sink"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModNetworkBlocks.IRON_PIPE.get(), 8)
                .pattern("III")
                .pattern("   ")
                .pattern("III")
                .define('I', ModTags.Items.lightPlate(Metal.IRON))
                .unlockedBy("has_iron_light_plate", has(ModTags.Items.lightPlate(Metal.IRON)))
                .save(consumer, id("crafting/iron_pipe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModNetworkBlocks.PNEUMATIC_TUBE.get(), 8)
                .pattern("IGI")
                .pattern("G G")
                .pattern("IGI")
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS)
                .unlockedBy("has_glass", has(Tags.Items.GLASS))
                .save(consumer, id("crafting/pneumatic_tube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(), 8)
                .pattern("RRR")
                .pattern("TTT")
                .pattern("RRR")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('T', ModNetworkBlocks.PNEUMATIC_TUBE.get())
                .unlockedBy("has_pneumatic_tube", has(ModNetworkBlocks.PNEUMATIC_TUBE.get()))
                .save(consumer, id("crafting/pneumatic_restriction_tube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModNetworkBlocks.CONVEYOR_BELT.get(), 4)
                .pattern("III")
                .pattern("RMR")
                .pattern("III")
                .define('I', ModTags.Items.lightPlate(Metal.IRON))
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('M', ModItems.component(CraftingComponent.MOTOR).get())
                .unlockedBy("has_motor", has(ModItems.component(CraftingComponent.MOTOR).get()))
                .save(consumer, id("crafting/conveyor_belt"));
    }

    private void addPortableElectricRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachineItems.MEDIUM_BATTERY.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern("BCB")
                .define('A', ModTags.Items.ingot(Metal.COPPER))
                .define('B', ModMachineItems.LOW_BATTERY.get())
                .define('C', ModTags.Items.lightPlate(Metal.LEAD))
                .unlockedBy("has_battery_item_low", has(ModMachineItems.LOW_BATTERY.get()))
                .save(consumer, id("crafting/battery_item_medium"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModMachineItems.ELECTRIC_DRILL.get())
                .pattern("AAB")
                .pattern("ACD")
                .pattern("BDE")
                .define('A', Tags.Items.GEMS_DIAMOND)
                .define('B', ModTags.Items.ingot(Metal.COPPER))
                .define('C', component(CraftingComponent.MOTOR))
                .define('D', Tags.Items.INGOTS_IRON)
                .define('E', ModMachineItems.LOW_BATTERY.get())
                .unlockedBy("has_motor", has(component(CraftingComponent.MOTOR)))
                .save(consumer, id("crafting/electric_drill"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModMachineItems.ELECTRIC_CHAINSAW.get())
                .pattern("AB ")
                .pattern("BCD")
                .pattern(" DE")
                .define('A', Tags.Items.GEMS_DIAMOND)
                .define('B', ModTags.Items.ingot(Metal.COPPER))
                .define('C', component(CraftingComponent.MOTOR))
                .define('D', Tags.Items.INGOTS_IRON)
                .define('E', ModMachineItems.LOW_BATTERY.get())
                .unlockedBy("has_motor", has(component(CraftingComponent.MOTOR)))
                .save(consumer, id("crafting/electric_chainsaw"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModMachineItems.ELECTRIC_PISTON.get())
                .pattern("AB ")
                .pattern("BCD")
                .pattern(" DE")
                .define('A', Blocks.PISTON)
                .define('B', ModTags.Items.ingot(Metal.COPPER))
                .define('C', component(CraftingComponent.MOTOR))
                .define('D', Tags.Items.INGOTS_IRON)
                .define('E', ModMachineItems.LOW_BATTERY.get())
                .unlockedBy("has_motor", has(component(CraftingComponent.MOTOR)))
                .save(consumer, id("crafting/electric_piston"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModMachineItems.VOLTMETER.get())
                .pattern("ABA")
                .pattern("ACA")
                .pattern("ADA")
                .define('A', Tags.Items.INGOTS_GOLD)
                .define('B', Items.PAPER)
                .define('C', Tags.Items.DUSTS_REDSTONE)
                .define('D', ModTags.Items.ingot(Metal.COPPER))
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/voltmeter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModMachineItems.THERMOMETER.get())
                .pattern("ABA")
                .pattern("ABA")
                .pattern("ACA")
                .define('A', Blocks.GLASS)
                .define('B', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/thermometer"));
    }

    private void addCrushingRecipes(Consumer<FinishedRecipe> consumer) {
        crush(consumer, "creeper_head", Ingredient.of(Items.CREEPER_HEAD), Items.GUNPOWDER, 8, -1);
        crush(consumer, "skeleton_skull", Ingredient.of(Items.SKELETON_SKULL), Items.BONE_MEAL, 8, -1);
        crush(consumer, "zombie_head", Ingredient.of(Items.ZOMBIE_HEAD), Items.ROTTEN_FLESH, 4, -1);

        for (Metal metal : Metal.values()) {
            if (MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                crush(
                        consumer,
                        metal.id() + "_ore",
                        Ingredient.of(ModTags.Items.ore(metal.id())),
                        ModItems.material(MaterialForm.ROCKY_CHUNK, metal).get(),
                        1,
                        1
                );
            }
        }
        crush(
                consumer,
                "pyrite_ore",
                Ingredient.of(ModTags.Items.ore("pyrite")),
                ModItems.component(CraftingComponent.SULFUR).get(),
                2,
                1
        );

        crush(consumer, "limestone", Ingredient.of(block(BaseBlockDefinition.LIMESTONE)), block(BaseBlockDefinition.COBBLED_LIMESTONE), 1, 0);
        crush(consumer, "burnt_limestone", Ingredient.of(block(BaseBlockDefinition.BURNT_LIMESTONE)), block(BaseBlockDefinition.COBBLED_BURNT_LIMESTONE), 1, 0);
        crush(consumer, "iron_block", Ingredient.of(Blocks.IRON_BLOCK), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.IRON).get(), 5, 1);
        crush(consumer, "gold_block", Ingredient.of(Blocks.GOLD_BLOCK), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.GOLD).get(), 5, 2);
        crush(consumer, "copper_block", Ingredient.of(ModTags.Items.storageBlock("copper")), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.COPPER).get(), 5, 1);
        crush(consumer, "lead_block", Ingredient.of(ModTags.Items.storageBlock("lead")), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.LEAD).get(), 5, 1);
        crush(consumer, "tungsten_block", Ingredient.of(ModTags.Items.storageBlock("tungsten")), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.TUNGSTEN).get(), 5, 2);
        crush(consumer, "steel_ingot", Ingredient.of(ModTags.Items.ingot(Metal.STEEL)), ModItems.material(MaterialForm.LIGHT_PLATE, Metal.STEEL).get(), 1, -1);
        crush(consumer, "blaze_rod", Ingredient.of(Items.BLAZE_ROD), Items.BLAZE_POWDER, 5, -1);
        crush(consumer, "bone", Ingredient.of(Items.BONE), Items.BONE_MEAL, 4, -1);
        crush(consumer, "stone", Ingredient.of(Blocks.STONE), Blocks.COBBLESTONE, 1, 0);
        crush(consumer, "polished_andesite", Ingredient.of(Blocks.POLISHED_ANDESITE), Blocks.ANDESITE, 1, 0);
        crush(consumer, "polished_diorite", Ingredient.of(Blocks.POLISHED_DIORITE), Blocks.DIORITE, 1, 0);
        crush(consumer, "polished_granite", Ingredient.of(Blocks.POLISHED_GRANITE), Blocks.GRANITE, 1, 0);
        crush(consumer, "stone_bricks", Ingredient.of(Blocks.STONE_BRICKS), Blocks.CRACKED_STONE_BRICKS, 1, 0);
        crush(consumer, "mossy_stone_bricks", Ingredient.of(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_COBBLESTONE, 1, 0);
        crush(consumer, "dark_prismarine", Ingredient.of(Blocks.DARK_PRISMARINE), Blocks.PRISMARINE, 1, 0);
        crush(consumer, "end_stone_bricks", Ingredient.of(Blocks.END_STONE_BRICKS), Blocks.END_STONE, 1, 0);
    }

    private void addSingleBlockCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, machine(SingleBlockMachineDefinition.BOX))
                .pattern("ABA").pattern("BAB").pattern("ABA")
                .define('A', Tags.Items.RODS_WOODEN).define('B', ItemTags.PLANKS)
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(consumer, id("crafting/box"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.SLUICE_BOX))
                .pattern("AB ").pattern("CAB").pattern("DDD")
                .define('A', ItemTags.PLANKS).define('B', Tags.Items.RODS_WOODEN)
                .define('C', component(CraftingComponent.FABRIC_MESH)).define('D', Blocks.STONE_SLAB)
                .unlockedBy("has_fabric_mesh", has(component(CraftingComponent.FABRIC_MESH)))
                .save(consumer, id("crafting/sluice_box"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.FABRICATOR))
                .pattern("AB").pattern("CD")
                .define('A', Tags.Items.INGOTS_COPPER).define('B', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.DUSTS_REDSTONE).define('D', Blocks.CRAFTING_TABLE)
                .unlockedBy("has_crafting_table", has(Blocks.CRAFTING_TABLE))
                .save(consumer, id("crafting/fabricator"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.SMALL_TANK))
                .pattern("AAA").pattern("ABA").pattern("AAA")
                .define('A', Tags.Items.GLASS).define('B', ModMachineBlocks.GRATE.get())
                .unlockedBy("has_grate", has(ModMachineBlocks.GRATE.get()))
                .save(consumer, id("crafting/small_tank"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, machine(SingleBlockMachineDefinition.FEEDING_TROUGH))
                .pattern("A A").pattern("B B").pattern("ABA")
                .define('A', Tags.Items.RODS_WOODEN).define('B', ItemTags.PLANKS)
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(consumer, id("crafting/feeding_trough"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.INSERTER))
                .pattern("AB ").pattern("BCB").pattern("DED")
                .define('A', Tags.Items.INGOTS_COPPER).define('B', Tags.Items.NUGGETS_IRON)
                .define('C', ModTags.Items.ingot(Metal.LEAD)).define('D', ModTags.Items.lightPlate(Metal.IRON))
                .define('E', component(CraftingComponent.MOTOR))
                .unlockedBy("has_motor", has(component(CraftingComponent.MOTOR)))
                .save(consumer, id("crafting/inserter"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.WATER_GENERATOR))
                .pattern("ABA").pattern("BCB").pattern("ABA")
                .define('A', ModTags.Items.lightPlate(Metal.IRON)).define('B', Tags.Items.GLASS)
                .define('C', Items.WATER_BUCKET)
                .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
                .save(consumer, id("crafting/water_generator"));
        automationEndpoint(consumer, SingleBlockMachineDefinition.RELAY, ModTags.Items.lightPlate(Metal.IRON));
        automationEndpoint(consumer, SingleBlockMachineDefinition.FILTER, component(CraftingComponent.FABRIC_MESH));
        automationEndpoint(consumer, SingleBlockMachineDefinition.TRANSPOSER, component(CraftingComponent.MOTOR));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.COMBUSTION_CHAMBER))
                .pattern("ABA").pattern("A C").pattern("AAA")
                .define('A', Items.BRICK).define('B', ModTags.Items.lightPlate(Metal.IRON))
                .define('C', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(consumer, id("crafting/combustion_chamber"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.STEAM_BOILER))
                .pattern("ABA").pattern("A A").pattern("ABA")
                .define('A', Tags.Items.INGOTS_IRON).define('B', ModTags.Items.lightPlate(Metal.IRON))
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer, id("crafting/steam_boiler"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.ELECTRIC_HEATER))
                .pattern("ABA").pattern("ACA").pattern("DDD")
                .define('A', Tags.Items.INGOTS_IRON).define('B', Tags.Items.INGOTS_COPPER)
                .define('C', ModMachineBlocks.GRATE.get()).define('D', component(CraftingComponent.FINE_COPPER_WIRE))
                .unlockedBy("has_fine_copper_wire", has(component(CraftingComponent.FINE_COPPER_WIRE)))
                .save(consumer, id("crafting/electric_heater"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.RF_HEATER))
                .pattern("ABA").pattern("ACA").pattern("DED")
                .define('A', Tags.Items.INGOTS_IRON).define('B', Tags.Items.INGOTS_COPPER)
                .define('C', ModMachineBlocks.GRATE.get()).define('D', Tags.Items.DUSTS_REDSTONE)
                .define('E', Tags.Items.INGOTS_GOLD)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/rf_heater"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.GASIFICATION_UNIT))
                .pattern("AAA").pattern("ABA").pattern("AAA")
                .define('A', Tags.Items.INGOTS_IRON).define('B', ModTags.Items.lightPlate(Metal.TUNGSTEN))
                .unlockedBy("has_tungsten_plate", has(ModTags.Items.lightPlate(Metal.TUNGSTEN)))
                .save(consumer, id("crafting/gasification_unit"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, machine(SingleBlockMachineDefinition.BRICK_FURNACE))
                .pattern("AAA").pattern("A A").pattern("ABA")
                .define('A', Blocks.BRICKS).define('B', ModTags.Items.lightPlate(Metal.COPPER))
                .unlockedBy("has_bricks", has(Blocks.BRICKS))
                .save(consumer, id("crafting/brick_furnace"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.AIRLOCK))
                .pattern("ABA").pattern("BCB").pattern("ABA")
                .define('A', Tags.Items.GLASS).define('B', Items.BUCKET).define('C', ModMachineBlocks.GRATE.get())
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(consumer, id("crafting/airlock"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.THERMOPILE))
                .pattern("ABA").pattern("BCB").pattern("ABA")
                .define('A', Tags.Items.INGOTS_IRON).define('B', ModTags.Items.lightPlate(Metal.COPPER))
                .define('C', ModMachineBlocks.GRATE.get())
                .unlockedBy("has_copper_plate", has(ModTags.Items.lightPlate(Metal.COPPER)))
                .save(consumer, id("crafting/thermopile"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.RF_TRANSFORMER))
                .pattern("ABA").pattern("CDC").pattern("ABA")
                .define('A', ModTags.Items.lightPlate(Metal.IRON)).define('B', ModTags.Items.lightPlate(Metal.GOLD))
                .define('C', ModTags.Items.lightPlate(Metal.LEAD)).define('D', Blocks.REDSTONE_BLOCK)
                .unlockedBy("has_redstone_block", has(Blocks.REDSTONE_BLOCK))
                .save(consumer, id("crafting/rf_transformer"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(SingleBlockMachineDefinition.ELECTRIC_ENGINE))
                .pattern("AAA").pattern(" B ").pattern("CDC")
                .define('A', Tags.Items.INGOTS_COPPER).define('B', Tags.Items.GLASS)
                .define('C', component(CraftingComponent.MOTOR)).define('D', Blocks.PISTON)
                .unlockedBy("has_motor", has(component(CraftingComponent.MOTOR)))
                .save(consumer, id("crafting/electric_engine"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModMachineBlocks.TUBE_LIGHT.get())
                .pattern(" A ").pattern("BCB")
                .define('A', Tags.Items.INGOTS_IRON).define('B', Tags.Items.NUGGETS_IRON)
                .define('C', Tags.Items.DUSTS_GLOWSTONE)
                .unlockedBy("has_glowstone", has(Tags.Items.DUSTS_GLOWSTONE))
                .save(consumer, id("crafting/tube_light"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModMachineItems.INSERTER_SPEED_UPGRADE.get())
                .pattern("A").pattern("B").define('A', Items.SUGAR).define('B', Blocks.STONE_SLAB)
                .unlockedBy("has_sugar", has(Items.SUGAR))
                .save(consumer, id("crafting/inserter_speed_upgrade"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModMachineItems.INSERTER_STACK_UPGRADE.get())
                .pattern("A").pattern("B").define('A', Tags.Items.CHESTS_WOODEN).define('B', Blocks.STONE_SLAB)
                .unlockedBy("has_chest", has(Tags.Items.CHESTS_WOODEN))
                .save(consumer, id("crafting/inserter_stack_upgrade"));
    }

    private void automationEndpoint(
            Consumer<FinishedRecipe> consumer,
            SingleBlockMachineDefinition definition,
            ItemLike component
    ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(definition))
                .pattern("AAA").pattern("BCB").pattern("BDB")
                .define('A', ItemTags.PLANKS).define('B', Tags.Items.COBBLESTONE)
                .define('C', component).define('D', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/" + definition.id()));
    }

    private void automationEndpoint(
            Consumer<FinishedRecipe> consumer,
            SingleBlockMachineDefinition definition,
            TagKey<Item> component
    ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, machine(definition))
                .pattern("AAA").pattern("BCB").pattern("BDB")
                .define('A', ItemTags.PLANKS).define('B', Tags.Items.COBBLESTONE)
                .define('C', component).define('D', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/" + definition.id()));
    }

    private void addSluiceRecipes(Consumer<FinishedRecipe> consumer) {
        for (Metal metal : Metal.values()) {
            if (!metal.isOre() || !MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                continue;
            }
            List<SluiceRecipe.ChanceOutput> outputs = new ArrayList<>();
            if (metal == Metal.GALENA) {
                outputs.add(chance(ModItems.material(MaterialForm.CHUNK, Metal.LEAD).get(), 1.0F));
                outputs.add(chance(ModItems.material(MaterialForm.CHUNK, Metal.SILVER).get(), 1.0F));
            } else {
                outputs.add(chance(ModItems.material(MaterialForm.CHUNK, metal).get(), 1.0F));
                for (Metal subProduct : sluiceSubProducts(metal)) {
                    outputs.add(chance(ModItems.material(MaterialForm.DUST, subProduct).get(), 0.15F));
                }
            }
            outputs.add(chance(Blocks.COBBLESTONE, 0.15F));
            SingleBlockRecipeBuilder.sluice(
                    consumer,
                    id("sluice/" + metal.id() + "_rocky_chunk"),
                    Ingredient.of(ModItems.material(MaterialForm.ROCKY_CHUNK, metal).get()),
                    outputs
            );
        }
        SingleBlockRecipeBuilder.sluice(
                consumer,
                id("sluice/gravel"),
                Ingredient.of(Blocks.GRAVEL),
                List.of(chance(Items.FLINT, 1.0F), chance(Items.FLINT, 0.15F))
        );
        List<SluiceRecipe.ChanceOutput> gold = new ArrayList<>();
        float chance = 0.01F;
        for (int roll = 0; roll < 9; roll++) {
            gold.add(chance(Items.GOLD_NUGGET, chance));
            chance *= 0.5F;
        }
        SingleBlockRecipeBuilder.sluice(consumer, id("sluice/sand"), Ingredient.of(Blocks.SAND), gold);
    }

    private void addGasificationRecipes(Consumer<FinishedRecipe> consumer) {
        Fluid woodGas = ModFluids.get(FluidDefinition.WOOD_GAS).source().get();
        gasify(consumer, "00_logs", Ingredient.of(ItemTags.LOGS), new ItemStack(Items.CHARCOAL), woodGas, 150, 30, 573.15D);
        gasify(consumer, "10_planks", Ingredient.of(ItemTags.PLANKS), ItemStack.EMPTY, woodGas, 50, 30, 523.15D);
        gasify(consumer, "11_stairs", Ingredient.of(vanillaTag("wooden_stairs")), ItemStack.EMPTY, woodGas, 50, 30, 523.15D);
        gasify(consumer, "12_fences", Ingredient.of(vanillaTag("wooden_fences")), ItemStack.EMPTY, woodGas, 50, 30, 523.15D);
        gasify(consumer, "13_doors", Ingredient.of(ItemTags.WOODEN_DOORS), ItemStack.EMPTY, woodGas, 50, 20, 523.15D);
        gasify(consumer, "14_slabs", Ingredient.of(ItemTags.WOODEN_SLABS), ItemStack.EMPTY, woodGas, 50, 15, 473.15D);
        gasify(consumer, "15_trapdoors", Ingredient.of(ItemTags.WOODEN_TRAPDOORS), ItemStack.EMPTY, woodGas, 50, 15, 473.15D);
        gasify(consumer, "20_saplings", Ingredient.of(ItemTags.SAPLINGS), ItemStack.EMPTY, woodGas, 30, 10, 453.15D);
        gasify(consumer, "21_leaves", Ingredient.of(ItemTags.LEAVES), ItemStack.EMPTY, woodGas, 30, 10, 453.15D);
        gasify(consumer, "30_hay", Ingredient.of(Blocks.HAY_BLOCK), ItemStack.EMPTY, woodGas, 100, 15, 523.15D);
        gasify(consumer, "31_cactus", Ingredient.of(Blocks.CACTUS), ItemStack.EMPTY, woodGas, 10, 10, 453.15D);
        gasify(consumer, "32_chest", Ingredient.of(Tags.Items.CHESTS_WOODEN), ItemStack.EMPTY, woodGas, 50, 30, 473.15D);
        gasify(consumer, "40_stick", Ingredient.of(Tags.Items.RODS_WOODEN), ItemStack.EMPTY, woodGas, 10, 10, 423.15D);
        gasify(consumer, "41_wheat", Ingredient.of(Items.WHEAT), ItemStack.EMPTY, woodGas, 50, 20, 473.15D);
        gasify(consumer, "42_sugar_cane", Ingredient.of(Items.SUGAR_CANE), ItemStack.EMPTY, woodGas, 30, 10, 473.15D);
        gasify(consumer, "43_nether_wart", Ingredient.of(Items.NETHER_WART), ItemStack.EMPTY, woodGas, 50, 10, 473.15D);
        gasify(consumer, "44_carrot", Ingredient.of(Items.CARROT), ItemStack.EMPTY, woodGas, 50, 10, 473.15D);
        gasify(consumer, "45_potato", Ingredient.of(Items.POTATO), ItemStack.EMPTY, woodGas, 50, 10, 473.15D);
        gasify(consumer, "46_beetroot", Ingredient.of(Items.BEETROOT), ItemStack.EMPTY, woodGas, 50, 10, 473.15D);
    }

    private void gasify(
            Consumer<FinishedRecipe> consumer,
            String name,
            Ingredient input,
            ItemStack itemOutput,
            Fluid fluid,
            int fluidAmount,
            int duration,
            double minimumTemperature
    ) {
        SingleBlockRecipeBuilder.gasification(
                consumer,
                id("gasification/" + name),
                input,
                itemOutput,
                new FluidStack(fluid, fluidAmount),
                duration,
                minimumTemperature
        );
    }

    private void addThermopileRecipes(Consumer<FinishedRecipe> consumer) {
        thermopile(consumer, "snow_block", Blocks.SNOW_BLOCK, Map.of(), 273.15D, 40.0D);
        thermopile(consumer, "ice", Blocks.ICE, Map.of(), 273.15D, 60.0D);
        thermopile(consumer, "packed_ice", Blocks.PACKED_ICE, Map.of(), 273.15D, 80.0D);
        thermopile(consumer, "torch", Blocks.TORCH, Map.of(), 1_300.0D, 4.0D);
        thermopile(consumer, "jack_o_lantern", Blocks.JACK_O_LANTERN, Map.of(), 1_300.0D, 3.5D);
        thermopile(consumer, "fire", Blocks.FIRE, Map.of(), 1_300.0D, 4.5D);
        thermopile(consumer, "magma_block", Blocks.MAGMA_BLOCK, Map.of(), 1_000.0D, 1.4D);
        for (int layers = 1; layers <= 8; layers++) {
            thermopile(
                    consumer,
                    "snow_layer_" + layers,
                    Blocks.SNOW,
                    Map.of("layers", Integer.toString(layers)),
                    273.15D,
                    layers / 15.0D * 40.0D
            );
        }
        thermopile(consumer, "water", Blocks.WATER, Map.of(), 300.0D, SingleBlockMachineMath.balancedConductivity(300.0D));
        thermopile(consumer, "lava", Blocks.LAVA, Map.of(), 1_300.0D, SingleBlockMachineMath.balancedConductivity(1_300.0D));
        for (FluidDefinition definition : FluidDefinition.values()) {
            double temperature = definition.temperatureKelvin();
            thermopile(
                    consumer,
                    definition.id(),
                    ModFluids.get(definition).block().get(),
                    Map.of(),
                    temperature,
                    SingleBlockMachineMath.balancedConductivity(temperature)
            );
        }
    }

    private void thermopile(
            Consumer<FinishedRecipe> consumer,
            String name,
            Block block,
            Map<String, String> state,
            double temperature,
            double conductivity
    ) {
        SingleBlockRecipeBuilder.thermopile(
                consumer,
                id("thermopile/" + name),
                block,
                state,
                temperature,
                conductivity
        );
    }

    private void addFluidFuelRecipes(Consumer<FinishedRecipe> consumer) {
        fluidFuel(consumer, FluidDefinition.OIL, 10_000, 30.0D);
        fluidFuel(consumer, FluidDefinition.HEAVY_OIL, 25_000, 60.0D);
        fluidFuel(consumer, FluidDefinition.LIGHT_OIL, 25_000, 80.0D);
        fluidFuel(consumer, FluidDefinition.NATURAL_GAS, 2_500, 40.0D);
        fluidFuel(consumer, FluidDefinition.FUEL, 25_000, 60.0D);
        fluidFuel(consumer, FluidDefinition.DIESEL, 10_000, 80.0D);
        fluidFuel(consumer, FluidDefinition.KEROSENE, 5_000, 120.0D);
        fluidFuel(consumer, FluidDefinition.GASOLINE, 12_000, 100.0D);
        fluidFuel(consumer, FluidDefinition.NAPHTHA, 25_000, 40.0D);
        fluidFuel(consumer, FluidDefinition.WOOD_GAS, 2_500, 20.0D);
    }

    private void fluidFuel(Consumer<FinishedRecipe> consumer, FluidDefinition definition, int duration, double power) {
        SingleBlockRecipeBuilder.fluidFuel(
                consumer,
                id("fluid_fuel/" + definition.id()),
                ModFluids.get(definition).source().get(),
                duration,
                power
        );
    }

    private static List<Metal> sluiceSubProducts(Metal metal) {
        return switch (metal) {
            case IRON -> List.of(Metal.NICKEL, Metal.ALUMINIUM);
            case GOLD -> List.of(Metal.COPPER, Metal.SILVER);
            case COPPER -> List.of(Metal.GOLD, Metal.IRON);
            case LEAD -> List.of(Metal.SILVER);
            case COBALT -> List.of(Metal.MITHRIL, Metal.OSMIUM);
            case TUNGSTEN -> List.of(Metal.IRON);
            case ALUMINIUM -> List.of(Metal.NICKEL, Metal.IRON);
            case GALENA -> List.of(Metal.LEAD, Metal.SILVER);
            case MITHRIL -> List.of(Metal.OSMIUM, Metal.ZINC);
            case NICKEL -> List.of(Metal.IRON, Metal.TIN);
            case OSMIUM -> List.of(Metal.MITHRIL, Metal.NICKEL);
            case SILVER -> List.of(Metal.LEAD);
            case TIN -> List.of(Metal.IRON, Metal.ALUMINIUM);
            case ZINC -> List.of(Metal.NICKEL, Metal.TIN);
            case STEEL -> List.of();
        };
    }

    private static SluiceRecipe.ChanceOutput chance(ItemLike item, float chance) {
        return new SluiceRecipe.ChanceOutput(new ItemStack(item), chance);
    }

    private static TagKey<Item> vanillaTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", path));
    }

    private void crush(
            Consumer<FinishedRecipe> consumer,
            String name,
            Ingredient input,
            ItemLike output,
            int count,
            int requiredLevel
    ) {
        CrushingRecipeBuilder.crushing(input, new ItemStack(output, count), requiredLevel)
                .save(consumer, id("crushing/" + name));
    }

    private void addMaterialConversions(Consumer<FinishedRecipe> consumer) {
        for (Metal metal : Metal.values()) {
            if (!MaterialForm.NUGGET.appliesTo(metal)) {
                continue;
            }
            packAndUnpack(
                    consumer,
                    metal.id() + "_ingot",
                    ModTags.Items.nugget(metal),
                    ModItems.nugget(metal),
                    ModTags.Items.ingot(metal),
                    ModItems.ingot(metal)
            );
        }
    }

    private void addStorageConversions(Consumer<FinishedRecipe> consumer) {
        packAndUnpack(
                consumer,
                "lead_block",
                ModTags.Items.ingot(Metal.LEAD),
                ModItems.ingot(Metal.LEAD),
                ModTags.Items.storageBlock("lead"),
                block(BaseBlockDefinition.LEAD_BLOCK)
        );
        packAndUnpack(
                consumer,
                "cobalt_block",
                ModTags.Items.ingot(Metal.COBALT),
                ModItems.ingot(Metal.COBALT),
                ModTags.Items.storageBlock("cobalt"),
                block(BaseBlockDefinition.COBALT_BLOCK)
        );
        packAndUnpack(
                consumer,
                "tungsten_block",
                ModTags.Items.ingot(Metal.TUNGSTEN),
                ModItems.ingot(Metal.TUNGSTEN),
                ModTags.Items.storageBlock("tungsten"),
                block(BaseBlockDefinition.TUNGSTEN_BLOCK)
        );
        packAndUnpack(
                consumer,
                "sulfur_block",
                ModTags.Items.SULFUR_DUST,
                ModItems.component(CraftingComponent.SULFUR).get(),
                ModTags.Items.storageBlock("sulfur"),
                block(BaseBlockDefinition.SULFUR_BLOCK)
        );
    }

    private void addDecorationRecipes(Consumer<FinishedRecipe> consumer) {
        bricks(consumer, BaseBlockDefinition.LIMESTONE, BaseBlockDefinition.LIMESTONE_BRICKS);
        bricks(consumer, BaseBlockDefinition.BURNT_LIMESTONE, BaseBlockDefinition.BURNT_LIMESTONE_BRICKS);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block(BaseBlockDefinition.LIMESTONE_TILES), 4)
                .pattern("LB")
                .pattern("BL")
                .define('L', block(BaseBlockDefinition.LIMESTONE))
                .define('B', block(BaseBlockDefinition.BURNT_LIMESTONE))
                .unlockedBy("has_burnt_limestone", has(block(BaseBlockDefinition.BURNT_LIMESTONE)))
                .save(consumer, id("crafting/limestone_tiles"));

        ShapedRecipeBuilder.shaped(
                        RecipeCategory.BUILDING_BLOCKS,
                        block(BaseBlockDefinition.INVERTED_LIMESTONE_TILES),
                        4
                )
                .pattern("BL")
                .pattern("LB")
                .define('L', block(BaseBlockDefinition.LIMESTONE))
                .define('B', block(BaseBlockDefinition.BURNT_LIMESTONE))
                .unlockedBy("has_burnt_limestone", has(block(BaseBlockDefinition.BURNT_LIMESTONE)))
                .save(consumer, id("crafting/inverted_limestone_tiles"));
    }

    private void addComponentRecipes(Consumer<FinishedRecipe> consumer) {
        Item alternator = component(CraftingComponent.ALTERNATOR);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, alternator, 4)
                .pattern("LL ")
                .pattern("MRI")
                .pattern("LL ")
                .define('L', ModTags.Items.ingot(Metal.LEAD))
                .define('M', component(CraftingComponent.MAGNET))
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_magnet", has(component(CraftingComponent.MAGNET)))
                .save(consumer, id("crafting/alternator"));

        Item motor = component(CraftingComponent.MOTOR);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, motor, 4)
                .pattern("LL ")
                .pattern("WRI")
                .pattern("LL ")
                .define('L', ModTags.Items.ingot(Metal.LEAD))
                .define('W', component(CraftingComponent.FINE_COPPER_WIRE))
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_fine_copper_wire", has(component(CraftingComponent.FINE_COPPER_WIRE)))
                .save(consumer, id("crafting/motor"));

        Item wire = component(CraftingComponent.FINE_COPPER_WIRE);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, wire, 8)
                .pattern("CCC")
                .pattern("CIC")
                .pattern("CCC")
                .define('C', ModTags.Items.ingot(Metal.COPPER))
                .define('I', ModTags.Items.lightPlate(Metal.IRON))
                .unlockedBy("has_iron_light_plate", has(ModTags.Items.lightPlate(Metal.IRON)))
                .save(consumer, id("crafting/fine_copper_wire"));

        Item magnet = component(CraftingComponent.MAGNET);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, magnet)
                .pattern("LRL")
                .pattern("RIR")
                .pattern("LRL")
                .define('L', Tags.Items.GEMS_LAPIS)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(consumer, id("crafting/magnet"));

        Item ironMesh = component(CraftingComponent.IRON_MESH);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ironMesh)
                .pattern("SSS")
                .pattern("SIS")
                .pattern("SSS")
                .define('S', Tags.Items.STRING)
                .define('I', ModTags.Items.lightPlate(Metal.IRON))
                .unlockedBy("has_iron_light_plate", has(ModTags.Items.lightPlate(Metal.IRON)))
                .save(consumer, id("crafting/iron_mesh"));

        Item fabricMesh = component(CraftingComponent.FABRIC_MESH);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, fabricMesh)
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', Tags.Items.STRING)
                .unlockedBy("has_string", has(Tags.Items.STRING))
                .save(consumer, id("crafting/fabric_mesh"));
    }

    private void addHammerRecipes(Consumer<FinishedRecipe> consumer) {
        hammer(consumer, HammerType.STONE, Tags.Items.COBBLESTONE);
        hammer(consumer, HammerType.IRON, Tags.Items.INGOTS_IRON);
        hammer(consumer, HammerType.STEEL, ModTags.Items.ingot(Metal.STEEL));
    }

    private void addSmeltingRecipes(Consumer<FinishedRecipe> consumer) {
        smelt(
                consumer,
                "galena_ore",
                ModTags.Items.ore("galena"),
                ModItems.ingot(Metal.LEAD),
                1
        );
        smelt(
                consumer,
                "cobalt_ore",
                ModTags.Items.ore("cobalt"),
                ModItems.ingot(Metal.COBALT),
                1
        );
        smelt(
                consumer,
                "tungsten_ore",
                ModTags.Items.ore("tungsten"),
                ModItems.ingot(Metal.TUNGSTEN),
                1
        );

        for (Metal metal : Metal.values()) {
            if (MaterialForm.DUST.appliesTo(metal)) {
                smelt(
                        consumer,
                        metal.id() + "_dust",
                        ModTags.Items.dust(metal),
                        ModItems.ingot(metal),
                        1
                );
            }
            if (MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                Metal product = metal == Metal.GALENA ? Metal.LEAD : metal;
                int count = metal == Metal.GALENA ? 2 : 1;
                smelt(
                        consumer,
                        metal.id() + "_rocky_chunk",
                        ModTags.Items.rockyChunk(metal),
                        ModItems.ingot(product),
                        count
                );
            }
            if (MaterialForm.CHUNK.appliesTo(metal)) {
                smelt(
                        consumer,
                        metal.id() + "_chunk",
                        ModTags.Items.chunk(metal),
                        ModItems.ingot(metal),
                        2
                );
            }
        }

        smelt(
                consumer,
                "limestone",
                block(BaseBlockDefinition.LIMESTONE),
                block(BaseBlockDefinition.BURNT_LIMESTONE),
                1
        );
        smelt(
                consumer,
                "cobbled_limestone",
                block(BaseBlockDefinition.COBBLED_LIMESTONE),
                block(BaseBlockDefinition.COBBLED_BURNT_LIMESTONE),
                1
        );
    }

    private void packAndUnpack(
            Consumer<FinishedRecipe> consumer,
            String idBase,
            TagKey<Item> unpackedTag,
            ItemLike unpackedOutput,
            TagKey<Item> packedTag,
            ItemLike packedOutput
    ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, packedOutput)
                .pattern("MMM")
                .pattern("MMM")
                .pattern("MMM")
                .define('M', unpackedTag)
                .unlockedBy("has_" + idBase + "_material", has(unpackedTag))
                .save(consumer, id("crafting/" + idBase + "_from_material"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, unpackedOutput, 9)
                .requires(packedTag)
                .unlockedBy("has_" + idBase, has(packedTag))
                .save(consumer, id("crafting/" + idBase + "_to_material"));
    }

    private void bricks(
            Consumer<FinishedRecipe> consumer,
            BaseBlockDefinition input,
            BaseBlockDefinition output
    ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block(output), 4)
                .pattern("LL")
                .pattern("LL")
                .define('L', block(input))
                .unlockedBy("has_" + input.id(), has(block(input)))
                .save(consumer, id("crafting/" + output.id()));
    }

    private void hammer(Consumer<FinishedRecipe> consumer, HammerType type, TagKey<Item> material) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.hammer(type).get())
                .pattern("MM ")
                .pattern("MSM")
                .pattern(" S ")
                .define('M', material)
                .define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_" + type.id() + "_material", has(material))
                .save(consumer, id("crafting/" + type.id()));
    }

    private void smelt(
            Consumer<FinishedRecipe> consumer,
            String inputId,
            TagKey<Item> ingredient,
            ItemLike result,
            int count
    ) {
        smelt(consumer, inputId, Ingredient.of(ingredient), result, count, has(ingredient));
    }

    private void smelt(
            Consumer<FinishedRecipe> consumer,
            String inputId,
            ItemLike ingredient,
            ItemLike result,
            int count
    ) {
        smelt(consumer, inputId, Ingredient.of(ingredient), result, count, has(ingredient));
    }

    private void smelt(
            Consumer<FinishedRecipe> consumer,
            String inputId,
            Ingredient ingredient,
            ItemLike result,
            int count,
            CriterionTriggerInstance unlockCriterion
    ) {
        CountedCookingRecipeBuilder.smelting(
                        ingredient,
                        result,
                        count,
                        LEGACY_SMELTING_EXPERIENCE,
                        SMELTING_TIME_TICKS
                )
                .unlockedBy("has_" + inputId, unlockCriterion)
                .save(consumer, id("smelting/" + inputId));
    }

    private record AdvancedProcessingResult(
            ResourceLocation id,
            MultiblockDefinition machine,
            Ingredient input,
            int inputCount,
            List<AdvancedProcessingRecipe.ChanceResult> outputs,
            @Nullable HydraulicPressMode pressMode,
            int duration,
            int energyPerTick
    ) implements FinishedRecipe {
        private AdvancedProcessingResult {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(machine, "machine");
            Objects.requireNonNull(input, "input");
            outputs = List.copyOf(outputs);
            if (inputCount <= 0 || outputs.isEmpty() || outputs.size() > 3) {
                throw new IllegalArgumentException("Advanced processing recipes require one to three outputs");
            }
            if ((machine == MultiblockDefinition.HYDRAULIC_PRESS) != (pressMode != null)) {
                throw new IllegalArgumentException("Hydraulic mode must be present only for press recipes");
            }
            if (duration <= 0 || energyPerTick < 0) {
                throw new IllegalArgumentException("Invalid advanced processing cost for " + id);
            }
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("machine", machine.id());
            json.add("ingredient", input.toJson());
            if (inputCount != 1) {
                json.addProperty("input_count", inputCount);
            }
            if (pressMode != null) {
                json.addProperty("press_mode", pressMode.serializedName());
            }
            JsonArray results = new JsonArray();
            for (AdvancedProcessingRecipe.ChanceResult output : outputs) {
                JsonObject result = new JsonObject();
                result.addProperty(
                        "item",
                        Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(output.stack().getItem())).toString()
                );
                if (output.stack().getCount() != 1) {
                    result.addProperty("count", output.stack().getCount());
                }
                if (output.chance() != 1.0F) {
                    result.addProperty("chance", output.chance());
                }
                results.add(result);
            }
            json.add("results", results);
            json.addProperty("duration", duration);
            json.addProperty("energy_per_tick", energyPerTick);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.ADVANCED_PROCESSING_SERIALIZER.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }

    private static Item component(CraftingComponent component) {
        return ModItems.component(component).get();
    }

    private static ItemLike block(BaseBlockDefinition definition) {
        return ModBlocks.get(definition).get();
    }

    private static Block machine(SingleBlockMachineDefinition definition) {
        return ModMachineBlocks.machine(definition).get();
    }

    private static ResourceLocation id(String path) {
        return Magneticraft.id(path);
    }
}
