package committee.nova.mods.magneticraft.data.recipe;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Objects;

/**
 * Datagen-only builder for Magneticraft crushing recipes.
 */
public final class CrushingRecipeBuilder {
    private final Ingredient input;
    private final ItemStack output;
    private final int requiredLevel;

    private CrushingRecipeBuilder(Ingredient input, ItemStack output, int requiredLevel) {
        this.input = Objects.requireNonNull(input);
        this.output = output.copy();
        this.requiredLevel = Math.max(-1, requiredLevel);
    }

    public static CrushingRecipeBuilder crushing(Ingredient input, ItemStack output, int requiredLevel) {
        return new CrushingRecipeBuilder(input, output, requiredLevel);
    }

    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        recipeOutput.accept(id, new CrushingRecipe(input, output, requiredLevel), null);
    }
}
