package committee.nova.mods.magneticraft.content.machine.crushingtable;

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

/**
 * Data-driven crushing-table conversion.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (see {@code content/multiblock/recipe/AdvancedProcessingRecipe}
 * for the worked example): no more {@code getId()}, {@code SimpleContainer} input replaced by
 * {@link SingleRecipeInput}, and the serializer is a {@code MapCodec}/{@code StreamCodec} pair.</p>
 */
public final class CrushingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack output;
    private final int requiredLevel;

    public CrushingRecipe(Ingredient input, ItemStack output, int requiredLevel) {
        this.input = input;
        this.output = output.copy();
        this.requiredLevel = Math.max(-1, requiredLevel);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
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
        private static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::output),
                Codec.INT.optionalFieldOf("required_level", -1).forGetter(CrushingRecipe::requiredLevel)
        ).apply(instance, CrushingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::input,
                ItemStack.STREAM_CODEC, CrushingRecipe::output,
                ByteBufCodecs.VAR_INT, recipe -> recipe.requiredLevel + 1,
                (input, output, requiredLevel) -> new CrushingRecipe(input, output, requiredLevel - 1)
        );

        @Override
        public MapCodec<CrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
