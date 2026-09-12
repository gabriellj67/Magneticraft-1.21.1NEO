package committee.nova.mods.magneticraft.content.multiblock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;
import java.util.Optional;

/**
 * Heat-driven fluid polymerization with an optional solid reagent.
 *
 * <p>Ported for the same 1.21.1 recipe rewrite as {@link AdvancedProcessingRecipe} - see that
 * class's header for the full writeup (id moved to {@code RecipeHolder}, {@code Container} ->
 * {@code RecipeInput}, {@code fromJson}/{@code toNetwork} -> {@code codec()}/{@code streamCodec()}).
 * This recipe still matches against both an item and a fluid simultaneously
 * ({@link #matches(ItemStack, FluidStack)}); the vanilla single-argument-input
 * {@link Recipe#matches(net.minecraft.world.item.crafting.RecipeInput, Level)} overload (backed by
 * {@link SingleRecipeInput}, since this recipe only ever inspects one item slot) is never actually
 * invoked by {@code AdvancedMultiblockLogic}, which always calls the two-argument overload directly.</p>
 */
public final class PolymerizerRecipe implements Recipe<SingleRecipeInput> {
    private final Optional<Ingredient> ingredient;
    private final FluidStack fluidInput;
    private final ItemStack result;
    private final int duration;
    private final double minimumTemperatureKelvin;
    private final double heatPerTick;

    public PolymerizerRecipe(
            Optional<Ingredient> ingredient,
            FluidStack fluidInput,
            ItemStack result,
            int duration,
            double minimumTemperatureKelvin,
            double heatPerTick
    ) {
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.isEmpty() || ingredient.get().test(input.item());
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
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return ingredient.isEmpty() || width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
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
        private static final MapCodec<PolymerizerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(PolymerizerRecipe::ingredient),
                FluidStack.CODEC.fieldOf("fluid").forGetter(PolymerizerRecipe::fluidInput),
                ItemStack.CODEC.fieldOf("result").forGetter(PolymerizerRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(PolymerizerRecipe::duration),
                Codec.doubleRange(0.0D, Double.MAX_VALUE)
                        .fieldOf("minimum_temperature")
                        .forGetter(PolymerizerRecipe::minimumTemperatureKelvin),
                Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE)
                        .fieldOf("heat_per_tick")
                        .forGetter(PolymerizerRecipe::heatPerTick)
        ).apply(instance, PolymerizerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PolymerizerRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), PolymerizerRecipe::ingredient,
                FluidStack.STREAM_CODEC, PolymerizerRecipe::fluidInput,
                ItemStack.STREAM_CODEC, PolymerizerRecipe::result,
                ByteBufCodecs.VAR_INT, PolymerizerRecipe::duration,
                ByteBufCodecs.DOUBLE, PolymerizerRecipe::minimumTemperatureKelvin,
                ByteBufCodecs.DOUBLE, PolymerizerRecipe::heatPerTick,
                PolymerizerRecipe::new
        );

        @Override
        public MapCodec<PolymerizerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PolymerizerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
