package committee.nova.mods.magneticraft.content.multiblock.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.HydraulicPressMode;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Data-driven item processing shared by grinder, sieve and hydraulic press.
 */
public final class AdvancedProcessingRecipe implements Recipe<SimpleContainer> {
    public static final int MAX_RESULTS = 3;

    private final ResourceLocation id;
    private final MultiblockDefinition machine;
    private final Ingredient input;
    private final int inputCount;
    private final List<ChanceResult> results;
    @Nullable
    private final HydraulicPressMode pressMode;
    private final int duration;
    private final int energyPerTick;

    public AdvancedProcessingRecipe(
            ResourceLocation id,
            MultiblockDefinition machine,
            Ingredient input,
            List<ItemStack> results,
            int duration,
            int energyPerTick
    ) {
        this(
                id,
                machine,
                input,
                1,
                results.stream().map(stack -> new ChanceResult(stack, 1.0F)).toList(),
                machine == MultiblockDefinition.HYDRAULIC_PRESS ? HydraulicPressMode.LIGHT : null,
                duration,
                energyPerTick
        );
    }

    public AdvancedProcessingRecipe(
            ResourceLocation id,
            MultiblockDefinition machine,
            Ingredient input,
            int inputCount,
            List<ChanceResult> results,
            @Nullable HydraulicPressMode pressMode,
            int duration,
            int energyPerTick
    ) {
        this.id = Objects.requireNonNull(id);
        this.machine = Objects.requireNonNull(machine);
        this.input = Objects.requireNonNull(input);
        if (machine != MultiblockDefinition.GRINDER
                && machine != MultiblockDefinition.SIEVE
                && machine != MultiblockDefinition.HYDRAULIC_PRESS) {
            throw new IllegalArgumentException("Unsupported advanced processing machine: " + machine.id());
        }
        if (inputCount <= 0) {
            throw new IllegalArgumentException("Advanced processing input count must be positive");
        }
        if (results.isEmpty() || results.size() > MAX_RESULTS) {
            throw new IllegalArgumentException("Advanced processing requires 1-" + MAX_RESULTS + " non-empty results");
        }
        if ((machine == MultiblockDefinition.HYDRAULIC_PRESS) != (pressMode != null)) {
            throw new IllegalArgumentException("Hydraulic press mode must be present only for hydraulic recipes");
        }
        if (duration <= 0 || energyPerTick < 0) {
            throw new IllegalArgumentException("Invalid processing duration or energy cost");
        }
        this.inputCount = inputCount;
        this.results = results.stream().map(ChanceResult::copy).toList();
        this.pressMode = pressMode;
        this.duration = duration;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return container.getItem(0).getCount() >= inputCount && input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return results.get(0).stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return results.get(0).stack().copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ADVANCED_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ADVANCED_PROCESSING_TYPE.get();
    }

    public MultiblockDefinition machine() {
        return machine;
    }

    public Ingredient input() {
        return input;
    }

    public int inputCount() {
        return inputCount;
    }

    public List<ItemStack> results() {
        return results.stream().map(result -> result.stack().copy()).toList();
    }

    public List<ChanceResult> chanceResults() {
        return results.stream().map(ChanceResult::copy).toList();
    }

    @Nullable
    public HydraulicPressMode pressMode() {
        return pressMode;
    }

    public int duration() {
        return duration;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

    public static final class Serializer implements RecipeSerializer<AdvancedProcessingRecipe> {
        @Override
        public AdvancedProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            MultiblockDefinition machine = parseMachine(GsonHelper.getAsString(json, "machine"));
            Ingredient input = Ingredient.fromJson(json.get("ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "input_count", 1);
            JsonArray resultArray = GsonHelper.getAsJsonArray(json, "results");
            if (resultArray.size() < 1 || resultArray.size() > MAX_RESULTS) {
                throw new IllegalArgumentException(id + " has invalid result count " + resultArray.size());
            }
            List<ChanceResult> results = new ArrayList<>(resultArray.size());
            for (JsonElement element : resultArray) {
                JsonObject result = GsonHelper.convertToJsonObject(element, "result");
                results.add(new ChanceResult(
                        ShapedRecipe.itemStackFromJson(result),
                        GsonHelper.getAsFloat(result, "chance", 1.0F)
                ));
            }
            return new AdvancedProcessingRecipe(
                    id,
                    machine,
                    input,
                    inputCount,
                    results,
                    machine == MultiblockDefinition.HYDRAULIC_PRESS
                            ? HydraulicPressMode.parse(GsonHelper.getAsString(json, "press_mode"))
                            : null,
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsInt(json, "energy_per_tick")
            );
        }

        @Nullable
        @Override
        public AdvancedProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            MultiblockDefinition machine = parseMachine(buffer.readUtf(64));
            Ingredient input = Ingredient.fromNetwork(buffer);
            int inputCount = buffer.readVarInt();
            int resultCount = buffer.readVarInt();
            if (resultCount < 1 || resultCount > MAX_RESULTS) {
                throw new IllegalArgumentException(id + " has invalid network result count " + resultCount);
            }
            List<ChanceResult> results = new ArrayList<>(resultCount);
            for (int index = 0; index < resultCount; index++) {
                results.add(new ChanceResult(buffer.readItem(), buffer.readFloat()));
            }
            return new AdvancedProcessingRecipe(
                    id,
                    machine,
                    input,
                    inputCount,
                    results,
                    buffer.readBoolean() ? HydraulicPressMode.parse(buffer.readUtf(16)) : null,
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AdvancedProcessingRecipe recipe) {
            buffer.writeUtf(recipe.machine.id());
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.results.size());
            recipe.results.forEach(result -> {
                buffer.writeItem(result.stack());
                buffer.writeFloat(result.chance());
            });
            buffer.writeBoolean(recipe.pressMode != null);
            if (recipe.pressMode != null) {
                buffer.writeUtf(recipe.pressMode.serializedName());
            }
            buffer.writeVarInt(recipe.duration);
            buffer.writeVarInt(recipe.energyPerTick);
        }

        private static MultiblockDefinition parseMachine(String id) {
            return java.util.Arrays.stream(MultiblockDefinition.values())
                    .filter(definition -> definition.id().equals(id.toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown advanced processing machine: " + id));
        }
    }

    public record ChanceResult(ItemStack stack, float chance) {
        public ChanceResult {
            stack = Objects.requireNonNull(stack).copy();
            if (stack.isEmpty() || !(chance > 0.0F && chance <= 1.0F)) {
                throw new IllegalArgumentException("Advanced result requires a non-empty stack and chance in (0,1]");
            }
        }

        private ChanceResult copy() {
            return new ChanceResult(stack, chance);
        }
    }
}
