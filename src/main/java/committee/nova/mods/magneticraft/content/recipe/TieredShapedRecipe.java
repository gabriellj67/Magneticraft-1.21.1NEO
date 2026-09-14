package committee.nova.mods.magneticraft.content.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Vanilla shaped matching with a versioned electrical payload added to the result.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (see PORTING_NOTES.md's cheat sheet and
 * {@code content/multiblock/recipe/AdvancedProcessingRecipe} for the worked example this
 * follows): {@link net.minecraft.world.item.crafting.Recipe} no longer carries {@code getId()},
 * {@code matches}/{@code assemble} take {@link CraftingInput} instead of a
 * {@code CraftingContainer}, and {@link RecipeSerializer} is now a {@code MapCodec}/
 * {@code StreamCodec} pair instead of {@code fromJson}/{@code fromNetwork}/{@code toNetwork}.</p>
 */
public final class TieredShapedRecipe implements CraftingRecipe {
    private static final String ELECTRICAL_TAG = "electrical";

    private final ShapedRecipe delegate;
    private final TieredElectricalItemData itemData;

    public TieredShapedRecipe(ShapedRecipe delegate, TieredElectricalItemData itemData) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.itemData = Objects.requireNonNull(itemData, "itemData");
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = delegate.assemble(input, registries);
        itemData.write(result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return delegate.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack result = delegate.getResultItem(registries).copy();
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
    public CraftingBookCategory category() {
        return delegate.category();
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
        private static final MapCodec<TieredShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ShapedRecipe.Serializer.CODEC.forGetter(TieredShapedRecipe::delegate),
                TieredElectricalItemData.CODEC.fieldOf(ELECTRICAL_TAG).forGetter(TieredShapedRecipe::itemData)
        ).apply(instance, TieredShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TieredShapedRecipe> STREAM_CODEC = StreamCodec.composite(
                ShapedRecipe.Serializer.STREAM_CODEC, TieredShapedRecipe::delegate,
                TieredElectricalItemData.STREAM_CODEC, TieredShapedRecipe::itemData,
                TieredShapedRecipe::new
        );

        @Override
        public MapCodec<TieredShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TieredShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
