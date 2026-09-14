package committee.nova.mods.magneticraft.data.recipe;

import committee.nova.mods.magneticraft.content.recipe.TieredShapedRecipe;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Wraps the vanilla shaped-recipe output with the electrical result payload. */
public final class TieredShapedFinishedRecipe {
    private TieredShapedFinishedRecipe() {
    }

    public static void save(
            ShapedRecipeBuilder builder,
            RecipeOutput recipeOutput,
            ResourceLocation id,
            TieredElectricalItemData itemData
    ) {
        Objects.requireNonNull(builder, "builder").save(
                new Intercept(Objects.requireNonNull(recipeOutput, "recipeOutput"), itemData),
                Objects.requireNonNull(id, "id")
        );
    }

    private record Intercept(RecipeOutput delegate, TieredElectricalItemData itemData) implements RecipeOutput {
        private Intercept {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(itemData, "itemData");
        }

        @Override
        public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
            if (!(recipe instanceof ShapedRecipe shapedRecipe)) {
                throw new IllegalStateException("Expected a ShapedRecipe for " + id + ", got " + recipe);
            }
            delegate.accept(id, new TieredShapedRecipe(shapedRecipe, itemData), advancement, conditions);
        }

        @Override
        public Advancement.Builder advancement() {
            return delegate.advancement();
        }
    }
}
