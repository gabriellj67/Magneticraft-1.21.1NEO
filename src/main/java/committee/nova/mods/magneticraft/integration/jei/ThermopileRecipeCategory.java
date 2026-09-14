package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class ThermopileRecipeCategory extends AbstractMagneticraftRecipeCategory<ThermopileRecipe> {
    ThermopileRecipeCategory(IGuiHelper guiHelper) {
        super(
                MagneticraftJeiPlugin.THERMOPILE,
                Component.translatable("block.magneticraft.thermopile"),
                ModMachineBlocks.machine(SingleBlockMachineDefinition.THERMOPILE).get(),
                guiHelper,
                166,
                68
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermopileRecipe recipe, IFocusGroup focuses) {
        ItemStack display = new ItemStack(recipe.block());
        if (display.isEmpty() && recipe.block() instanceof LiquidBlock liquidBlock) {
            display = new ItemStack(liquidBlock.fluid.getBucket());
        }
        if (display.isEmpty() && recipe.block() == Blocks.FIRE) {
            display = new ItemStack(Items.FLINT_AND_STEEL);
        }
        if (display.isEmpty()) {
            display = new ItemStack(Items.BARRIER);
        }
        var inputSlot = builder.addInputSlot(8, 16)
                .setStandardSlotBackground()
                .addItemStack(display)
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal(
                        String.valueOf(BuiltInRegistries.BLOCK.getKey(recipe.block()))
                )));
        if (!recipe.stateProperties().isEmpty()) {
            inputSlot.addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                    "jei.magneticraft.block_state",
                    formatStateProperties(recipe.stateProperties())
            )));
        }
    }

    @Override
    public void draw(
            ThermopileRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.temperature",
                        String.format(Locale.ROOT, "%.1f", recipe.temperatureKelvin())
                ),
                36,
                14
        );
        drawLine(
                graphics,
                Component.translatable(
                        "jei.magneticraft.conductivity",
                        String.format(Locale.ROOT, "%.3f", recipe.conductivity())
                ),
                36,
                28
        );
    }

    private static String formatStateProperties(Map<String, String> properties) {
        return properties.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
