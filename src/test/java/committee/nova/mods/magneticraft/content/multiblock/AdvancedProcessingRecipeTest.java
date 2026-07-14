package committee.nova.mods.magneticraft.content.multiblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedProcessingRecipeTest {
    @BeforeAll
    @SuppressWarnings("deprecation")
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
        BuiltInRegistries.FLUID.bindTags(Map.of(
                FluidTags.WATER,
                List.of(
                        BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER),
                        BuiltInRegistries.FLUID.wrapAsHolder(Fluids.FLOWING_WATER)
                )
        ));
    }

    @Test
    void inputCountPressModeAndChanceOutputsArePartOfTheRecipeContract() {
        AdvancedProcessingRecipe recipe = pressRecipe();
        SimpleContainer input = new SimpleContainer(new ItemStack(Items.IRON_INGOT, 4));

        assertTrue(recipe.matches(input, null));
        input.getItem(0).setCount(3);
        assertFalse(recipe.matches(input, null));
        assertEquals(4, recipe.inputCount());
        assertEquals(HydraulicPressMode.HEAVY, recipe.pressMode());
        assertEquals(0.25F, recipe.chanceResults().get(1).chance());

        ItemStack detached = recipe.chanceResults().get(0).stack();
        detached.setCount(64);
        assertEquals(1, recipe.chanceResults().get(0).stack().getCount());
    }

    @Test
    void networkRoundTripPreservesEveryProcessingField() {
        AdvancedProcessingRecipe original = pressRecipe();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AdvancedProcessingRecipe.Serializer serializer = new AdvancedProcessingRecipe.Serializer();
            serializer.toNetwork(buffer, original);
            AdvancedProcessingRecipe decoded = serializer.fromNetwork(original.getId(), buffer);

            assertEquals(MultiblockDefinition.HYDRAULIC_PRESS, decoded.machine());
            assertEquals(4, decoded.inputCount());
            assertEquals(HydraulicPressMode.HEAVY, decoded.pressMode());
            assertEquals(120, decoded.duration());
            assertEquals(60, decoded.energyPerTick());
            assertEquals(2, decoded.chanceResults().size());
            assertEquals(Items.IRON_BLOCK, decoded.chanceResults().get(0).stack().getItem());
            assertEquals(0.25F, decoded.chanceResults().get(1).chance());
        } finally {
            buffer.release();
        }
    }

    @Test
    void invalidCountsModesAndProbabilitiesAreRejected() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("magneticraft", "invalid");
        Ingredient input = Ingredient.of(Items.IRON_INGOT);
        List<AdvancedProcessingRecipe.ChanceResult> result = List.of(
                new AdvancedProcessingRecipe.ChanceResult(new ItemStack(Items.IRON_BLOCK), 1.0F)
        );

        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.GRINDER, input, 0, result, null, 20, 40
        ));
        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.HYDRAULIC_PRESS, input, 1, result, null, 20, 40
        ));
        assertThrows(IllegalArgumentException.class, () ->
                new AdvancedProcessingRecipe.ChanceResult(new ItemStack(Items.IRON_BLOCK), Float.NaN));
    }

    @Test
    void oilHeaterAndRefineryFluidJsonPreserveEveryProcessingField() {
        AdvancedProcessingRecipe.Serializer serializer = new AdvancedProcessingRecipe.Serializer();
        AdvancedProcessingRecipe heater = serializer.fromJson(
                id("test_oil_heater"),
                json("""
                        {
                          "machine": "oil_heater",
                          "fluid_input": {"tag": "minecraft:water", "amount": 25},
                          "fluid_results": [
                            {"tank": 0, "fluid": "minecraft:lava", "amount": 50}
                          ],
                          "duration": 4,
                          "minimum_temperature": 373.15
                        }
                        """)
        );
        assertFluidRecipe(heater, MultiblockDefinition.OIL_HEATER, FluidTags.WATER.location(), true,
                25, 4, 373.15D);
        assertEquals(List.of(new FluidResultContract(0, "minecraft:lava", 50)), fluidResults(heater));
        assertTrue(heater.matchesFluid(new FluidStack(Fluids.WATER, 25)));
        assertFalse(heater.matchesFluid(new FluidStack(Fluids.WATER, 24)));
        assertFalse(heater.matchesFluid(new FluidStack(Fluids.LAVA, 25)));

        AdvancedProcessingRecipe refinery = serializer.fromJson(
                id("test_refinery"),
                json("""
                        {
                          "machine": "refinery",
                          "fluid_input": {"fluid": "minecraft:water", "amount": 100},
                          "fluid_results": [
                            {"tank": 0, "fluid": "minecraft:water", "amount": 4},
                            {"tank": 1, "fluid": "minecraft:lava", "amount": 3},
                            {"tank": 2, "fluid": "minecraft:flowing_water", "amount": 3}
                          ],
                          "duration": 7,
                          "minimum_temperature": 623.15
                        }
                        """)
        );
        assertFluidRecipe(refinery, MultiblockDefinition.REFINERY, id("water", "minecraft"), false,
                100, 7, 623.15D);
        assertEquals(List.of(
                new FluidResultContract(0, "minecraft:water", 4),
                new FluidResultContract(1, "minecraft:lava", 3),
                new FluidResultContract(2, "minecraft:flowing_water", 3)
        ), fluidResults(refinery));
        assertTrue(refinery.matchesFluid(new FluidStack(Fluids.WATER, 100)));
        assertFalse(refinery.matchesFluid(new FluidStack(Fluids.WATER, 99)));
        assertFalse(refinery.matchesFluid(new FluidStack(Fluids.LAVA, 100)));
    }

    @Test
    void fluidNetworkRoundTripPreservesTagDirectInputsOutputsAndTemperature() {
        AdvancedProcessingRecipe.Serializer serializer = new AdvancedProcessingRecipe.Serializer();
        for (AdvancedProcessingRecipe original : List.of(
                fluidRecipe(
                        "heater_roundtrip",
                        MultiblockDefinition.OIL_HEATER,
                        new AdvancedProcessingRecipe.FluidInput(FluidTags.WATER.location(), true),
                        10,
                        List.of(new AdvancedProcessingRecipe.FluidOutput(0, new FluidStack(Fluids.LAVA, 100))),
                        2,
                        373.15D
                ),
                fluidRecipe(
                        "refinery_roundtrip",
                        MultiblockDefinition.REFINERY,
                        new AdvancedProcessingRecipe.FluidInput(id("water", "minecraft"), false),
                        100,
                        List.of(
                                new AdvancedProcessingRecipe.FluidOutput(0, new FluidStack(Fluids.WATER, 40)),
                                new AdvancedProcessingRecipe.FluidOutput(1, new FluidStack(Fluids.LAVA, 30)),
                                new AdvancedProcessingRecipe.FluidOutput(2, new FluidStack(Fluids.FLOWING_WATER, 30))
                        ),
                        1,
                        623.15D
                )
        )) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                serializer.toNetwork(buffer, original);
                AdvancedProcessingRecipe decoded = serializer.fromNetwork(original.getId(), buffer);
                assertNotNull(decoded);
                assertFluidRecipe(
                        decoded,
                        original.machine(),
                        Objects.requireNonNull(original.fluidInput()).key(),
                        original.fluidInput().tag(),
                        original.fluidInputAmount(),
                        original.duration(),
                        original.minimumTemperatureKelvin()
                );
                assertEquals(fluidResults(original), fluidResults(decoded));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void fluidRecipesRejectUnsupportedMachinesDuplicateTanksAndInvalidTankLayouts() {
        ResourceLocation id = id("invalid_fluid_recipe");
        AdvancedProcessingRecipe.FluidInput input =
                new AdvancedProcessingRecipe.FluidInput(id("water", "minecraft"), false);
        AdvancedProcessingRecipe.FluidOutput tankZero =
                new AdvancedProcessingRecipe.FluidOutput(0, new FluidStack(Fluids.WATER, 10));
        AdvancedProcessingRecipe.FluidOutput duplicateTankZero =
                new AdvancedProcessingRecipe.FluidOutput(0, new FluidStack(Fluids.LAVA, 10));
        AdvancedProcessingRecipe.FluidOutput tankOne =
                new AdvancedProcessingRecipe.FluidOutput(1, new FluidStack(Fluids.LAVA, 10));

        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.GRINDER, input, 10, List.of(tankZero), 1, 0.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.REFINERY, input, 10,
                List.of(tankZero, duplicateTankZero), 1, 0.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.OIL_HEATER, input, 10, List.of(tankOne), 1, 0.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> new AdvancedProcessingRecipe(
                id, MultiblockDefinition.OIL_HEATER, input, 10, List.of(tankZero, tankOne), 1, 0.0D
        ));
        assertThrows(IllegalArgumentException.class, () ->
                new AdvancedProcessingRecipe.FluidOutput(-1, new FluidStack(Fluids.WATER, 1)));
        assertThrows(IllegalArgumentException.class, () ->
                new AdvancedProcessingRecipe.FluidOutput(3, new FluidStack(Fluids.WATER, 1)));
    }

    private static AdvancedProcessingRecipe pressRecipe() {
        return new AdvancedProcessingRecipe(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "test_press"),
                MultiblockDefinition.HYDRAULIC_PRESS,
                Ingredient.of(Items.IRON_INGOT),
                4,
                List.of(
                        new AdvancedProcessingRecipe.ChanceResult(new ItemStack(Items.IRON_BLOCK), 1.0F),
                        new AdvancedProcessingRecipe.ChanceResult(new ItemStack(Items.IRON_NUGGET, 2), 0.25F)
                ),
                HydraulicPressMode.HEAVY,
                120,
                60
        );
    }

    private static AdvancedProcessingRecipe fluidRecipe(
            String name,
            MultiblockDefinition machine,
            AdvancedProcessingRecipe.FluidInput input,
            int inputAmount,
            List<AdvancedProcessingRecipe.FluidOutput> outputs,
            int duration,
            double minimumTemperature
    ) {
        return new AdvancedProcessingRecipe(
                id(name), machine, input, inputAmount, outputs, duration, minimumTemperature
        );
    }

    private static void assertFluidRecipe(
            AdvancedProcessingRecipe recipe,
            MultiblockDefinition machine,
            ResourceLocation inputKey,
            boolean taggedInput,
            int inputAmount,
            int duration,
            double minimumTemperature
    ) {
        assertTrue(recipe.isFluidProcessing());
        assertEquals(machine, recipe.machine());
        assertNotNull(recipe.fluidInput());
        assertEquals(inputKey, recipe.fluidInput().key());
        assertEquals(taggedInput, recipe.fluidInput().tag());
        assertEquals(inputAmount, recipe.fluidInputAmount());
        assertEquals(duration, recipe.duration());
        assertEquals(minimumTemperature, recipe.minimumTemperatureKelvin());
        assertEquals(0, recipe.inputCount());
        assertEquals(0, recipe.energyPerTick());
        assertTrue(recipe.results().isEmpty());
        assertFalse(recipe.canCraftInDimensions(1, 1));
    }

    private static List<FluidResultContract> fluidResults(AdvancedProcessingRecipe recipe) {
        return recipe.fluidOutputs().stream()
                .map(output -> new FluidResultContract(
                        output.tank(),
                        Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(output.stack().getFluid())).toString(),
                        output.stack().getAmount()
                ))
                .toList();
    }

    private static JsonObject json(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static ResourceLocation id(String path) {
        return id(path, "magneticraft");
    }

    private static ResourceLocation id(String path, String namespace) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private record FluidResultContract(int tank, String fluid, int amount) {
    }
}
