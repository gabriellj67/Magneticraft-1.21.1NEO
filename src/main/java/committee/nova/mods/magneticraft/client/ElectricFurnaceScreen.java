package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu-synchronized electric furnace screen.
 */
public final class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        MachineScreenLayout.drawPanel(graphics, left, top, imageWidth, imageHeight);
        MachineScreenLayout.drawPlayerInventory(graphics, left, top);
        MachineScreenLayout.drawSlot(graphics, left, top, 56, 35);
        MachineScreenLayout.drawSlot(graphics, left, top, 116, 35);
        MachineScreenLayout.drawInset(graphics, left + 77, top + 37, 34, 12);
        MachineScreenLayout.drawInset(graphics, left + 14, top + 18, 10, 52);

        int total = menu.totalProgress();
        int progress = total <= 0 ? 0 : 32 * menu.progress() / total;
        graphics.fill(left + 78, top + 38, left + 78 + progress, top + 48, 0xFFE88A2A);

        int capacity = menu.energyCapacity();
        int energy = capacity <= 0 ? 0 : 48 * menu.energyStored() / capacity;
        graphics.fill(left + 16, top + 68 - energy, left + 22, top + 68, 0xFF43D96B);
    }
}
