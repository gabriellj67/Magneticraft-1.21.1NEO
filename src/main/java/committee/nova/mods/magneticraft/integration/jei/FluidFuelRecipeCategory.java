package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class FluidFuelRecipeCategory extends AbstractMagneticraftRecipeCategory<FluidFuelRecipe> {
    FluidFuelRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.FLUID_FUEL,
                Component.translatable("block.magneticraft.industrial_combustion_chamber"),
                ModAdvancedBlocks.controller(MultiblockDefinition.BIG_COMBUSTION_CHAMBER).get(),
                guiHelper,
                166,
                76
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidFuelRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 8)
                .setStandardSlotBackground()
                .setFluidRenderer(1_000, true, 16, 32)
                .addFluidStack(recipe.fluid(), 1_000);
    }

    @Override
    public void draw(
            FluidFuelRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawLine(graphics, Component.translatable("jei.magneticraft.duration", recipe.durationTicks()), 36, 10);
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.power",
                        String.format(Locale.ROOT, "%.1f", recipe.powerPerTick())
                ),
                36,
                24
        );
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.total_energy",
                        String.format(Locale.ROOT, "%.1f", recipe.totalEnergyPerMilliBucket())
                ),
                8,
                54
        );
    }
}
