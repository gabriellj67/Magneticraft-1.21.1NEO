package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

final class AdvancedProcessingRecipeCategory extends AbstractMagneticraftRecipeCategory<AdvancedProcessingRecipe> {
    AdvancedProcessingRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.ADVANCED_PROCESSING,
                Component.translatable("jei.magneticraft.advanced_processing"),
                ModAdvancedBlocks.controller(MultiblockDefinition.GRINDER).get(),
                guiHelper,
                166,
                92
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AdvancedProcessingRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = Arrays.stream(recipe.input().getItems())
                .map(ItemStack::copy)
                .peek(stack -> stack.setCount(recipe.inputCount()))
                .toList();
        builder.addInputSlot(8, 14)
                .setStandardSlotBackground()
                .addItemStacks(inputs);

        List<AdvancedProcessingRecipe.ChanceResult> outputs = recipe.chanceResults();
        for (int index = 0; index < outputs.size(); index++) {
            AdvancedProcessingRecipe.ChanceResult output = outputs.get(index);
            builder.addOutputSlot(108 + index * 20, 14)
                    .setOutputSlotBackground()
                    .addItemStack(output.stack())
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                            "jei.magneticraft.chance",
                            Math.round(output.chance() * 100.0F)
                    )));
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
                .addItemLike(ModAdvancedBlocks.controller(recipe.machine()).get());
    }

    @Override
    public void draw(
            AdvancedProcessingRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawArrow(graphics, 62, 14);
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.machine",
                        Component.translatable("block.magneticraft." + recipe.machine().id())
                ),
                8,
                46
        );
        drawLine(graphics, Component.translatable("jei.magneticraft.duration", recipe.duration()), 8, 60);
        drawLine(
                graphics,
                Component.translatable("jei.magneticraft.energy_per_tick", recipe.energyPerTick()),
                86,
                60
        );
        if (recipe.pressMode() != null) {
            drawLine(
                    graphics,
                    Component.translatable(
                            "jei.magneticraft.press_mode",
                            Component.translatable(
                                    "message.magneticraft.hydraulic_press_mode."
                                            + recipe.pressMode().serializedName()
                            )
                    ),
                    8,
                    76
            );
        }
    }
}
