package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

final class SluiceRecipeCategory extends AbstractMagneticraftRecipeCategory<SluiceRecipe> {
    private static final int OUTPUT_COLUMNS = 5;

    SluiceRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.SLUICE,
                Component.translatable("block.magneticraft.sluice_box"),
                ModMachineBlocks.machine(SingleBlockMachineDefinition.SLUICE_BOX).get(),
                guiHelper,
                166,
                62
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SluiceRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 26)
                .setStandardSlotBackground()
                .addIngredients(recipe.input());

        List<SluiceRecipe.ChanceOutput> outputs = recipe.outputs();
        for (int index = 0; index < outputs.size(); index++) {
            SluiceRecipe.ChanceOutput output = outputs.get(index);
            builder.addOutputSlot(68 + index % OUTPUT_COLUMNS * 20, 7 + index / OUTPUT_COLUMNS * 22)
                    .setOutputSlotBackground()
                    .addItemStack(output.stack())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                            "jei.magneticraft.chance",
                            Math.round(output.chance() * 100.0F)
                    )));
        }
    }

    @Override
    public void draw(
            SluiceRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawArrow(graphics, 36, 26);
    }
}
