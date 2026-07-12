package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Heat-driven solid-to-gas conversion.
 */
public final class GasificationRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack itemOutput;
    private final FluidStack fluidOutput;
    private final int durationTicks;
    private final double minimumTemperatureKelvin;

    public GasificationRecipe(
            ResourceLocation id,
            Ingredient input,
            ItemStack itemOutput,
            FluidStack fluidOutput,
            int durationTicks,
            double minimumTemperatureKelvin
    ) {
        this.id = Objects.requireNonNull(id);
        this.input = Objects.requireNonNull(input);
        this.itemOutput = itemOutput.copy();
        this.fluidOutput = fluidOutput.copy();
        this.durationTicks = Math.max(1, durationTicks);
        this.minimumTemperatureKelvin = Math.max(0.0D, minimumTemperatureKelvin);
        if (this.itemOutput.isEmpty() && this.fluidOutput.isEmpty()) {
            throw new IllegalArgumentException("Gasification recipe has no output");
        }
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return itemOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return itemOutput.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.GASIFICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.GASIFICATION_TYPE.get();
    }

    public Ingredient input() {
        return input;
    }

    public ItemStack itemOutput() {
        return itemOutput.copy();
    }

    public FluidStack fluidOutput() {
        return fluidOutput.copy();
    }

    public int durationTicks() {
        return durationTicks;
    }

    public double minimumTemperatureKelvin() {
        return minimumTemperatureKelvin;
    }

    public static final class Serializer implements RecipeSerializer<GasificationRecipe> {
        @Override
        public GasificationRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            ItemStack itemOutput = json.has("item_result")
                    ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "item_result"))
                    : ItemStack.EMPTY;
            FluidStack fluidOutput = readFluid(GsonHelper.getAsJsonObject(json, "fluid_result"));
            return new GasificationRecipe(
                    id,
                    input,
                    itemOutput,
                    fluidOutput,
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsDouble(json, "minimum_temperature")
            );
        }

        @Nullable
        @Override
        public GasificationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            ItemStack itemOutput = buffer.readItem();
            ResourceLocation fluidId = buffer.readResourceLocation();
            int amount = buffer.readVarInt();
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluidId), "Unknown fluid " + fluidId);
            return new GasificationRecipe(
                    id,
                    input,
                    itemOutput,
                    new FluidStack(fluid, amount),
                    buffer.readVarInt(),
                    buffer.readDouble()
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GasificationRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeItem(recipe.itemOutput);
            buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(recipe.fluidOutput.getFluid())));
            buffer.writeVarInt(recipe.fluidOutput.getAmount());
            buffer.writeVarInt(recipe.durationTicks);
            buffer.writeDouble(recipe.minimumTemperatureKelvin);
        }

        private static FluidStack readFluid(JsonObject json) {
            ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(id), "Unknown fluid " + id);
            return new FluidStack(fluid, GsonHelper.getAsInt(json, "amount"));
        }
    }
}
