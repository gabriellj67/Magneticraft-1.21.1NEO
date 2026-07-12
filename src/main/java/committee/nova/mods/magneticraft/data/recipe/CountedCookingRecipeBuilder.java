package committee.nova.mods.magneticraft.data.recipe;

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Emits Forge's counted cooking-result JSON extension while retaining vanilla smelting semantics.
 *
 * <p>Adapted from MochiButter/Magneticraft's GPL-2.0 FurnaceMultiRecipeBuilder and reduced to the
 * single smelting behavior required by the Nova 1.12 recipe contract.</p>
 */
public final class CountedCookingRecipeBuilder implements RecipeBuilder {
    private final Item result;
    private final int count;
    private final Ingredient ingredient;
    private final float experience;
    private final int cookingTime;
    private final RecipeSerializer<? extends AbstractCookingRecipe> serializer;
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
    private String group = "";

    private CountedCookingRecipeBuilder(
            Ingredient ingredient,
            ItemLike result,
            int count,
            float experience,
            int cookingTime,
            RecipeSerializer<? extends AbstractCookingRecipe> serializer
    ) {
        if (count < 1) {
            throw new IllegalArgumentException("Cooking result count must be positive: " + count);
        }
        if (cookingTime < 1) {
            throw new IllegalArgumentException("Cooking time must be positive: " + cookingTime);
        }
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
        this.result = Objects.requireNonNull(result, "result").asItem();
        this.count = count;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.serializer = Objects.requireNonNull(serializer, "serializer");
    }

    public static CountedCookingRecipeBuilder smelting(
            Ingredient ingredient,
            ItemLike result,
            int count,
            float experience,
            int cookingTime
    ) {
        return new CountedCookingRecipeBuilder(
                ingredient,
                result,
                count,
                experience,
                cookingTime,
                RecipeSerializer.SMELTING_RECIPE
        );
    }

    @Override
    public CountedCookingRecipeBuilder unlockedBy(
            String criterionName,
            CriterionTriggerInstance criterionTrigger
    ) {
        advancement.addCriterion(criterionName, criterionTrigger);
        return this;
    }

    @Override
    public CountedCookingRecipeBuilder group(@Nullable String group) {
        this.group = group == null ? "" : group;
        return this;
    }

    @Override
    public Item getResult() {
        return result;
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation recipeId) {
        ResourceLocation advancementId = recipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/");
        advancement.parent(ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(RequirementsStrategy.OR);
        consumer.accept(new Result(
                recipeId,
                group,
                ingredient,
                result,
                count,
                experience,
                cookingTime,
                advancement,
                advancementId,
                serializer
        ));
    }

    private record Result(
            ResourceLocation id,
            String group,
            Ingredient ingredient,
            Item result,
            int count,
            float experience,
            int cookingTime,
            Advancement.Builder advancement,
            ResourceLocation advancementId,
            RecipeSerializer<? extends AbstractCookingRecipe> serializer
    ) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            if (!group.isEmpty()) {
                json.addProperty("group", group);
            }
            json.addProperty("category", CookingBookCategory.MISC.getSerializedName());
            json.add("ingredient", ingredient.toJson());

            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(result)).toString());
            if (count > 1) {
                resultJson.addProperty("count", count);
            }
            json.add("result", resultJson);
            json.addProperty("experience", experience);
            json.addProperty("cookingtime", cookingTime);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return serializer;
        }

        @Override
        public @Nullable JsonObject serializeAdvancement() {
            return advancement.serializeToJson();
        }

        @Override
        public @Nullable ResourceLocation getAdvancementId() {
            return advancementId;
        }
    }
}
