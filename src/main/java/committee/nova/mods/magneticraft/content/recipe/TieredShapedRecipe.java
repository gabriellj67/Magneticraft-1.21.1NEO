package committee.nova.mods.magneticraft.content.recipe;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Vanilla shaped matching with a versioned electrical payload added to the result. */
public final class TieredShapedRecipe implements net.minecraft.world.item.crafting.Recipe<CraftingContainer> {
    private static final String ELECTRICAL_TAG = "electrical";

    private final ShapedRecipe delegate;
    private final TieredElectricalItemData itemData;

    public TieredShapedRecipe(ShapedRecipe delegate, TieredElectricalItemData itemData) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.itemData = Objects.requireNonNull(itemData, "itemData");
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return delegate.matches(container, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = delegate.assemble(container, registryAccess);
        itemData.write(result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return delegate.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ItemStack result = delegate.getResultItem(registryAccess).copy();
        itemData.write(result);
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return delegate.getIngredients();
    }

    @Override
    public String getGroup() {
        return delegate.getGroup();
    }

    @Override
    public ResourceLocation getId() {
        return delegate.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TIERED_SHAPED_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public ShapedRecipe delegate() {
        return delegate;
    }

    public TieredElectricalItemData itemData() {
        return itemData;
    }

    public static final class Serializer implements RecipeSerializer<TieredShapedRecipe> {
        @Override
        public TieredShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe delegate = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            TieredElectricalItemData itemData = TieredElectricalItemData.fromJson(
                    GsonHelper.getAsJsonObject(json, ELECTRICAL_TAG)
            );
            return new TieredShapedRecipe(delegate, itemData);
        }

        @Nullable
        @Override
        public TieredShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapedRecipe delegate = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
            return delegate == null ? null : new TieredShapedRecipe(delegate, TieredElectricalItemData.readNetwork(buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, TieredShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe.delegate);
            recipe.itemData.writeNetwork(buffer);
        }
    }
}
