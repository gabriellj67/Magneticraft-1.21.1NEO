package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.data.recipe.CountedCookingRecipeBuilder;
import committee.nova.mods.magneticraft.data.recipe.CrushingRecipeBuilder;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;

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
        addCrushingRecipes(consumer);
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

        // Temporary dependency bridge. Delete when brick_furnace and the heat network are migrated.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMachineBlocks.ELECTRIC_FURNACE.get())
                .pattern("ABA")
                .pattern("CCC")
                .define('A', ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get())
                .define('B', Blocks.FURNACE)
                .define('C', ModTags.Items.ingot(Metal.COPPER))
                .unlockedBy("has_fine_copper_wire", has(ModItems.component(CraftingComponent.FINE_COPPER_WIRE).get()))
                .save(consumer, id("crafting/electric_furnace_temporary"));

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

    private static Item component(CraftingComponent component) {
        return ModItems.component(component).get();
    }

    private static ItemLike block(BaseBlockDefinition definition) {
        return ModBlocks.get(definition).get();
    }

    private static ResourceLocation id(String path) {
        return Magneticraft.id(path);
    }
}
