package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Menu-synchronized electric furnace screen.
 */
public final class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.withElectricalPanel(
                new LegacyMachineGuiLayout.Size(BASE_IMAGE_WIDTH, LegacyMachineGuiLayout.STANDARD_HEIGHT)
        );
        imageWidth = size.width();
        imageHeight = size.height();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 14 && mouseX < leftPos + 24
                && mouseY >= topPos + 18 && mouseY < topPos + 70) {
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
        } else if (mouseX >= leftPos + 77 && mouseX < leftPos + 111
                && mouseY >= topPos + 37 && mouseY < topPos + 49) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.magneticraft.progress.tooltip",
                            menu.progress(),
                            menu.totalProgress()
                    ),
                    mouseX,
                    mouseY
            );
        }
        ElectricalStatePanel.renderTooltip(
                graphics, font, mouseX, mouseY, leftPos, topPos, ELECTRICAL_PANEL,
                menu.position(), menu.energyStored(), menu.energyCapacity()
        );
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        MachineScreenLayout.drawPanel(graphics, left, top, imageWidth, imageHeight);
        MachineScreenLayout.drawPlayerInventory(graphics, left, top);
        MachineScreenLayout.drawSlot(
                graphics,
                left,
                top,
                LegacyMachineGuiLayout.ELECTRIC_FURNACE_INPUT.x(),
                LegacyMachineGuiLayout.ELECTRIC_FURNACE_INPUT.y()
        );
        MachineScreenLayout.drawSlot(
                graphics,
                left,
                top,
                LegacyMachineGuiLayout.ELECTRIC_FURNACE_OUTPUT.x(),
                LegacyMachineGuiLayout.ELECTRIC_FURNACE_OUTPUT.y()
        );
        MachineScreenLayout.drawInset(graphics, left + 77, top + 37, 34, 12);
        MachineScreenLayout.drawInset(graphics, left + 14, top + 18, 10, 52);

        int total = menu.totalProgress();
        int progress = total <= 0 ? 0 : 32 * menu.progress() / total;
        graphics.fill(left + 78, top + 38, left + 78 + progress, top + 48, 0xFFE88A2A);

        int capacity = menu.energyCapacity();
        int energy = capacity <= 0 ? 0 : 48 * menu.energyStored() / capacity;
        graphics.fill(left + 16, top + 68 - energy, left + 22, top + 68, 0xFF43D96B);
        ElectricalStatePanel.render(
                graphics, font, left, top, ELECTRICAL_PANEL,
                menu.position(), menu.energyStored(), menu.energyCapacity()
        );
    }
}
