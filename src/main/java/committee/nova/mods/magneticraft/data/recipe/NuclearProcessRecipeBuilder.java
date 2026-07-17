package committee.nova.mods.magneticraft.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearProcessRecipe;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Datagen adapter for the strict nuclear-processing recipe contract. */
public final class NuclearProcessRecipeBuilder {
    private NuclearProcessRecipeBuilder() {
    }

    public static void save(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            NuclearFacilityType facility,
            List<NuclearProcessRecipe.CountedIngredient> ingredients,
            List<ItemStack> results,
            int durationTicks,
            int joulesPerTick
    ) {
        consumer.accept(new Result(id, facility, List.copyOf(ingredients),
                results.stream().map(ItemStack::copy).toList(), durationTicks, joulesPerTick));
    }

    private record Result(
            ResourceLocation id,
            NuclearFacilityType facility,
            List<NuclearProcessRecipe.CountedIngredient> ingredients,
            List<ItemStack> results,
            int durationTicks,
            int joulesPerTick
    ) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("facility", facility.id());
            JsonArray ingredientArray = new JsonArray();
            for (NuclearProcessRecipe.CountedIngredient counted : ingredients) {
                JsonObject entry = new JsonObject();
                entry.add("ingredient", counted.ingredient().toJson());
                if (counted.count() != 1) {
                    entry.addProperty("count", counted.count());
                }
                ingredientArray.add(entry);
            }
            json.add("ingredients", ingredientArray);

            JsonArray resultArray = new JsonArray();
            for (ItemStack stack : results) {
                JsonObject entry = new JsonObject();
                entry.addProperty("item", Objects.requireNonNull(
                        ForgeRegistries.ITEMS.getKey(stack.getItem())).toString());
                if (stack.getCount() != 1) {
                    entry.addProperty("count", stack.getCount());
                }
                resultArray.add(entry);
            }
            json.add("results", resultArray);
            json.addProperty("duration_ticks", durationTicks);
            json.addProperty("joules_per_tick", joulesPerTick);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.NUCLEAR_PROCESSING_SERIALIZER.get();
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
}
