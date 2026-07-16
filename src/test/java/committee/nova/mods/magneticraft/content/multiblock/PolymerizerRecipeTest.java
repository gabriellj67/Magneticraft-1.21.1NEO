package committee.nova.mods.magneticraft.content.multiblock;

import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.multiblock.recipe.PolymerizerRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.VanillaIngredientSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolymerizerRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
        if (CraftingHelper.getID(VanillaIngredientSerializer.INSTANCE) == null) {
            CraftingHelper.register(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item"),
                    VanillaIngredientSerializer.INSTANCE
            );
        }
    }

    @Test
    void jsonSupportsOptionalIngredientAndKeepsEveryThermalField() {
        PolymerizerRecipe recipe = new PolymerizerRecipe.Serializer().fromJson(
                id("rubber"),
                JsonParser.parseString("""
                        {
                          "ingredient": {"item": "minecraft:paper"},
                          "fluid": {"fluid": "minecraft:water", "amount": 500},
                          "result": {"item": "minecraft:slime_ball"},
                          "duration": 200,
                          "minimum_temperature": 473.15,
                          "heat_per_tick": 40.0
                        }
                        """).getAsJsonObject()
        );

        assertTrue(recipe.ingredient().orElseThrow().test(new ItemStack(Items.PAPER)));
        assertTrue(recipe.matches(new ItemStack(Items.PAPER), new FluidStack(Fluids.WATER, 500)));
        assertFalse(recipe.matches(ItemStack.EMPTY, new FluidStack(Fluids.WATER, 500)));
        assertFalse(recipe.matches(new ItemStack(Items.PAPER), new FluidStack(Fluids.WATER, 499)));
        assertEquals(Items.SLIME_BALL, recipe.result().getItem());
        assertEquals(200, recipe.duration());
        assertEquals(473.15D, recipe.minimumTemperatureKelvin());
        assertEquals(40.0D, recipe.heatPerTick());
    }

    @Test
    void omittedIngredientAllowsFluidOnlyRecipes() {
        PolymerizerRecipe recipe = new PolymerizerRecipe(
                id("plastic"),
                Optional.empty(),
                new FluidStack(Fluids.WATER, 250),
                new ItemStack(Items.PAPER),
                100,
                423.15D,
                20.0D
        );

        assertTrue(recipe.matches(ItemStack.EMPTY, new FluidStack(Fluids.WATER, 250)));
        assertTrue(recipe.matches(new ItemStack(Items.COAL), new FluidStack(Fluids.WATER, 250)));
    }

    @Test
    void networkRoundTripPreservesOptionalIngredientFluidAndCosts() {
        PolymerizerRecipe original = new PolymerizerRecipe(
                id("network"),
                Optional.of(Ingredient.of(Items.GUNPOWDER)),
                new FluidStack(Fluids.LAVA, 500),
                new ItemStack(Items.SLIME_BALL, 2),
                200,
                473.15D,
                40.0D
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PolymerizerRecipe.Serializer serializer = new PolymerizerRecipe.Serializer();
            serializer.toNetwork(buffer, original);
            PolymerizerRecipe decoded = serializer.fromNetwork(original.getId(), buffer);

            assertTrue(decoded.ingredient().orElseThrow().test(new ItemStack(Items.GUNPOWDER)));
            assertEquals(Fluids.LAVA, decoded.fluidInput().getFluid());
            assertEquals(500, decoded.fluidInput().getAmount());
            assertEquals(2, decoded.result().getCount());
            assertEquals(200, decoded.duration());
            assertEquals(473.15D, decoded.minimumTemperatureKelvin());
            assertEquals(40.0D, decoded.heatPerTick());
        } finally {
            buffer.release();
        }
    }

    @Test
    void invalidResourceAndThermalValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> recipe(FluidStack.EMPTY, 20.0D));
        assertThrows(IllegalArgumentException.class, () -> recipe(new FluidStack(Fluids.WATER, 1), 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new PolymerizerRecipe(
                id("invalid"), Optional.empty(), new FluidStack(Fluids.WATER, 1),
                ItemStack.EMPTY, 20, 300.0D, 1.0D
        ));
    }

    private static PolymerizerRecipe recipe(FluidStack fluid, double heatPerTick) {
        return new PolymerizerRecipe(
                id("invalid"), Optional.empty(), fluid, new ItemStack(Items.PAPER),
                20, 300.0D, heatPerTick
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }
}
