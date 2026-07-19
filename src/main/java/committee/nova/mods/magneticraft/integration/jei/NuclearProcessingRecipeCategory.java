package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearProcessRecipe;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** Count-aware JEI view for the compressed open-cycle nuclear fuel front end. */
final class NuclearProcessingRecipeCategory extends AbstractMagneticraftRecipeCategory<NuclearProcessRecipe> {
    NuclearProcessingRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.NUCLEAR_PROCESSING,
                Component.translatable("jei.magneticraft.nuclear_processing"),
                ModNuclearBlocks.controller(
                        committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType.URANIUM_PROCESSOR
                ).get(),
                guiHelper,
                176,
                72
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, NuclearProcessRecipe recipe, IFocusGroup focuses) {
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            NuclearProcessRecipe.CountedIngredient counted = recipe.ingredients().get(index);
            builder.addInputSlot(8 + index * 20, 18)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.stream(counted.ingredient().getItems())
                            .map(stack -> counted(stack, counted.count()))
                            .toList());
        }
        for (int index = 0; index < recipe.results().size(); index++) {
            builder.addOutputSlot(100 + index * 20, 18)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.results().get(index));
        }
    }

    @Override
    public void draw(NuclearProcessRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        drawArrow(graphics, 78, 18);
        drawLine(graphics, Component.translatable(
                "block.magneticraft." + recipe.facility().id()), 8, 4);
        String routeKey = recipe.getId().getPath().endsWith("_direct")
                ? "jei.magneticraft.nuclear_processing.route.basic"
                : recipe.facility() == NuclearFacilityType.CENTRIFUGE_CASCADE
                        ? "jei.magneticraft.nuclear_processing.route.advanced"
                        : null;
        if (routeKey != null) {
            drawLine(graphics, Component.translatable(routeKey), 8, 38);
        }
        drawLine(graphics, Component.translatable(
                "jei.magneticraft.duration", recipe.durationTicks()), 8, 50);
        drawLine(graphics, Component.translatable(
                "jei.magneticraft.energy_per_tick_joules", recipe.joulesPerTick()), 92, 50);
    }

    private static ItemStack counted(ItemStack template, int count) {
        ItemStack stack = template.copy();
        stack.setCount(Math.min(count, stack.getMaxStackSize()));
        return stack;
    }
}
