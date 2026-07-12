package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Functional, texture-independent screen shared by every Task 5 device.
 */
public final class SingleBlockMachineScreen extends AbstractContainerScreen<SingleBlockMachineMenu> {
    private final List<Button> inserterButtons = new ArrayList<>();

    public SingleBlockMachineScreen(SingleBlockMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.imageHeight();
    }

    @Override
    protected void init() {
        super.init();
        if (menu.definition() == SingleBlockMachineDefinition.INSERTER) {
            String[] labels = {"白", "标", "损", "NBT", "放", "取"};
            for (int id = 0; id < labels.length; id++) {
                int buttonId = id;
                Button button = Button.builder(
                                Component.literal(labels[id]),
                                ignored -> {
                                    if (minecraft != null && minecraft.gameMode != null) {
                                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                                    }
                                }
                        )
                        .bounds(leftPos + 7 + id * 27, topPos + 67, 24, 16)
                        .build();
                inserterButtons.add(addRenderableWidget(button));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int id = 0; id < inserterButtons.size(); id++) {
            Button button = inserterButtons.get(id);
            boolean enabled = (menu.flags() & (1 << id)) != 0;
            button.setMessage(Component.literal((enabled ? "✓" : "·") + button.getMessage().getString().replace("✓", "").replace("·", "")));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawStatusBars(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                menu.playerInventoryTop() - 12,
                0x404040,
                false
        );
    }

    private void drawStatusBars(GuiGraphics graphics) {
        int barX = leftPos + 151;
        int barBottom = topPos + 68;
        if (menu.energyCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = 48 * menu.energyStored() / menu.energyCapacity();
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFF43D96B);
            barX -= 13;
        }
        if (menu.primaryCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = 48 * menu.primaryFluid() / menu.primaryCapacity();
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFF2F78D0);
            barX -= 13;
        }
        if (menu.secondaryCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = 48 * menu.secondaryFluid() / menu.secondaryCapacity();
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFFE8E8E8);
        }
        if (menu.totalProgress() > 0) {
            MachineScreenLayout.drawInset(graphics, leftPos + 72, topPos + 65, 34, 8);
            int width = 32 * Math.min(menu.progress(), menu.totalProgress()) / menu.totalProgress();
            graphics.fill(leftPos + 73, topPos + 66, leftPos + 73 + width, topPos + 72, 0xFFE88A2A);
        }
        if (menu.temperatureKelvin() > 0.0D) {
            graphics.drawString(
                    font,
                    Component.literal(String.format("%.1f °C", menu.temperatureKelvin() - 273.15D)),
                    leftPos + 8,
                    topPos + 7,
                    0x404040,
                    false
            );
        } else if (menu.voltage() > 0.0D) {
            graphics.drawString(
                    font,
                    Component.literal(String.format("%.1f V", menu.voltage())),
                    leftPos + 8,
                    topPos + 7,
                    0x404040,
                    false
            );
        }
    }
}
