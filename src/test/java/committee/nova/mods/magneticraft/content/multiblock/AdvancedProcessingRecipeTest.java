package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedProcessingRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
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
}
