package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMath;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Shared liquid-fuel contract consumed by later generator tasks.
 */
public final class FluidFuelRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Fluid fluid;
    private final int durationTicks;
    private final double powerPerTick;

    public FluidFuelRecipe(ResourceLocation id, Fluid fluid, int durationTicks, double powerPerTick) {
        this.id = Objects.requireNonNull(id);
        this.fluid = Objects.requireNonNull(fluid);
        this.durationTicks = Math.max(1, durationTicks);
        this.powerPerTick = Math.max(0.0D, powerPerTick);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
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
        @Override
        public FluidFuelRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation fluidId = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluidId), "Unknown fluid " + fluidId);
            return new FluidFuelRecipe(
                    id,
                    fluid,
                    GsonHelper.getAsInt(json, "duration"),
                    GsonHelper.getAsDouble(json, "power")
            );
        }

        @Nullable
        @Override
        public FluidFuelRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ResourceLocation fluidId = buffer.readResourceLocation();
            Fluid fluid = Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluidId), "Unknown fluid " + fluidId);
            return new FluidFuelRecipe(id, fluid, buffer.readVarInt(), buffer.readDouble());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FluidFuelRecipe recipe) {
            buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(recipe.fluid)));
            buffer.writeVarInt(recipe.durationTicks);
            buffer.writeDouble(recipe.powerPerTick);
        }
    }
}
