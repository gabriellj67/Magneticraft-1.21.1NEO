package committee.nova.mods.magneticraft.data.recipe;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/** Adds the electrical result payload while retaining vanilla shaped recipe generation. */
public final class TieredShapedFinishedRecipe {
    private TieredShapedFinishedRecipe() {
    }

    public static void save(
            ShapedRecipeBuilder builder,
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            TieredElectricalItemData itemData
    ) {
        Objects.requireNonNull(builder, "builder").save(
                vanilla -> consumer.accept(new Result(vanilla, itemData)),
                Objects.requireNonNull(id, "id")
        );
    }

    private record Result(FinishedRecipe vanilla, TieredElectricalItemData itemData) implements FinishedRecipe {
        private Result {
            Objects.requireNonNull(vanilla, "vanilla");
            Objects.requireNonNull(itemData, "itemData");
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            vanilla.serializeRecipeData(json);
            json.add("electrical", itemData.toJson());
        }

        @Override
        public ResourceLocation getId() {
            return vanilla.getId();
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.TIERED_SHAPED_SERIALIZER.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return vanilla.serializeAdvancement();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return vanilla.getAdvancementId();
        }
    }
}
