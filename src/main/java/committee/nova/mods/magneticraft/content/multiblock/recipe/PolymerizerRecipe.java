package committee.nova.mods.magneticraft.content.multiblock.recipe;

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
import java.util.Optional;

/** Heat-driven fluid polymerization with an optional solid reagent. */
public final class PolymerizerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Optional<Ingredient> ingredient;
    private final FluidStack fluidInput;
    private final ItemStack result;
    private final int duration;
    private final double minimumTemperatureKelvin;
    private final double heatPerTick;

    public PolymerizerRecipe(
            ResourceLocation id,
            Optional<Ingredient> ingredient,
            FluidStack fluidInput,
            ItemStack result,
            int duration,
            double minimumTemperatureKelvin,
            double heatPerTick
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
        this.fluidInput = Objects.requireNonNull(fluidInput, "fluidInput").copy();
        this.result = Objects.requireNonNull(result, "result").copy();
        if (this.fluidInput.isEmpty() || this.fluidInput.getAmount() <= 0) {
            throw new IllegalArgumentException("Polymerizer fluid input must be non-empty");
        }
        if (this.result.isEmpty()) {
            throw new IllegalArgumentException("Polymerizer result must be non-empty");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Polymerizer duration must be positive");
        }
        if (!Double.isFinite(minimumTemperatureKelvin) || minimumTemperatureKelvin < 0.0D) {
            throw new IllegalArgumentException("Polymerizer minimum temperature must be finite and non-negative");
        }
        if (!Double.isFinite(heatPerTick) || heatPerTick <= 0.0D) {
            throw new IllegalArgumentException("Polymerizer heat cost must be finite and positive");
        }
        this.duration = duration;
        this.minimumTemperatureKelvin = minimumTemperatureKelvin;
        this.heatPerTick = heatPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return ingredient.isEmpty() || ingredient.get().test(container.getItem(0));
    }

    public boolean matches(ItemStack stack, FluidStack fluid) {
        return matchesIngredient(stack)
                && !fluid.isEmpty()
                && fluid.getFluid() == fluidInput.getFluid()
                && fluid.getAmount() >= fluidInput.getAmount();
    }

    public boolean matchesIngredient(ItemStack stack) {
        return ingredient.isEmpty() || ingredient.get().test(stack);
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return ingredient.isEmpty() || width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.POLYMERIZING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.POLYMERIZING_TYPE.get();
    }

    public Optional<Ingredient> ingredient() {
        return ingredient;
    }

    public FluidStack fluidInput() {
        return fluidInput.copy();
    }

    public ItemStack result() {
        return result.copy();
    }

    public int duration() {
        return duration;
    }

    public double minimumTemperatureKelvin() {
        return minimumTemperatureKelvin;
    }

    public double heatPerTick() {
        return heatPerTick;
    }

    public static final class Serializer implements RecipeSerializer<PolymerizerRecipe> {
        @Override
        public PolymerizerRecipe fromJson(ResourceLocation id, JsonObject json) {
            Optional<Ingredient> ingredient = json.has("ingredient")
                    ? Optional.of(Ingredient.fromJson(json.get("ingredient")))
                    : Optional.empty();
            JsonObject fluidJson = GsonHelper.getAsJsonObject(json, "fluid");
            Fluid fluid = requiredFluid(ResourceLocation.parse(GsonHelper.getAsString(fluidJson, "fluid")));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new PolymerizerRecipe(
                    id,
                    ingredient,
                    new FluidStack(fluid, GsonHelper.getAsInt(fluidJson, "amount")),
                    result,
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsDouble(json, "minimum_temperature"),
                    GsonHelper.getAsDouble(json, "heat_per_tick")
            );
        }

        @Nullable
        @Override
        public PolymerizerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Optional<Ingredient> ingredient = buffer.readBoolean()
                    ? Optional.of(Ingredient.fromNetwork(buffer))
                    : Optional.empty();
            Fluid fluid = requiredFluid(buffer.readResourceLocation());
            FluidStack fluidInput = new FluidStack(fluid, buffer.readVarInt());
            return new PolymerizerRecipe(
                    id,
                    ingredient,
                    fluidInput,
                    buffer.readItem(),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PolymerizerRecipe recipe) {
            buffer.writeBoolean(recipe.ingredient.isPresent());
            recipe.ingredient.ifPresent(value -> value.toNetwork(buffer));
            buffer.writeResourceLocation(Objects.requireNonNull(
                    ForgeRegistries.FLUIDS.getKey(recipe.fluidInput.getFluid())
            ));
            buffer.writeVarInt(recipe.fluidInput.getAmount());
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.duration);
            buffer.writeDouble(recipe.minimumTemperatureKelvin);
            buffer.writeDouble(recipe.heatPerTick);
        }

        private static Fluid requiredFluid(ResourceLocation id) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
            if (fluid == null) {
                throw new IllegalArgumentException("Unknown polymerizer fluid " + id);
            }
            return fluid;
        }
    }
}
