package committee.nova.mods.magneticraft.data.recipe;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.function.Consumer;

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

    public void save(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        consumer.accept(new Result(id, input, output, requiredLevel));
    }

    private record Result(
            ResourceLocation id,
            Ingredient input,
            ItemStack output,
            int requiredLevel
    ) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("ingredient", input.toJson());
            JsonObject result = new JsonObject();
            ResourceLocation itemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(output.getItem()));
            result.addProperty("item", itemId.toString());
            if (output.getCount() != 1) {
                result.addProperty("count", output.getCount());
            }
            json.add("result", result);
            if (requiredLevel >= 0) {
                json.addProperty("required_level", requiredLevel);
            }
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.CRUSHING_SERIALIZER.get();
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
