package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMath;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

/**
 * Shared liquid-fuel contract consumed by later generator tasks.
 *
 * <p>Ported for the vanilla 1.21.1 recipe rewrite (no more {@code Recipe#getId()};
 * {@code Container} -> {@code RecipeInput}; {@code fromJson}/{@code toNetwork} ->
 * {@code codec()}/{@code streamCodec()}) - see {@code AdvancedProcessingRecipe} in
 * {@code content/multiblock/recipe} for the full writeup of this rewrite.</p>
 */
public final class FluidFuelRecipe implements Recipe<SingleRecipeInput> {
    private final Fluid fluid;
    private final int durationTicks;
    private final double powerPerTick;

    public FluidFuelRecipe(Fluid fluid, int durationTicks, double powerPerTick) {
        this.fluid = Objects.requireNonNull(fluid);
        this.durationTicks = Math.max(1, durationTicks);
        this.powerPerTick = Math.max(0.0D, powerPerTick);
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
        return ModRecipeTypes.FLUID_FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.FLUID_FUEL_TYPE.get();
    }

    public Fluid fluid() {
        return fluid;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public double powerPerTick() {
        return powerPerTick;
    }

    public double totalEnergyPerMilliBucket() {
        return SingleBlockMachineMath.fluidFuelEnergy(durationTicks, powerPerTick) / 1_000.0D;
    }

    public static final class Serializer implements RecipeSerializer<FluidFuelRecipe> {
        private static final MapCodec<FluidFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidFuelRecipe::fluid),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(FluidFuelRecipe::durationTicks),
                Codec.doubleRange(0.0D, Double.MAX_VALUE).fieldOf("power").forGetter(FluidFuelRecipe::powerPerTick)
        ).apply(instance, FluidFuelRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidFuelRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), FluidFuelRecipe::fluid,
                ByteBufCodecs.VAR_INT, FluidFuelRecipe::durationTicks,
                ByteBufCodecs.DOUBLE, FluidFuelRecipe::powerPerTick,
                FluidFuelRecipe::new
        );

        @Override
        public MapCodec<FluidFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
