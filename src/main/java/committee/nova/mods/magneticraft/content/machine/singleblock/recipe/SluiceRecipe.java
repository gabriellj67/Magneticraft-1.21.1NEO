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

import java.util.List;
import java.util.Objects;

/**
 * One sluice input and independently rolled outputs.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (no more {@code Recipe#getId()};
 * {@code Container} -> {@code RecipeInput}; {@code fromJson}/{@code toNetwork} ->
 * {@code codec()}/{@code streamCodec()}) - see {@code AdvancedProcessingRecipe} in
 * {@code content/multiblock/recipe} for the full writeup of this rewrite, including
 * the {@code ChanceResult}-style nested record codec this class's {@link ChanceOutput}
 * follows.</p>
 */
public final class SluiceRecipe implements Recipe<SingleRecipeInput> {
    public static final int MAX_OUTPUTS = 10;

    private final Ingredient input;
    private final List<ChanceOutput> outputs;

    public SluiceRecipe(Ingredient input, List<ChanceOutput> outputs) {
        this.input = Objects.requireNonNull(input);
        validateOutputCount(outputs.size());
        this.outputs = outputs.stream().map(ChanceOutput::copy).toList();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.stream()
                .filter(output -> output.chance() >= 1.0F && !output.stack().isEmpty())
                .map(ChanceOutput::stack)
                .findFirst()
                .orElse(ItemStack.EMPTY);
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
        static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(ChanceOutput::stack),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(ChanceOutput::chance)
        ).apply(instance, ChanceOutput::new));

        static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ChanceOutput::stack,
                ByteBufCodecs.FLOAT, ChanceOutput::chance,
                ChanceOutput::new
        );

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
        private static final MapCodec<SluiceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(SluiceRecipe::input),
                ChanceOutput.CODEC.listOf(1, MAX_OUTPUTS).fieldOf("results").forGetter(SluiceRecipe::outputs)
        ).apply(instance, SluiceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SluiceRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SluiceRecipe::input,
                ChanceOutput.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_OUTPUTS)), SluiceRecipe::outputs,
                SluiceRecipe::new
        );

        @Override
        public MapCodec<SluiceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SluiceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
