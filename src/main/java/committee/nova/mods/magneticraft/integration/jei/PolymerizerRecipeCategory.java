package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.recipe.PolymerizerRecipe;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

final class PolymerizerRecipeCategory extends AbstractMagneticraftRecipeCategory<PolymerizerRecipe> {
    PolymerizerRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.POLYMERIZING,
                Component.translatable("jei.magneticraft.polymerizing"),
                ModAdvancedBlocks.controller(MultiblockDefinition.POLYMERIZER).get(),
                guiHelper,
                166,
                82
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PolymerizerRecipe recipe, IFocusGroup focuses) {
        recipe.ingredient().ifPresent(ingredient -> builder.addInputSlot(8, 14)
                .setStandardSlotBackground()
                .addIngredients(ingredient));
        FluidStack fluid = recipe.fluidInput();
        builder.addInputSlot(34, 6)
                .setStandardSlotBackground()
                .setFluidRenderer(Math.max(1, fluid.getAmount()), true, 16, 32)
                .addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getTag());
        builder.addOutputSlot(108, 14)
                .setOutputSlotBackground()
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
            PolymerizerRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawArrow(graphics, 66, 14);
        drawLine(graphics, Component.translatable("jei.magneticraft.duration", recipe.duration()), 8, 50);
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.minimum_temperature",
                        Math.round(recipe.minimumTemperatureKelvin())
                ),
                8,
                64
        );
        drawLine(
                graphics,
                Component.translatable("jei.magneticraft.heat_per_tick", Math.round(recipe.heatPerTick())),
                92,
                64
        );
    }
}
