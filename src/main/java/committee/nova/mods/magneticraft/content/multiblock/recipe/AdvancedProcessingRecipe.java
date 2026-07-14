package committee.nova.mods.magneticraft.content.multiblock.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.HydraulicPressMode;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Data-driven processing shared by the advanced item and oil-processing machines.
 */
public final class AdvancedProcessingRecipe implements Recipe<SimpleContainer> {
    public static final int MAX_RESULTS = 3;

    private final ResourceLocation id;
    private final MultiblockDefinition machine;
    private final Ingredient input;
    private final int inputCount;
    private final List<ChanceResult> results;
    @Nullable
    private final FluidInput fluidInput;
    private final int fluidInputAmount;
    private final List<FluidOutput> fluidOutputs;
    private final double minimumTemperatureKelvin;
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
        this.fluidInput = null;
        this.fluidInputAmount = 0;
        this.fluidOutputs = List.of();
        this.minimumTemperatureKelvin = 0.0D;
        this.pressMode = pressMode;
        this.duration = duration;
        this.energyPerTick = energyPerTick;
    }

    public AdvancedProcessingRecipe(
            ResourceLocation id,
            MultiblockDefinition machine,
            FluidInput fluidInput,
            int fluidInputAmount,
            List<FluidOutput> fluidOutputs,
            int duration,
            double minimumTemperatureKelvin
    ) {
        this.id = Objects.requireNonNull(id);
        this.machine = Objects.requireNonNull(machine);
        this.input = Ingredient.EMPTY;
        this.inputCount = 0;
        this.results = List.of();
        this.fluidInput = Objects.requireNonNull(fluidInput);
        if (machine != MultiblockDefinition.OIL_HEATER && machine != MultiblockDefinition.REFINERY) {
            throw new IllegalArgumentException("Unsupported fluid processing machine: " + machine.id());
        }
        if (fluidInputAmount <= 0 || duration <= 0) {
            throw new IllegalArgumentException("Fluid processing input amount and duration must be positive");
        }
        if (!Double.isFinite(minimumTemperatureKelvin) || minimumTemperatureKelvin < 0.0D) {
            throw new IllegalArgumentException("Minimum processing temperature must be finite and non-negative");
        }
        if (fluidOutputs.isEmpty() || fluidOutputs.size() > MAX_RESULTS) {
            throw new IllegalArgumentException("Fluid processing requires 1-" + MAX_RESULTS + " outputs");
        }
        List<FluidOutput> copiedOutputs = fluidOutputs.stream().map(FluidOutput::copy).toList();
        long distinctTanks = copiedOutputs.stream().map(FluidOutput::tank).distinct().count();
        if (distinctTanks != copiedOutputs.size()
                || (machine == MultiblockDefinition.OIL_HEATER
                && (copiedOutputs.size() != 1 || copiedOutputs.get(0).tank() != 0))) {
            throw new IllegalArgumentException("Fluid outputs must target unique valid machine tanks");
        }
        this.fluidInputAmount = fluidInputAmount;
        this.fluidOutputs = copiedOutputs;
        this.minimumTemperatureKelvin = minimumTemperatureKelvin;
        this.pressMode = null;
        this.duration = duration;
        this.energyPerTick = 0;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return !isFluidProcessing()
                && container.getItem(0).getCount() >= inputCount
                && input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return !isFluidProcessing() && width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
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

    public boolean isFluidProcessing() {
        return fluidInput != null;
    }

    public boolean matchesFluid(FluidStack stack) {
        return fluidInput != null && stack.getAmount() >= fluidInputAmount && fluidInput.matches(stack);
    }

    @Nullable
    public FluidInput fluidInput() {
        return fluidInput;
    }

    public int fluidInputAmount() {
        return fluidInputAmount;
    }

    public List<FluidOutput> fluidOutputs() {
        return fluidOutputs.stream().map(FluidOutput::copy).toList();
    }

    public double minimumTemperatureKelvin() {
        return minimumTemperatureKelvin;
    }

    public static final class Serializer implements RecipeSerializer<AdvancedProcessingRecipe> {
        @Override
        public AdvancedProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            MultiblockDefinition machine = parseMachine(GsonHelper.getAsString(json, "machine"));
            if (isFluidMachine(machine)) {
                JsonObject input = GsonHelper.getAsJsonObject(json, "fluid_input");
                JsonArray resultArray = GsonHelper.getAsJsonArray(json, "fluid_results");
                List<FluidOutput> outputs = new ArrayList<>(resultArray.size());
                for (int index = 0; index < resultArray.size(); index++) {
                    JsonObject output = GsonHelper.convertToJsonObject(resultArray.get(index), "fluid_result");
                    outputs.add(new FluidOutput(
                            GsonHelper.getAsInt(output, "tank", index),
                            fluidStackFromJson(output)
                    ));
                }
                return new AdvancedProcessingRecipe(
                        id,
                        machine,
                        FluidInput.fromJson(input),
                        GsonHelper.getAsInt(input, "amount"),
                        outputs,
                        GsonHelper.getAsInt(json, "duration"),
                        GsonHelper.getAsDouble(json, "minimum_temperature", 0.0D)
                );
            }
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
            if (buffer.readBoolean()) {
                FluidInput input = FluidInput.fromNetwork(buffer);
                int inputAmount = buffer.readVarInt();
                int outputCount = buffer.readVarInt();
                if (outputCount < 1 || outputCount > MAX_RESULTS) {
                    throw new IllegalArgumentException(id + " has invalid network fluid output count " + outputCount);
                }
                List<FluidOutput> outputs = new ArrayList<>(outputCount);
                for (int index = 0; index < outputCount; index++) {
                    outputs.add(new FluidOutput(buffer.readVarInt(), readFluidStack(buffer)));
                }
                return new AdvancedProcessingRecipe(
                        id,
                        machine,
                        input,
                        inputAmount,
                        outputs,
                        buffer.readVarInt(),
                        buffer.readDouble()
                );
            }
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
            buffer.writeBoolean(recipe.isFluidProcessing());
            if (recipe.fluidInput != null) {
                recipe.fluidInput.toNetwork(buffer);
                buffer.writeVarInt(recipe.fluidInputAmount);
                buffer.writeVarInt(recipe.fluidOutputs.size());
                for (FluidOutput output : recipe.fluidOutputs) {
                    buffer.writeVarInt(output.tank());
                    writeFluidStack(buffer, output.stack());
                }
                buffer.writeVarInt(recipe.duration);
                buffer.writeDouble(recipe.minimumTemperatureKelvin);
                return;
            }
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

        private static boolean isFluidMachine(MultiblockDefinition machine) {
            return machine == MultiblockDefinition.OIL_HEATER || machine == MultiblockDefinition.REFINERY;
        }

        private static FluidStack fluidStackFromJson(JsonObject json) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluidId), "Unknown fluid " + fluidId);
            return new FluidStack(fluid, GsonHelper.getAsInt(json, "amount"));
        }

        private static FluidStack readFluidStack(FriendlyByteBuf buffer) {
            ResourceLocation fluidId = buffer.readResourceLocation();
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluidId), "Unknown fluid " + fluidId);
            return new FluidStack(fluid, buffer.readVarInt());
        }

        private static void writeFluidStack(FriendlyByteBuf buffer, FluidStack stack) {
            buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(stack.getFluid())));
            buffer.writeVarInt(stack.getAmount());
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

    public record FluidInput(ResourceLocation key, boolean tag) {
        public FluidInput {
            Objects.requireNonNull(key);
        }

        public boolean matches(FluidStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (tag) {
                return stack.getFluid().defaultFluidState().is(TagKey.create(Registries.FLUID, key));
            }
            return key.equals(ForgeRegistries.FLUIDS.getKey(stack.getFluid()));
        }

        public List<FluidStack> examples(int amount) {
            int boundedAmount = Math.max(1, amount);
            return ForgeRegistries.FLUIDS.getValues().stream()
                    .filter(fluid -> matches(new FluidStack(fluid, boundedAmount)))
                    .map(fluid -> new FluidStack(fluid, boundedAmount))
                    .toList();
        }

        private static FluidInput fromJson(JsonObject json) {
            boolean hasFluid = json.has("fluid");
            boolean hasTag = json.has("tag");
            if (hasFluid == hasTag) {
                throw new IllegalArgumentException("Fluid input requires exactly one of fluid or tag");
            }
            return new FluidInput(ResourceLocation.parse(GsonHelper.getAsString(json, hasTag ? "tag" : "fluid")), hasTag);
        }

        private static FluidInput fromNetwork(FriendlyByteBuf buffer) {
            return new FluidInput(buffer.readResourceLocation(), buffer.readBoolean());
        }

        private void toNetwork(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(key);
            buffer.writeBoolean(tag);
        }
    }

    public record FluidOutput(int tank, FluidStack stack) {
        public FluidOutput {
            if (tank < 0 || tank >= MAX_RESULTS) {
                throw new IllegalArgumentException("Fluid output tank must be in [0, " + (MAX_RESULTS - 1) + "]");
            }
            stack = Objects.requireNonNull(stack).copy();
            if (stack.isEmpty() || stack.getAmount() <= 0) {
                throw new IllegalArgumentException("Fluid output must be non-empty");
            }
        }

        private FluidOutput copy() {
            return new FluidOutput(tank, stack);
        }
    }
}
