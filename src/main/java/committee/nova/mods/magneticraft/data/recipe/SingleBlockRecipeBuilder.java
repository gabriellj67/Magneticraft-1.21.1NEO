package committee.nova.mods.magneticraft.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Datagen-only serializers for Task 5 machine recipes.
 */
public final class SingleBlockRecipeBuilder {
    private SingleBlockRecipeBuilder() {
    }

    public static void sluice(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            Ingredient input,
            List<SluiceRecipe.ChanceOutput> outputs
    ) {
        consumer.accept(new SluiceResult(id, input, List.copyOf(outputs)));
    }

    public static void gasification(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            Ingredient input,
            ItemStack itemOutput,
            FluidStack fluidOutput,
            int duration,
            double minimumTemperature
    ) {
        consumer.accept(new GasificationResult(
                id,
                input,
                itemOutput.copy(),
                fluidOutput.copy(),
                duration,
                minimumTemperature
        ));
    }

    public static void thermopile(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            Block block,
            Map<String, String> state,
            double temperature,
            double conductivity
    ) {
        consumer.accept(new ThermopileResult(id, block, Map.copyOf(state), temperature, conductivity));
    }

    public static void fluidFuel(
            Consumer<FinishedRecipe> consumer,
            ResourceLocation id,
            Fluid fluid,
            int duration,
            double power
    ) {
        consumer.accept(new FluidFuelResult(id, fluid, duration, power));
    }

    private static JsonObject stackJson(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem())).toString());
        if (stack.getCount() != 1) {
            json.addProperty("count", stack.getCount());
        }
        return json;
    }

    private interface NoAdvancementRecipe extends FinishedRecipe {
        @Nullable
        @Override
        default JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        default ResourceLocation getAdvancementId() {
            return null;
        }
    }

    private record SluiceResult(
            ResourceLocation id,
            Ingredient input,
            List<SluiceRecipe.ChanceOutput> outputs
    ) implements NoAdvancementRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("ingredient", input.toJson());
            JsonArray results = new JsonArray();
            for (SluiceRecipe.ChanceOutput output : outputs) {
                JsonObject entry = new JsonObject();
                entry.add("stack", stackJson(output.stack()));
                if (output.chance() != 1.0F) {
                    entry.addProperty("chance", output.chance());
                }
                results.add(entry);
            }
            json.add("results", results);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.SLUICE_SERIALIZER.get();
        }
    }

    private record GasificationResult(
            ResourceLocation id,
            Ingredient input,
            ItemStack itemOutput,
            FluidStack fluidOutput,
            int duration,
            double minimumTemperature
    ) implements NoAdvancementRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("ingredient", input.toJson());
            if (!itemOutput.isEmpty()) {
                json.add("item_result", stackJson(itemOutput));
            }
            JsonObject fluid = new JsonObject();
            fluid.addProperty(
                    "fluid",
                    Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(fluidOutput.getFluid())).toString()
            );
            fluid.addProperty("amount", fluidOutput.getAmount());
            json.add("fluid_result", fluid);
            json.addProperty("duration", duration);
            json.addProperty("minimum_temperature", minimumTemperature);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.GASIFICATION_SERIALIZER.get();
        }
    }

    private record ThermopileResult(
            ResourceLocation id,
            Block block,
            Map<String, String> state,
            double temperature,
            double conductivity
    ) implements NoAdvancementRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("block", Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).toString());
            if (!state.isEmpty()) {
                JsonObject stateJson = new JsonObject();
                state.forEach(stateJson::addProperty);
                json.add("state", stateJson);
            }
            json.addProperty("temperature", temperature);
            json.addProperty("conductivity", conductivity);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.THERMOPILE_SERIALIZER.get();
        }
    }

    private record FluidFuelResult(
            ResourceLocation id,
            Fluid fluid,
            int duration,
            double power
    ) implements NoAdvancementRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            json.addProperty("fluid", Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(fluid)).toString());
            json.addProperty("duration", duration);
            json.addProperty("power", power);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeTypes.FLUID_FUEL_SERIALIZER.get();
        }
    }
}
