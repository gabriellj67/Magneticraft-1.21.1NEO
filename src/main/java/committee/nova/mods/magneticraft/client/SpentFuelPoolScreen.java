package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class SpentFuelPoolScreen extends AbstractContainerScreen<SpentFuelPoolMenu> {
    public SpentFuelPoolScreen(SpentFuelPoolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SpentFuelPoolMenu.IMAGE_WIDTH;
        imageHeight = SpentFuelPoolMenu.IMAGE_HEIGHT;
        inventoryLabelX = 26;
        inventoryLabelY = SpentFuelPoolMenu.PLAYER_TOP - 12;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 112, 0xFF15191E);
        for (Slot slot : menu.slots) MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 196), 9, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.structure",
                menu.width(), menu.length(), menu.height(),
                Component.translatable(menu.formed() ? "gui.magneticraft.multiblock.formed"
                        : "gui.magneticraft.multiblock.unformed")), 100, 24, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName())),
                100, 36, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.port",
                Component.translatable(menu.portPresent() ? "gui.magneticraft.state.connected"
                        : "gui.magneticraft.state.disconnected")), 100, 48, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable(menu.cooling()
                        ? "gui.magneticraft.spent_fuel_pool.cooling"
                        : "gui.magneticraft.spent_fuel_pool.cooling_blocked"),
                100, 60, menu.cooling() ? 0xFF43D96B : 0xFFFF6B5F, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.inventory",
                menu.safeAssemblies(), menu.fuelCount()), 100, 72, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.thermal",
                menu.waterBlocks(), menu.releasedHeat(), Math.round(menu.portTemperatureKelvin())),
                100, 84, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.spent_fuel_pool.radiation",
                String.format(java.util.Locale.ROOT, "%.3f", menu.doseRateMillisievertsPerHour())),
                100, 96, 0xFFFFC857, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
