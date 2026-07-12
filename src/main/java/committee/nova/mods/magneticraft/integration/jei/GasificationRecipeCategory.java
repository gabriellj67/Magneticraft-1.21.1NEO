package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class GasificationRecipeCategory extends AbstractMagneticraftRecipeCategory<GasificationRecipe> {
    GasificationRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.GASIFICATION,
                Component.translatable("block.magneticraft.gasification_unit"),
                ModMachineBlocks.machine(SingleBlockMachineDefinition.GASIFICATION_UNIT).get(),
                guiHelper,
                166,
                68
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GasificationRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 16)
                .setStandardSlotBackground()
                .addIngredients(recipe.input());
        if (!recipe.itemOutput().isEmpty()) {
            builder.addOutputSlot(102, 16)
                    .setOutputSlotBackground()
                    .addItemStack(recipe.itemOutput());
        }
        if (!recipe.fluidOutput().isEmpty()) {
            int amount = recipe.fluidOutput().getAmount();
            builder.addOutputSlot(138, 8)
                    .setStandardSlotBackground()
                    .setFluidRenderer(Math.max(1_000, amount), true, 16, 32)
                    .addFluidStack(recipe.fluidOutput().getFluid(), amount, recipe.fluidOutput().getTag());
        }
    }

    @Override
    public void draw(
            GasificationRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawArrow(graphics, 58, 16);
        drawLine(graphics, Component.translatable("jei.magneticraft.duration", recipe.durationTicks()), 8, 50);
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.temperature",
                        String.format(Locale.ROOT, "%.1f", recipe.minimumTemperatureKelvin())
                ),
                86,
                50
        );
    }
}
