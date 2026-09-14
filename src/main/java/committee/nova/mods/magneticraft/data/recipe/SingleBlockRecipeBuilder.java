package committee.nova.mods.magneticraft.data.recipe;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Map;

/**
 * Datagen-only builders for the single-block machine recipe types.
 */
public final class SingleBlockRecipeBuilder {
    private SingleBlockRecipeBuilder() {
    }

    public static void sluice(
            RecipeOutput recipeOutput,
            ResourceLocation id,
            Ingredient input,
            List<SluiceRecipe.ChanceOutput> outputs
    ) {
        recipeOutput.accept(id, new SluiceRecipe(input, outputs), null);
    }

    public static void gasification(
            RecipeOutput recipeOutput,
            ResourceLocation id,
            Ingredient input,
            ItemStack itemOutput,
            FluidStack fluidOutput,
            int duration,
            double minimumTemperature
    ) {
        recipeOutput.accept(
                id,
                new GasificationRecipe(input, itemOutput, fluidOutput, duration, minimumTemperature),
                null
        );
    }

    public static void thermopile(
            RecipeOutput recipeOutput,
            ResourceLocation id,
            Block block,
            Map<String, String> state,
            double temperature,
            double conductivity
    ) {
        recipeOutput.accept(id, new ThermopileRecipe(block, state, temperature, conductivity), null);
    }

    public static void fluidFuel(
            RecipeOutput recipeOutput,
            ResourceLocation id,
            Fluid fluid,
            int duration,
            double power
    ) {
        recipeOutput.accept(id, new FluidFuelRecipe(fluid, duration, power), null);
    }
}
