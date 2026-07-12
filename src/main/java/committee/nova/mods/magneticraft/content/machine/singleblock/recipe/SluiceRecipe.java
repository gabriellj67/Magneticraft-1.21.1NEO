package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One sluice input and independently rolled outputs.
 */
public final class SluiceRecipe implements Recipe<SimpleContainer> {
    public static final int MAX_OUTPUTS = 10;

    private final ResourceLocation id;
    private final Ingredient input;
    private final List<ChanceOutput> outputs;

    public SluiceRecipe(ResourceLocation id, Ingredient input, List<ChanceOutput> outputs) {
        this.id = Objects.requireNonNull(id);
        this.input = Objects.requireNonNull(input);
        validateOutputCount(outputs.size());
        this.outputs = outputs.stream().map(ChanceOutput::copy).toList();
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputs.stream()
                .filter(output -> output.chance() >= 1.0F && !output.stack().isEmpty())
                .map(ChanceOutput::stack)
                .findFirst()
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SLUICE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SLUICE_TYPE.get();
    }

    public Ingredient input() {
        return input;
    }

    public List<ChanceOutput> outputs() {
        return outputs.stream().map(ChanceOutput::copy).toList();
    }

    static void validateOutputCount(int count) {
        if (count < 1 || count > MAX_OUTPUTS) {
            throw new IllegalArgumentException(
                    "A sluice recipe needs between 1 and " + MAX_OUTPUTS + " output rolls: " + count
            );
        }
    }

    public record ChanceOutput(ItemStack stack, float chance) {
        public ChanceOutput {
            stack = stack.copy();
            if (!Float.isFinite(chance) || chance < 0.0F || chance > 1.0F) {
                throw new IllegalArgumentException("Sluice output chance must be in [0, 1]");
            }
        }

        private ChanceOutput copy() {
            return new ChanceOutput(stack, chance);
        }
    }

    public static final class Serializer implements RecipeSerializer<SluiceRecipe> {
        @Override
        public SluiceRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            JsonArray array = GsonHelper.getAsJsonArray(json, "results");
            validateOutputCount(array.size());
            List<ChanceOutput> outputs = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                JsonObject output = GsonHelper.convertToJsonObject(element, "result");
                ItemStack stack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(output, "stack"));
                outputs.add(new ChanceOutput(stack, GsonHelper.getAsFloat(output, "chance", 1.0F)));
            }
            return new SluiceRecipe(id, input, outputs);
        }

        @Nullable
        @Override
        public SluiceRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int size = buffer.readVarInt();
            validateOutputCount(size);
            List<ChanceOutput> outputs = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                outputs.add(new ChanceOutput(buffer.readItem(), buffer.readFloat()));
            }
            return new SluiceRecipe(id, input, outputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SluiceRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.outputs.size());
            for (ChanceOutput output : recipe.outputs) {
                buffer.writeItem(output.stack);
                buffer.writeFloat(output.chance);
            }
        }
    }
}
