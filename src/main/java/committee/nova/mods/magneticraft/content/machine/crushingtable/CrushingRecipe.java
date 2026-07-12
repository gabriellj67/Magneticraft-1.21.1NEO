package committee.nova.mods.magneticraft.content.machine.crushingtable;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Data-driven crushing-table conversion.
 */
public final class CrushingRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;
    private final int requiredLevel;

    public CrushingRecipe(ResourceLocation id, Ingredient input, ItemStack output, int requiredLevel) {
        this.id = id;
        this.input = input;
        this.output = output.copy();
        this.requiredLevel = Math.max(-1, requiredLevel);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CRUSHING_TYPE.get();
    }

    public Ingredient input() {
        return input;
    }

    public ItemStack output() {
        return output.copy();
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public static final class Serializer implements RecipeSerializer<CrushingRecipe> {
        @Override
        public CrushingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int requiredLevel = GsonHelper.getAsInt(json, "required_level", -1);
            return new CrushingRecipe(id, input, output, requiredLevel);
        }

        @Nullable
        @Override
        public CrushingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ItemStack output = buffer.readItem();
            int requiredLevel = buffer.readVarInt() - 1;
            return new CrushingRecipe(id, input, output, requiredLevel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CrushingRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeItem(recipe.output);
            buffer.writeVarInt(recipe.requiredLevel + 1);
        }
    }
}
