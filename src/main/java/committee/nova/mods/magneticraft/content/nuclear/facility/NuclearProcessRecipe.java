package committee.nova.mods.magneticraft.content.nuclear.facility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Counted, transactional recipe shared by all compressed nuclear front-end facilities. */
public final class NuclearProcessRecipe implements Recipe<SimpleContainer> {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_RESULTS = 4;

    private final ResourceLocation id;
    private final NuclearFacilityType facility;
    private final List<CountedIngredient> ingredients;
    private final List<ItemStack> results;
    private final int durationTicks;
    private final int joulesPerTick;

    public NuclearProcessRecipe(ResourceLocation id, NuclearFacilityType facility,
                                List<CountedIngredient> ingredients, List<ItemStack> results,
                                int durationTicks, int joulesPerTick) {
        this.id = Objects.requireNonNull(id);
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
    public boolean matches(SimpleContainer container, Level level) {
        return consumptionPlan(container).isPresent();
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
    public ItemStack assemble(SimpleContainer container, RegistryAccess registries) {
        return results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return results.get(0).copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
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

    public record CountedIngredient(Ingredient ingredient, int count) {
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
        @Override
        public NuclearProcessRecipe fromJson(ResourceLocation id, JsonObject json) {
            NuclearFacilityType facility = NuclearFacilityType.byId(GsonHelper.getAsString(json, "facility"));
            JsonArray ingredientJson = GsonHelper.getAsJsonArray(json, "ingredients");
            if (ingredientJson.size() < 1 || ingredientJson.size() > MAX_INGREDIENTS) {
                throw new IllegalArgumentException("ingredients must contain 1.." + MAX_INGREDIENTS + " entries");
            }
            List<CountedIngredient> ingredients = new ArrayList<>();
            ingredientJson.forEach(element -> {
                JsonObject entry = GsonHelper.convertToJsonObject(element, "ingredient entry");
                ingredients.add(new CountedIngredient(
                        Ingredient.fromJson(GsonHelper.getNonNull(entry, "ingredient")),
                        GsonHelper.getAsInt(entry, "count", 1)
                ));
            });

            JsonArray resultJson = GsonHelper.getAsJsonArray(json, "results");
            if (resultJson.size() < 1 || resultJson.size() > MAX_RESULTS) {
                throw new IllegalArgumentException("results must contain 1.." + MAX_RESULTS + " entries");
            }
            List<ItemStack> results = new ArrayList<>();
            resultJson.forEach(element -> results.add(readStack(
                    GsonHelper.convertToJsonObject(element, "result entry"))));
            return new NuclearProcessRecipe(
                    id, facility, ingredients, results,
                    GsonHelper.getAsInt(json, "duration_ticks"),
                    GsonHelper.getAsInt(json, "joules_per_tick")
            );
        }

        @Nullable
        @Override
        public NuclearProcessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            NuclearFacilityType facility = buffer.readEnum(NuclearFacilityType.class);
            int ingredientCount = buffer.readVarInt();
            if (ingredientCount < 1 || ingredientCount > MAX_INGREDIENTS) {
                throw new IllegalArgumentException("Invalid network ingredient count: " + ingredientCount);
            }
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            int resultCount = buffer.readVarInt();
            if (resultCount < 1 || resultCount > MAX_RESULTS) {
                throw new IllegalArgumentException("Invalid network result count: " + resultCount);
            }
            List<ItemStack> results = new ArrayList<>();
            for (int i = 0; i < resultCount; i++) {
                results.add(buffer.readItem());
            }
            return new NuclearProcessRecipe(id, facility, ingredients, results,
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, NuclearProcessRecipe recipe) {
            buffer.writeEnum(recipe.facility);
            buffer.writeVarInt(recipe.ingredients.size());
            recipe.ingredients.forEach(ingredient -> {
                ingredient.ingredient().toNetwork(buffer);
                buffer.writeVarInt(ingredient.count());
            });
            buffer.writeVarInt(recipe.results.size());
            recipe.results.forEach(buffer::writeItem);
            buffer.writeVarInt(recipe.durationTicks);
            buffer.writeVarInt(recipe.joulesPerTick);
        }

        private static ItemStack readStack(JsonObject json) {
            ResourceLocation itemId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "item"));
            Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Unknown recipe result item: " + itemId);
            }
            int count = GsonHelper.getAsInt(json, "count", 1);
            if (count <= 0 || count > item.getMaxStackSize()) {
                throw new IllegalArgumentException("Invalid recipe result count: " + count);
            }
            return new ItemStack(item, count);
        }
    }
}
