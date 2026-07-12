package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class CrushingRecipeCategory extends AbstractMagneticraftRecipeCategory<CrushingRecipe> {
    CrushingRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.CRUSHING,
                Component.translatable("block.magneticraft.crushing_table"),
                ModMachineBlocks.CRUSHING_TABLE.get(),
                guiHelper,
                148,
                54
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(20, 14)
                .setStandardSlotBackground()
                .addIngredients(recipe.input());
        builder.addOutputSlot(110, 14)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(
            CrushingRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawArrow(graphics, 62, 14);
        if (recipe.requiredLevel() >= 0) {
            drawLine(
                    graphics,
                    Component.translatable("jei.magneticraft.required_level", recipe.requiredLevel()),
                    20,
                    40
            );
        }
    }
}
