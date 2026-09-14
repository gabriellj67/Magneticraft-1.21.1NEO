package committee.nova.mods.magneticraft.content.nuclear.facility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Counted, transactional recipe shared by all compressed nuclear front-end facilities.
 *
 * <p>Rewritten for the vanilla 1.21.1 recipe rework (same shape as
 * {@code content/multiblock/recipe/AdvancedProcessingRecipe} - see its Phase 4 writeup in
 * PORTING_NOTES.md): {@code Recipe} no longer carries its own id, {@code matches}/{@code assemble}
 * take a {@link RecipeInput} instead of a {@code Container}, and the serializer is a
 * {@code MapCodec}/{@code StreamCodec} pair instead of {@code fromJson}/{@code fromNetwork}/
 * {@code toNetwork}. This recipe needs up to {@link #MAX_INGREDIENTS} input slots at once (not a
 * single slot), so it defines its own minimal {@link Input} wrapper around a
 * {@link SimpleContainer} rather than reusing vanilla's single-slot {@code SingleRecipeInput}.</p>
 */
public final class NuclearProcessRecipe implements Recipe<NuclearProcessRecipe.Input> {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_RESULTS = 4;

    private final NuclearFacilityType facility;
    private final List<CountedIngredient> ingredients;
    private final List<ItemStack> results;
    private final int durationTicks;
    private final int joulesPerTick;

    public NuclearProcessRecipe(NuclearFacilityType facility,
                                List<CountedIngredient> ingredients, List<ItemStack> results,
                                int durationTicks, int joulesPerTick) {
        this.facility = Objects.requireNonNull(facility);
        if (ingredients.isEmpty() || ingredients.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("Nuclear recipe ingredient count must be 1.." + MAX_INGREDIENTS);
        }
        if (results.isEmpty() || results.size() > MAX_RESULTS) {
            throw new IllegalArgumentException("Nuclear recipe result count must be 1.." + MAX_RESULTS);
        }
        this.ingredients = List.copyOf(ingredients);
        this.results = results.stream().map(ItemStack::copy).toList();
        this.durationTicks = positive(durationTicks, "duration_ticks");
        this.joulesPerTick = positive(joulesPerTick, "joules_per_tick");
    }

    public NuclearFacilityType facility() {
        return facility;
    }

    public List<CountedIngredient> ingredients() {
        return ingredients;
    }

    public List<ItemStack> results() {
        return results.stream().map(ItemStack::copy).toList();
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int joulesPerTick() {
        return joulesPerTick;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return consumptionPlan(input.container()).isPresent();
    }

    public Optional<ConsumptionPlan> consumptionPlan(SimpleContainer container) {
        int[] available = new int[container.getContainerSize()];
        for (int slot = 0; slot < available.length; slot++) {
            available[slot] = container.getItem(slot).getCount();
        }
        List<SlotConsumption> consumed = new ArrayList<>();
        for (CountedIngredient counted : ingredients) {
            int remaining = counted.count();
            for (int slot = 0; slot < available.length && remaining > 0; slot++) {
                ItemStack stack = container.getItem(slot);
                if (available[slot] <= 0 || !counted.ingredient().test(stack)) {
                    continue;
                }
                int amount = Math.min(available[slot], remaining);
                available[slot] -= amount;
                remaining -= amount;
                consumed.add(new SlotConsumption(slot, amount));
            }
            if (remaining > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(new ConsumptionPlan(consumed));
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.get(0).copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.NUCLEAR_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.NUCLEAR_PROCESSING_TYPE.get();
    }

    private static int positive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /** Minimal multi-slot {@link RecipeInput} - vanilla only ships the single-slot variant. */
    public record Input(SimpleContainer container) implements RecipeInput {
        @Override
        public ItemStack getItem(int slot) {
            return container.getItem(slot);
        }

        @Override
        public int size() {
            return container.getContainerSize();
        }
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1)
                        .forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient,
                ByteBufCodecs.VAR_INT, CountedIngredient::count,
                CountedIngredient::new
        );

        public CountedIngredient {
            Objects.requireNonNull(ingredient);
            positive(count, "ingredient count");
        }
    }

    public record SlotConsumption(int slot, int count) {
    }

    public record ConsumptionPlan(List<SlotConsumption> slots) {
        public ConsumptionPlan {
            slots = List.copyOf(slots);
        }

        public void apply(IItemHandlerModifiable inventory) {
            for (SlotConsumption consumption : slots) {
                inventory.extractItem(consumption.slot(), consumption.count(), false);
            }
        }
    }

    public static final class Serializer implements RecipeSerializer<NuclearProcessRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, NuclearFacilityType> FACILITY_STREAM_CODEC =
                StreamCodec.of(
                        (buffer, value) -> buffer.writeEnum(value),
                        buffer -> buffer.readEnum(NuclearFacilityType.class)
                );

        private static final MapCodec<NuclearProcessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.xmap(NuclearFacilityType::byId, NuclearFacilityType::id)
                        .fieldOf("facility").forGetter(NuclearProcessRecipe::facility),
                CountedIngredient.CODEC.listOf(1, MAX_INGREDIENTS)
                        .fieldOf("ingredients").forGetter(NuclearProcessRecipe::ingredients),
                ItemStack.CODEC.listOf(1, MAX_RESULTS)
                        .fieldOf("results").forGetter(NuclearProcessRecipe::results),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration_ticks")
                        .forGetter(NuclearProcessRecipe::durationTicks),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("joules_per_tick")
                        .forGetter(NuclearProcessRecipe::joulesPerTick)
        ).apply(instance, NuclearProcessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, NuclearProcessRecipe> STREAM_CODEC = StreamCodec.composite(
                FACILITY_STREAM_CODEC, NuclearProcessRecipe::facility,
                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)), NuclearProcessRecipe::ingredients,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_RESULTS)), NuclearProcessRecipe::results,
                ByteBufCodecs.VAR_INT, NuclearProcessRecipe::durationTicks,
                ByteBufCodecs.VAR_INT, NuclearProcessRecipe::joulesPerTick,
                NuclearProcessRecipe::new
        );

        @Override
        public MapCodec<NuclearProcessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NuclearProcessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
