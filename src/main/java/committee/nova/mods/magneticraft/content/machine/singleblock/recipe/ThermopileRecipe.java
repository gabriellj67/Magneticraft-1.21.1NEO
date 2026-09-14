package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data-driven block-state heat source used by the thermopile.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (no more {@code Recipe#getId()};
 * {@code Container} -> {@code RecipeInput}; {@code fromJson}/{@code toNetwork} ->
 * {@code codec()}/{@code streamCodec()}) - see {@code AdvancedProcessingRecipe} in
 * {@code content/multiblock/recipe} for the full writeup of this rewrite. This
 * recipe never matches through the standard {@code RecipeInput} path (same as the
 * Forge original) - it only ever matches a {@link BlockState} directly via
 * {@link #matches(BlockState)}.</p>
 */
public final class ThermopileRecipe implements Recipe<SingleRecipeInput> {
    private final Block block;
    private final Map<String, String> stateProperties;
    private final double temperatureKelvin;
    private final double conductivity;

    public ThermopileRecipe(
            Block block,
            Map<String, String> stateProperties,
            double temperatureKelvin,
            double conductivity
    ) {
        this.block = Objects.requireNonNull(block);
        this.stateProperties = Map.copyOf(stateProperties);
        this.temperatureKelvin = Math.max(0.0D, temperatureKelvin);
        this.conductivity = Math.max(0.000001D, conductivity);
    }

    public boolean matches(BlockState state) {
        if (!state.is(block)) {
            return false;
        }
        for (Map.Entry<String, String> expected : stateProperties.entrySet()) {
            Property<?> property = state.getProperties().stream()
                    .filter(candidate -> candidate.getName().equals(expected.getKey()))
                    .findFirst()
                    .orElse(null);
            if (property == null || !valueName(state, property).equals(expected.getValue())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(BlockState state, Property property) {
        return property.getName(state.getValue(property));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.THERMOPILE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.THERMOPILE_TYPE.get();
    }

    public Block block() {
        return block;
    }

    public Map<String, String> stateProperties() {
        return stateProperties;
    }

    public double temperatureKelvin() {
        return temperatureKelvin;
    }

    public double conductivity() {
        return conductivity;
    }

    public int specificity() {
        return stateProperties.size();
    }

    public static final class Serializer implements RecipeSerializer<ThermopileRecipe> {
        private static final Codec<Map<String, String>> STATE_CODEC =
                Codec.unboundedMap(Codec.STRING, Codec.STRING);
        private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> STATE_STREAM_CODEC =
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8);

        private static final MapCodec<ThermopileRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(ThermopileRecipe::block),
                STATE_CODEC.optionalFieldOf("state", Map.of())
                        .forGetter(recipe -> new LinkedHashMap<>(recipe.stateProperties())),
                Codec.doubleRange(0.0D, Double.MAX_VALUE)
                        .fieldOf("temperature")
                        .forGetter(ThermopileRecipe::temperatureKelvin),
                Codec.doubleRange(0.000001D, Double.MAX_VALUE)
                        .fieldOf("conductivity")
                        .forGetter(ThermopileRecipe::conductivity)
        ).apply(instance, ThermopileRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ThermopileRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK), ThermopileRecipe::block,
                STATE_STREAM_CODEC, recipe -> new HashMap<>(recipe.stateProperties()),
                ByteBufCodecs.DOUBLE, ThermopileRecipe::temperatureKelvin,
                ByteBufCodecs.DOUBLE, ThermopileRecipe::conductivity,
                ThermopileRecipe::new
        );

        @Override
        public MapCodec<ThermopileRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ThermopileRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
