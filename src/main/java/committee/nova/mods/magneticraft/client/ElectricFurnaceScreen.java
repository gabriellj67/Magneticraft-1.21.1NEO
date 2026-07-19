package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Menu-synchronized electric furnace screen.
 */
public final class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);
    private static final LegacyMachineGuiLayout.Rect ENERGY_BAR =
            new LegacyMachineGuiLayout.Rect(14, 18, 10, 52);
    private static final LegacyMachineGuiLayout.Rect PROGRESS_BAR =
            new LegacyMachineGuiLayout.Rect(68, 37, 34, 12);

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
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
        if (inside(mouseX, mouseY, ENERGY_BAR)) {
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
        } else if (inside(mouseX, mouseY, PROGRESS_BAR)) {
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
        MachineScreenLayout.drawHeader(graphics, left, top, BASE_IMAGE_WIDTH);
        MachineScreenLayout.drawCard(graphics, left + 5, top + 14, BASE_IMAGE_WIDTH - 10, 58);
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
        int total = menu.totalProgress();
        int progress = total <= 0 ? 0 : (PROGRESS_BAR.width() - 4) * menu.progress() / total;
        MachineScreenLayout.drawStatusBar(
                graphics,
                left + PROGRESS_BAR.x(), top + PROGRESS_BAR.y(), PROGRESS_BAR.width(), PROGRESS_BAR.height(),
                progress, MachineScreenLayout.PROGRESS, false
        );

        int capacity = menu.energyCapacity();
        int energy = capacity <= 0 ? 0 : (ENERGY_BAR.height() - 4) * menu.energyStored() / capacity;
        MachineScreenLayout.drawStatusBar(
                graphics,
                left + ENERGY_BAR.x(), top + ENERGY_BAR.y(), ENERGY_BAR.width(), ENERGY_BAR.height(),
                energy, MachineScreenLayout.ENERGY, true
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
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder("electric_furnace", imageWidth, imageHeight)
                .element("header", new LegacyMachineGuiLayout.Rect(1, 1, BASE_IMAGE_WIDTH - 2, 15), "sections", 1)
                .element("machine", new LegacyMachineGuiLayout.Rect(5, 14, BASE_IMAGE_WIDTH - 10, 58))
                .child("input_slot", LegacyMachineGuiLayout.slotBounds(LegacyMachineGuiLayout.ELECTRIC_FURNACE_INPUT),
                        "machine", "machine_controls", 0)
                .child("output_slot", LegacyMachineGuiLayout.slotBounds(LegacyMachineGuiLayout.ELECTRIC_FURNACE_OUTPUT),
                        "machine", "machine_controls", 0)
                .child("progress", PROGRESS_BAR,
                        "machine", "machine_controls", 2)
                .child("energy", ENERGY_BAR,
                        "machine", "machine_controls", 2)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, 78, 166, 86), "sections", 1)
                .element("electrical", ELECTRICAL_PANEL, "sections", 1);
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT, LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }

    private boolean inside(int mouseX, int mouseY, LegacyMachineGuiLayout.Rect bounds) {
        return mouseX >= leftPos + bounds.x() && mouseX < leftPos + bounds.right()
                && mouseY >= topPos + bounds.y() && mouseY < topPos + bounds.bottom();
    }
}
