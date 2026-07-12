package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Lightweight battery screen driven only by menu-synchronized data.
 */
public final class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {
    public BatteryScreen(BatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 82 && mouseX < leftPos + 94
                && mouseY >= topPos + 20 && mouseY < topPos + 67) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.magneticraft.energy.tooltip",
                            menu.energyStored(),
                            menu.energyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        MachineScreenLayout.drawPanel(graphics, left, top, imageWidth, imageHeight);
        MachineScreenLayout.drawPlayerInventory(graphics, left, top);
        MachineScreenLayout.drawSlot(graphics, left, top, 53, 35);
        MachineScreenLayout.drawSlot(graphics, left, top, 107, 35);
        MachineScreenLayout.drawInset(graphics, left + 82, top + 20, 12, 47);

        int capacity = menu.energyCapacity();
        int height = capacity <= 0 ? 0 : 45 * menu.energyStored() / capacity;
        graphics.fill(left + 84, top + 65 - height, left + 92, top + 65, 0xFF43D96B);
    }
}
