package committee.nova.mods.magneticraft.data.recipe;

import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearProcessRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Datagen adapter for the strict nuclear-processing recipe contract. */
public final class NuclearProcessRecipeBuilder {
    private NuclearProcessRecipeBuilder() {
    }

    public static void save(
            RecipeOutput recipeOutput,
            ResourceLocation id,
            NuclearFacilityType facility,
            List<NuclearProcessRecipe.CountedIngredient> ingredients,
            List<ItemStack> results,
            int durationTicks,
            int joulesPerTick
    ) {
        recipeOutput.accept(
                id,
                new NuclearProcessRecipe(facility, ingredients, results, durationTicks, joulesPerTick),
                null
        );
    }
}
