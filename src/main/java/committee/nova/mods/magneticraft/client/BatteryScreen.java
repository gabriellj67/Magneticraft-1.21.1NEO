package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Lightweight battery screen driven only by menu-synchronized data.
 */
public final class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);

    public BatteryScreen(BatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.withElectricalPanel(
                new LegacyMachineGuiLayout.Size(BASE_IMAGE_WIDTH, LegacyMachineGuiLayout.STANDARD_HEIGHT)
        );
        imageWidth = size.width();
        imageHeight = size.height();
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
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
        MachineScreenLayout.drawHeader(graphics, left, top, BASE_IMAGE_WIDTH);
        MachineScreenLayout.drawCard(graphics, left + 5, top + 14, BASE_IMAGE_WIDTH - 10, 58);
        MachineScreenLayout.drawPlayerInventory(graphics, left, top);
        MachineScreenLayout.drawSlot(
                graphics,
                left,
                top,
                LegacyMachineGuiLayout.BATTERY_INPUT.x(),
                LegacyMachineGuiLayout.BATTERY_INPUT.y()
        );
        MachineScreenLayout.drawSlot(
                graphics,
                left,
                top,
                LegacyMachineGuiLayout.BATTERY_OUTPUT.x(),
                LegacyMachineGuiLayout.BATTERY_OUTPUT.y()
        );
        int capacity = menu.energyCapacity();
        int height = capacity <= 0 ? 0 : 45 * menu.energyStored() / capacity;
        MachineScreenLayout.drawStatusBar(
                graphics, left + 82, top + 20, 12, 47,
                height, MachineScreenLayout.ENERGY, true
        );
        ElectricalStatePanel.render(
                graphics, font, left, top, ELECTRICAL_PANEL,
                menu.position(), menu.energyStored(), menu.energyCapacity()
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, BASE_IMAGE_WIDTH - 16),
                titleLabelX, titleLabelY, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(int imageWidth, int imageHeight) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder("battery", imageWidth, imageHeight)
                .element("header", new LegacyMachineGuiLayout.Rect(1, 1, BASE_IMAGE_WIDTH - 2, 15), "sections", 1)
                .element("machine", new LegacyMachineGuiLayout.Rect(5, 14, BASE_IMAGE_WIDTH - 10, 58))
                .child("input_slot", LegacyMachineGuiLayout.slotBounds(LegacyMachineGuiLayout.BATTERY_INPUT),
                        "machine", "machine_controls", 0)
                .child("output_slot", LegacyMachineGuiLayout.slotBounds(LegacyMachineGuiLayout.BATTERY_OUTPUT),
                        "machine", "machine_controls", 0)
                .child("energy", new LegacyMachineGuiLayout.Rect(82, 20, 12, 47),
                        "machine", "machine_controls", 2)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, 78, 166, 86), "sections", 1)
                .element("electrical", ELECTRICAL_PANEL, "sections", 1);
        addPlayerSlots(builder, LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.STANDARD_PLAYER_TOP);
        return builder.build();
    }

    private static void addPlayerSlots(MachineScreenBounds.Builder builder, int left, int top) {
        List<LegacyMachineGuiLayout.Rect> slots = LegacyMachineGuiLayout.playerInventorySlots(left, top);
        for (int index = 0; index < slots.size(); index++) {
            builder.child("player_slot_" + index, slots.get(index), "player_inventory", "player_slots", 0);
        }
    }
}
