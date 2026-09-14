package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

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

/**
 * Heat-driven solid-to-gas conversion.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (no more {@code Recipe#getId()};
 * {@code Container} -> {@code RecipeInput}; {@code fromJson}/{@code toNetwork} ->
 * {@code codec()}/{@code streamCodec()}) - see {@code AdvancedProcessingRecipe} in
 * {@code content/multiblock/recipe} for the full writeup of this rewrite.</p>
 */
public final class GasificationRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack itemOutput;
    private final FluidStack fluidOutput;
    private final int durationTicks;
    private final double minimumTemperatureKelvin;

    public GasificationRecipe(
            Ingredient input,
            ItemStack itemOutput,
            FluidStack fluidOutput,
            int durationTicks,
            double minimumTemperatureKelvin
    ) {
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return itemOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemOutput.copy();
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
        private static final MapCodec<GasificationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(GasificationRecipe::input),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("item_result", ItemStack.EMPTY)
                        .forGetter(GasificationRecipe::itemOutput),
                FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid_result", FluidStack.EMPTY)
                        .forGetter(GasificationRecipe::fluidOutput),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(GasificationRecipe::durationTicks),
                Codec.doubleRange(0.0D, Double.MAX_VALUE)
                        .fieldOf("minimum_temperature")
                        .forGetter(GasificationRecipe::minimumTemperatureKelvin)
        ).apply(instance, GasificationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GasificationRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, GasificationRecipe::input,
                ItemStack.OPTIONAL_STREAM_CODEC, GasificationRecipe::itemOutput,
                FluidStack.OPTIONAL_STREAM_CODEC, GasificationRecipe::fluidOutput,
                ByteBufCodecs.VAR_INT, GasificationRecipe::durationTicks,
                ByteBufCodecs.DOUBLE, GasificationRecipe::minimumTemperatureKelvin,
                GasificationRecipe::new
        );

        @Override
        public MapCodec<GasificationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GasificationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
