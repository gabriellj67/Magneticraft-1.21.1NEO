package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Compact status screen that always exposes structure, direction, ports, power and operation. */
public final class NuclearFacilityScreen extends AbstractContainerScreen<NuclearFacilityMenu> {
    public NuclearFacilityScreen(NuclearFacilityMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = NuclearFacilityMenu.IMAGE_WIDTH;
        imageHeight = NuclearFacilityMenu.IMAGE_HEIGHT;
        inventoryLabelX = 26;
        inventoryLabelY = NuclearFacilityMenu.PLAYER_TOP - 12;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 88, 0xFF15191E);
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawBar(graphics, 26, 70, 72, 8, menu.progress(), menu.totalProgress(), 0xFFE88A2A);
        drawBar(graphics, 116, 70, 72, 8, menu.storedJoules(), menu.capacityJoules(), 0xFF43D96B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 196), 9, 6, 0x404040, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.structure",
                        menu.width(), menu.height(), menu.depth(),
                        Component.translatable(menu.formed()
                                ? "gui.magneticraft.multiblock.formed"
                                : "gui.magneticraft.multiblock.unformed")),
                10, 22, 0xFFD7DCE2, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.direction",
                        Component.translatable("direction.minecraft." + menu.facing().getName())),
                10, 32, 0xFFD7DCE2, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.ports",
                        portState(1), portState(2), portState(4)),
                10, 82, 0xFFD7DCE2, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.columns", menu.activeColumns()),
                116, 32, 0xFFD7DCE2, false);
        graphics.drawString(font,
                Component.translatable(menu.running()
                        ? "gui.magneticraft.state.running"
                        : "gui.magneticraft.state.stopped"),
                116, 22, menu.running() ? 0xFF43D96B : 0xFF9AA0A6, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_facility.input"),
                26, 62, 0xFF9AA0A6, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_facility.output"),
                116, 62, 0xFF9AA0A6, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component portState(int mask) {
        return Component.translatable((menu.portFlags() & mask) != 0
                ? "gui.magneticraft.nuclear_facility.port_present"
                : "gui.magneticraft.nuclear_facility.port_missing");
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, int height,
                         int value, int capacity, int color) {
        MachineScreenLayout.drawInset(graphics, leftPos + x, topPos + y, width, height);
        int filled = capacity <= 0 ? 0 : (int) Math.min(width - 4, (long) Math.max(0, value) * (width - 4) / capacity);
        graphics.fill(leftPos + x + 2, topPos + y + 2,
                leftPos + x + 2 + filled, topPos + y + height - 2, color);
    }
}
