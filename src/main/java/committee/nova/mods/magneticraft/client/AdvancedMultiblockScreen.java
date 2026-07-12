package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Compact shared status screen for every advanced multiblock controller.
 */
public final class AdvancedMultiblockScreen extends AbstractContainerScreen<AdvancedMultiblockMenu> {
    public AdvancedMultiblockScreen(AdvancedMultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 208;
        inventoryLabelY = 115;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF22272E);
        graphics.fill(leftPos + 5, topPos + 14, leftPos + 171, topPos + 112, 0xFF15191E);

        int displayedSlots = menu.definition() == MultiblockDefinition.SHELVING_UNIT
                ? 0
                : Math.min(menu.definition().inventorySlots(), 4);
        for (int slot = 0; slot < displayedSlots; slot++) {
            int x = leftPos + 79 - (displayedSlots - 1) * 9 + slot * 18;
            graphics.fill(x, topPos + 34, x + 18, topPos + 52, 0xFF3A414A);
        }

        if (menu.energyCapacity() > 0) {
            drawHorizontalBar(graphics, leftPos + 8, topPos + 62, 160, menu.energyStored(), menu.energyCapacity(), 0xFF4DA3FF);
        }
        if (menu.totalProgress() > 0) {
            drawHorizontalBar(graphics, leftPos + 8, topPos + 74, 160, menu.progress(), menu.totalProgress(), 0xFFFFB454);
        }
        if (menu.bulkCapacity() > 0) {
            drawHorizontalBar(graphics, leftPos + 8, topPos + 86, 160, menu.bulkAmount(), menu.bulkCapacity(), 0xFFD2A85C);
        }
        for (int tank = 0; tank < menu.tankCount(); tank++) {
            int x = leftPos + 8 + tank * 32;
            int capacity = menu.fluidCapacity(tank);
            graphics.fill(x, topPos + 84, x + 28, topPos + 108, 0xFF303740);
            int filled = scaled(menu.fluidAmount(tank), capacity, 22);
            graphics.fill(x + 1, topPos + 107 - filled, x + 27, topPos + 107, 0xFF4FC3C8);
        }
        graphics.fill(leftPos + 5, topPos + 122, leftPos + 171, topPos + 204, 0xFF171B20);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE6EDF3, false);
        graphics.drawString(
                font,
                Component.translatable(
                        menu.working() ? "gui.magneticraft.state.running" : "gui.magneticraft.state.stopped"
                ),
                8,
                52,
                menu.working() ? 0xFF7EE787 : 0xFF8B949E,
                false
        );
        int right = 168;
        if (menu.voltage() != 0.0D) {
            Component voltage = Component.translatable(
                    "gui.magneticraft.voltage",
                    String.format(Locale.ROOT, "%.1f", menu.voltage())
            );
            graphics.drawString(font, voltage, right - font.width(voltage), 52, 0xFFFFD866, false);
        } else if (menu.temperatureKelvin() != 0.0D) {
            Component temperature = Component.translatable(
                    "gui.magneticraft.temperature_kelvin",
                    String.format(Locale.ROOT, "%.1f", menu.temperatureKelvin())
            );
            graphics.drawString(font, temperature, right - font.width(temperature), 52, 0xFFFF8F70, false);
        }
        if (menu.installedChests() > 0 || menu.definition() == MultiblockDefinition.SHELVING_UNIT) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.magneticraft.shelving_summary",
                            menu.installedChests(),
                            menu.shelvingSlots()
                    ),
                    8,
                    88,
                    0xFFD2A85C,
                    false
            );
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C0C8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (inside(mouseX, mouseY, 8, 62, 160, 6) && menu.energyCapacity() > 0) {
            tooltip(graphics, mouseX, mouseY, "gui.magneticraft.energy.tooltip", menu.energyStored(), menu.energyCapacity());
        } else if (inside(mouseX, mouseY, 8, 74, 160, 6) && menu.totalProgress() > 0) {
            tooltip(graphics, mouseX, mouseY, "gui.magneticraft.progress.tooltip", menu.progress(), menu.totalProgress());
        } else if (inside(mouseX, mouseY, 8, 86, 160, 6) && menu.bulkCapacity() > 0) {
            tooltip(graphics, mouseX, mouseY, "gui.magneticraft.items.tooltip", menu.bulkAmount(), menu.bulkCapacity());
        } else {
            for (int tank = 0; tank < menu.tankCount(); tank++) {
                if (inside(mouseX, mouseY, 8 + tank * 32, 84, 28, 24)) {
                    tooltip(
                            graphics,
                            mouseX,
                            mouseY,
                            "gui.magneticraft.fluid.tooltip",
                            menu.fluidAmount(tank),
                            menu.fluidCapacity(tank)
                    );
                    break;
                }
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private void tooltip(GuiGraphics graphics, int mouseX, int mouseY, String key, int value, int capacity) {
        graphics.renderTooltip(font, Component.translatable(key, value, capacity), mouseX, mouseY);
    }

    private static void drawHorizontalBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int value,
            int capacity,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 6, 0xFF303740);
        graphics.fill(x + 1, y + 1, x + 1 + scaled(value, capacity, width - 2), y + 5, color);
    }

    private static int scaled(int value, int capacity, int size) {
        if (capacity <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, (long) value * size / capacity);
    }
}
