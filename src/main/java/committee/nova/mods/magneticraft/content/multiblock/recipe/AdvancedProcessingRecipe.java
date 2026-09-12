package committee.nova.mods.magneticraft.content.multiblock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.HydraulicPressMode;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Data-driven processing shared by the advanced item and oil-processing machines.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite: {@link Recipe} no longer carries its own
 * id (that lives in the {@link net.minecraft.world.item.crafting.RecipeHolder} wrapper the
 * {@code RecipeManager} returns), {@code matches}/{@code assemble} now take a
 * {@link net.minecraft.world.item.crafting.RecipeInput} instead of a {@code Container}, and
 * {@link RecipeSerializer} is serialized via {@link #codec()}/{@link #streamCodec()}
 * (a {@code MapCodec}/{@code StreamCodec} pair) instead of the old {@code fromJson}/
 * {@code fromNetwork}/{@code toNetwork} triplet. Confirmed against the official Mojang 1.21.1
 * client mappings (not guessed): {@code Recipe}, {@code RecipeSerializer}, {@code RecipeManager},
 * {@code RecipeHolder}, {@code RecipeInput}, {@code SingleRecipeInput}. See the new cheat-sheet
 * entry in {@code PORTING_NOTES.md} for the full writeup - this is the first recipe-consuming
 * file ported, so every future recipe class in the mod hits this same rewrite.</p>
 */
public final class AdvancedProcessingRecipe implements Recipe<SingleRecipeInput> {
    public static final int MAX_RESULTS = 3;

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
            MultiblockDefinition machine,
            Ingredient input,
            List<ItemStack> results,
            int duration,
            int energyPerTick
    ) {
        this(
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
            MultiblockDefinition machine,
            Ingredient input,
            int inputCount,
            List<ChanceResult> results,
            @Nullable HydraulicPressMode pressMode,
            int duration,
            int energyPerTick
    ) {
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
            MultiblockDefinition machine,
            FluidInput fluidInput,
            int fluidInputAmount,
            List<FluidOutput> fluidOutputs,
            int duration,
            double minimumTemperatureKelvin
    ) {
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
    public boolean matches(SingleRecipeInput input, Level level) {
        return !isFluidProcessing()
                && input.item().getCount() >= inputCount
                && this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return !isFluidProcessing() && width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
    }

    @Override
    public RecipeSerializer<? extends AdvancedProcessingRecipe> getSerializer() {
        return ModRecipeTypes.advancedProcessingSerializer(machine).get();
    }

    @Override
    public RecipeType<? extends AdvancedProcessingRecipe> getType() {
        return ModRecipeTypes.advancedProcessingType(machine).get();
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

    /**
     * One serializer instance per machine (mirrors the Forge original): since a given
     * {@code expectedMachine} is always exclusively item-based or fluid-based (see the
     * constructor validation above), the item/fluid codec choice is made once here at
     * construction, not per-decode - there is no dynamic dispatch to get wrong.
     */
    public static final class Serializer implements RecipeSerializer<AdvancedProcessingRecipe> {
        private final MultiblockDefinition expectedMachine;
        private final MapCodec<AdvancedProcessingRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, AdvancedProcessingRecipe> streamCodec;

        public Serializer(MultiblockDefinition expectedMachine) {
            this.expectedMachine = Objects.requireNonNull(expectedMachine);
            if (isFluidMachine(expectedMachine)) {
                this.codec = fluidCodec(expectedMachine);
                this.streamCodec = fluidStreamCodec(expectedMachine);
            } else {
                this.codec = itemCodec(expectedMachine);
                this.streamCodec = itemStreamCodec(expectedMachine);
            }
        }

        @Override
        public MapCodec<AdvancedProcessingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AdvancedProcessingRecipe> streamCodec() {
            return streamCodec;
        }

        private static MapCodec<AdvancedProcessingRecipe> itemCodec(MultiblockDefinition expectedMachine) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(AdvancedProcessingRecipe::input),
                    Codec.intRange(1, Integer.MAX_VALUE)
                            .optionalFieldOf("input_count", 1)
                            .forGetter(AdvancedProcessingRecipe::inputCount),
                    ChanceResult.CODEC.listOf(1, MAX_RESULTS)
                            .fieldOf("results")
                            .forGetter(AdvancedProcessingRecipe::chanceResults),
                    HydraulicPressMode.CODEC.optionalFieldOf("press_mode")
                            .forGetter(recipe -> Optional.ofNullable(recipe.pressMode())),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration")
                            .forGetter(AdvancedProcessingRecipe::duration),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("energy_per_tick")
                            .forGetter(AdvancedProcessingRecipe::energyPerTick)
            ).apply(instance, (ingredient, inputCount, results, pressMode, duration, energyPerTick) ->
                    new AdvancedProcessingRecipe(
                            expectedMachine, ingredient, inputCount, results, pressMode.orElse(null),
                            duration, energyPerTick
                    )));
        }

        private static StreamCodec<RegistryFriendlyByteBuf, AdvancedProcessingRecipe> itemStreamCodec(
                MultiblockDefinition expectedMachine
        ) {
            return StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, AdvancedProcessingRecipe::input,
                    ByteBufCodecs.VAR_INT, AdvancedProcessingRecipe::inputCount,
                    ChanceResult.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_RESULTS)), AdvancedProcessingRecipe::chanceResults,
                    ByteBufCodecs.optional(HydraulicPressMode.STREAM_CODEC),
                    recipe -> Optional.ofNullable(recipe.pressMode()),
                    ByteBufCodecs.VAR_INT, AdvancedProcessingRecipe::duration,
                    ByteBufCodecs.VAR_INT, AdvancedProcessingRecipe::energyPerTick,
                    (ingredient, inputCount, results, pressMode, duration, energyPerTick) ->
                            new AdvancedProcessingRecipe(
                                    expectedMachine, ingredient, inputCount, results, pressMode.orElse(null),
                                    duration, energyPerTick
                            )
            );
        }

        private static MapCodec<AdvancedProcessingRecipe> fluidCodec(MultiblockDefinition expectedMachine) {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    FluidInput.CODEC.fieldOf("fluid_input").forGetter(AdvancedProcessingRecipe::fluidInput),
                    Codec.intRange(1, Integer.MAX_VALUE)
                            .fieldOf("fluid_input_amount")
                            .forGetter(AdvancedProcessingRecipe::fluidInputAmount),
                    FluidOutput.CODEC.listOf(1, MAX_RESULTS)
                            .fieldOf("fluid_outputs")
                            .forGetter(AdvancedProcessingRecipe::fluidOutputs),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration")
                            .forGetter(AdvancedProcessingRecipe::duration),
                    Codec.doubleRange(0.0D, Double.MAX_VALUE)
                            .optionalFieldOf("minimum_temperature", 0.0D)
                            .forGetter(AdvancedProcessingRecipe::minimumTemperatureKelvin)
            ).apply(instance, (fluidInput, fluidInputAmount, fluidOutputs, duration, minimumTemperature) ->
                    new AdvancedProcessingRecipe(
                            expectedMachine, fluidInput, fluidInputAmount, fluidOutputs, duration, minimumTemperature
                    )));
        }

        private static StreamCodec<RegistryFriendlyByteBuf, AdvancedProcessingRecipe> fluidStreamCodec(
                MultiblockDefinition expectedMachine
        ) {
            return StreamCodec.composite(
                    FluidInput.STREAM_CODEC, recipe -> Objects.requireNonNull(recipe.fluidInput()),
                    ByteBufCodecs.VAR_INT, AdvancedProcessingRecipe::fluidInputAmount,
                    FluidOutput.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_RESULTS)), AdvancedProcessingRecipe::fluidOutputs,
                    ByteBufCodecs.VAR_INT, AdvancedProcessingRecipe::duration,
                    ByteBufCodecs.DOUBLE, AdvancedProcessingRecipe::minimumTemperatureKelvin,
                    (fluidInput, fluidInputAmount, fluidOutputs, duration, minimumTemperature) ->
                            new AdvancedProcessingRecipe(
                                    expectedMachine, fluidInput, fluidInputAmount, fluidOutputs,
                                    duration, minimumTemperature
                            )
            );
        }

        private static boolean isFluidMachine(MultiblockDefinition machine) {
            return machine == MultiblockDefinition.OIL_HEATER || machine == MultiblockDefinition.REFINERY;
        }
    }

    public record ChanceResult(ItemStack stack, float chance) {
        static final Codec<ChanceResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(ChanceResult::stack),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(ChanceResult::chance)
        ).apply(instance, ChanceResult::new));

        static final StreamCodec<RegistryFriendlyByteBuf, ChanceResult> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ChanceResult::stack,
                ByteBufCodecs.FLOAT, ChanceResult::chance,
                ChanceResult::new
        );

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
        static final Codec<FluidInput> CODEC = Codec.either(
                ResourceLocation.CODEC.fieldOf("fluid").codec(),
                ResourceLocation.CODEC.fieldOf("tag").codec()
        ).xmap(
                either -> either.map(id -> new FluidInput(id, false), id -> new FluidInput(id, true)),
                input -> input.tag ? com.mojang.datafixers.util.Either.right(input.key)
                        : com.mojang.datafixers.util.Either.left(input.key)
        );

        static final StreamCodec<RegistryFriendlyByteBuf, FluidInput> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, FluidInput::key,
                ByteBufCodecs.BOOL, FluidInput::tag,
                FluidInput::new
        );

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
            return key.equals(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        }

        public List<FluidStack> examples(int amount) {
            int boundedAmount = Math.max(1, amount);
            return net.minecraft.core.registries.BuiltInRegistries.FLUID.stream()
                    .filter(fluid -> matches(new FluidStack(fluid, boundedAmount)))
                    .map(fluid -> new FluidStack(fluid, boundedAmount))
                    .toList();
        }
    }

    public record FluidOutput(int tank, FluidStack stack) {
        static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, MAX_RESULTS - 1).fieldOf("tank").forGetter(FluidOutput::tank),
                FluidStack.CODEC.fieldOf("fluid").forGetter(FluidOutput::stack)
        ).apply(instance, FluidOutput::new));

        static final StreamCodec<RegistryFriendlyByteBuf, FluidOutput> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FluidOutput::tank,
                FluidStack.STREAM_CODEC, FluidOutput::stack,
                FluidOutput::new
        );

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
