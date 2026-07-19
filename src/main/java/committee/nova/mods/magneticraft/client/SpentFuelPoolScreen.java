package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelHandlingStatus;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class SpentFuelPoolScreen extends AbstractContainerScreen<SpentFuelPoolMenu> {
    private static final int INFO_X = 100;
    private static final int INFO_WIDTH = SpentFuelPoolMenu.IMAGE_WIDTH - INFO_X - 8;

    public SpentFuelPoolScreen(SpentFuelPoolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SpentFuelPoolMenu.IMAGE_WIDTH;
        imageHeight = SpentFuelPoolMenu.IMAGE_HEIGHT;
        inventoryLabelX = 26;
        inventoryLabelY = SpentFuelPoolMenu.PLAYER_TOP - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 118, 0xFF15191E);
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, imageWidth - 18), 9, 6, 0x404040, false);
        line(graphics, 24, Component.translatable("gui.magneticraft.spent_fuel_pool.structure",
                menu.width(), menu.length(), menu.height(),
                Component.translatable(menu.formed() ? "gui.magneticraft.multiblock.formed"
                        : "gui.magneticraft.multiblock.unformed")), 0xFFD7DCE2);
        line(graphics, 36, Component.translatable("gui.magneticraft.spent_fuel_pool.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName())), 0xFFD7DCE2);
        line(graphics, 48, Component.translatable("gui.magneticraft.spent_fuel_pool.port",
                Component.translatable(menu.portPresent() ? "gui.magneticraft.state.connected"
                        : "gui.magneticraft.state.disconnected")), 0xFFD7DCE2);
        SpentFuelHandlingStatus status = menu.handlingStatus();
        line(graphics, 60, Component.translatable("gui.magneticraft.spent_fuel_pool.handling_status",
                Component.translatable(status.translationKey())), statusColor(status));
        String coolingKey = menu.fuelCount() == 0
                ? "gui.magneticraft.spent_fuel_pool.cooling_standby"
                : menu.cooling()
                        ? "gui.magneticraft.spent_fuel_pool.cooling"
                        : "gui.magneticraft.spent_fuel_pool.cooling_blocked";
        int coolingColor = menu.fuelCount() == 0 ? 0xFFD7DCE2
                : menu.cooling() ? 0xFF43D96B : 0xFFFF6B5F;
        line(graphics, 72, Component.translatable(coolingKey), coolingColor);
        line(graphics, 84, Component.translatable("gui.magneticraft.spent_fuel_pool.inventory",
                menu.transferableAssemblies(), menu.safeAssemblies(), menu.fuelCount()), 0xFFD7DCE2);
        line(graphics, 96, Component.translatable("gui.magneticraft.spent_fuel_pool.thermal",
                menu.waterBlocks(), menu.releasedHeat(), Math.round(menu.portTemperatureKelvin())), 0xFFD7DCE2);
        line(graphics, 108, Component.translatable("gui.magneticraft.spent_fuel_pool.radiation",
                String.format(java.util.Locale.ROOT, "%.3f", menu.doseRateMillisievertsPerHour())), 0xFFFFC857);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void line(GuiGraphics graphics, int y, Component text, int color) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, text, INFO_WIDTH),
                INFO_X, y, color, false);
    }

    private static int statusColor(SpentFuelHandlingStatus status) {
        return switch (status) {
            case EMPTY -> 0xFFD7DCE2;
            case DANGEROUS -> 0xFFFF4D4D;
            case COOLING -> 0xFFFFB74D;
            case TRANSFERABLE -> 0xFF67B7FF;
            case SEALABLE -> 0xFF67D98B;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
