package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

/** Optional JEI entry point. JEI owns discovery, so the common mod never links these API types. */
@JeiPlugin
public final class MagneticraftJeiPlugin implements IModPlugin {
    public static final RecipeType<CrushingRecipe> CRUSHING = type("crushing_table", CrushingRecipe.class);
    public static final RecipeType<SluiceRecipe> SLUICE = type("sluice_box", SluiceRecipe.class);
    public static final RecipeType<GasificationRecipe> GASIFICATION = type("gasification_unit", GasificationRecipe.class);
    public static final RecipeType<ThermopileRecipe> THERMOPILE = type("thermopile", ThermopileRecipe.class);
    public static final RecipeType<FluidFuelRecipe> FLUID_FUEL = type("fluid_fuel", FluidFuelRecipe.class);
    public static final RecipeType<AdvancedProcessingRecipe> ADVANCED_PROCESSING =
            type("advanced_processing", AdvancedProcessingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return Magneticraft.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CrushingRecipeCategory(guiHelper),
                new SluiceRecipeCategory(guiHelper),
                new GasificationRecipeCategory(guiHelper),
                new ThermopileRecipeCategory(guiHelper),
                new FluidFuelRecipeCategory(guiHelper),
                new AdvancedProcessingRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RecipeManager recipes = level.getRecipeManager();
        registration.addRecipes(CRUSHING, recipes.getAllRecipesFor(ModRecipeTypes.CRUSHING_TYPE.get()));
        registration.addRecipes(SLUICE, recipes.getAllRecipesFor(ModRecipeTypes.SLUICE_TYPE.get()));
        registration.addRecipes(GASIFICATION, recipes.getAllRecipesFor(ModRecipeTypes.GASIFICATION_TYPE.get()));
        registration.addRecipes(THERMOPILE, recipes.getAllRecipesFor(ModRecipeTypes.THERMOPILE_TYPE.get()));
        registration.addRecipes(FLUID_FUEL, recipes.getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get()));
        registration.addRecipes(
                ADVANCED_PROCESSING,
                recipes.getAllRecipesFor(ModRecipeTypes.ADVANCED_PROCESSING_TYPE.get())
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CRUSHING, ModMachineBlocks.CRUSHING_TABLE.get());
        registration.addRecipeCatalysts(
                SLUICE,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.SLUICE_BOX).get()
        );
        registration.addRecipeCatalysts(
                GASIFICATION,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.GASIFICATION_UNIT).get()
        );
        registration.addRecipeCatalysts(
                THERMOPILE,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.THERMOPILE).get()
        );
        registration.addRecipeCatalysts(
                FLUID_FUEL,
                ModAdvancedBlocks.controller(MultiblockDefinition.BIG_COMBUSTION_CHAMBER).get()
        );
        registration.addRecipeCatalysts(
                ADVANCED_PROCESSING,
                ModAdvancedBlocks.controller(MultiblockDefinition.GRINDER).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.SIEVE).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.HYDRAULIC_PRESS).get()
        );
    }

    private static <T> RecipeType<T> type(String path, Class<? extends T> recipeClass) {
        return RecipeType.create(Magneticraft.MOD_ID, path, recipeClass);
    }
}
