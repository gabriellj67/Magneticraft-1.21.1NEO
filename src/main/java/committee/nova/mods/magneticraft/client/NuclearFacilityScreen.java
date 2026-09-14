package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

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
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, imageWidth);
        MachineScreenLayout.drawCard(graphics, leftPos + 7, topPos + 18, imageWidth - 14, 70);
        MachineScreenLayout.drawCard(
                graphics, leftPos + 23, topPos + NuclearFacilityMenu.PLAYER_TOP - 2, 166, 82
        );
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawBar(graphics, 26, 70, 72, 8, menu.progress(), menu.totalProgress(), MachineScreenLayout.PROGRESS);
        drawBar(graphics, 116, 70, 72, 8, menu.storedJoules(), menu.capacityJoules(), MachineScreenLayout.ENERGY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 196), 9, 6,
                MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.structure",
                        menu.width(), menu.height(), menu.depth(),
                        Component.translatable(menu.formed()
                                ? "gui.magneticraft.multiblock.formed"
                                : "gui.magneticraft.multiblock.unformed")),
                10, 22, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.direction",
                        Component.translatable("direction.minecraft." + menu.facing().getName())),
                10, 32, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.ports",
                        portState(1), portState(2), portState(4)),
                10, 82, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font,
                Component.translatable("gui.magneticraft.nuclear_facility.columns", menu.activeColumns()),
                116, 32, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font,
                Component.translatable(menu.running()
                        ? "gui.magneticraft.state.running"
                        : "gui.magneticraft.state.stopped"),
                116, 22, menu.running() ? MachineScreenLayout.SUCCESS : MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_facility.input"),
                26, 62, MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_facility.output"),
                116, 62, MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        int filled = capacity <= 0 ? 0 : (int) Math.min(width - 4, (long) Math.max(0, value) * (width - 4) / capacity);
        MachineScreenLayout.drawStatusBar(
                graphics, leftPos + x, topPos + y, width, height, filled, color, false
        );
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(int imageWidth, int imageHeight) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "nuclear_facility", imageWidth, imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, imageWidth - 2, 15), "sections", 1)
                .element("telemetry", new LegacyMachineGuiLayout.Rect(7, 18, imageWidth - 14, 70))
                .child("progress", new LegacyMachineGuiLayout.Rect(26, 70, 72, 8),
                        "telemetry", "status_bars", 4)
                .child("energy", new LegacyMachineGuiLayout.Rect(116, 70, 72, 8),
                        "telemetry", "status_bars", 4)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(
                        23, NuclearFacilityMenu.PLAYER_TOP - 2, 166, 82
                ), "sections", 2);
        for (int index = 0; index < 4; index++) {
            builder.child("input_slot_" + index,
                    LegacyMachineGuiLayout.slotBounds(new LegacyMachineGuiLayout.Point(26 + index * 18, 44)),
                    "telemetry", "machine_slots", 0);
            builder.child("output_slot_" + index,
                    LegacyMachineGuiLayout.slotBounds(new LegacyMachineGuiLayout.Point(116 + index * 18, 44)),
                    "telemetry", "machine_slots", 0);
        }
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                26, NuclearFacilityMenu.PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }
}
