package committee.nova.mods.magneticraft.content.nuclear.facility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearProcessRecipeTest {
    @Test
    void countedInputsCreateAnExactNonMutatingPlan() {
        NuclearProcessRecipe recipe = recipe();
        SimpleContainer container = new SimpleContainer(4);
        container.setItem(0, new ItemStack(Items.CLAY_BALL, 5));
        container.setItem(1, new ItemStack(Items.CLAY_BALL, 7));
        container.setItem(2, new ItemStack(Items.IRON_INGOT, 4));

        var plan = recipe.consumptionPlan(container);
        assertTrue(plan.isPresent());
        assertEquals(5, container.getItem(0).getCount(), "planning mutated input inventory");

        ItemStackHandler handler = new ItemStackHandler(4);
        for (int slot = 0; slot < 4; slot++) {
            handler.setStackInSlot(slot, container.getItem(slot).copy());
        }
        plan.orElseThrow().apply(handler);
        assertTrue(handler.getStackInSlot(0).isEmpty());
        assertTrue(handler.getStackInSlot(1).isEmpty());
        assertTrue(handler.getStackInSlot(2).isEmpty());
    }

    @Test
    void insufficientCountFailsClosed() {
        NuclearProcessRecipe recipe = recipe();
        SimpleContainer container = new SimpleContainer(4);
        container.setItem(0, new ItemStack(Items.CLAY_BALL, 11));
        container.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
        assertFalse(recipe.consumptionPlan(container).isPresent());
    }

    private static NuclearProcessRecipe recipe() {
        return new NuclearProcessRecipe(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "test_fuel"),
                NuclearFacilityType.FUEL_FABRICATOR,
                List.of(
                        new NuclearProcessRecipe.CountedIngredient(Ingredient.of(Items.CLAY_BALL), 12),
                        new NuclearProcessRecipe.CountedIngredient(Ingredient.of(Items.IRON_INGOT), 4)
                ),
                List.of(new ItemStack(Items.DIAMOND)),
                200,
                100
        );
    }
}
