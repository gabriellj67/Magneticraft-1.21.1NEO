package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelHandlingStatus;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

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
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, imageWidth);
        MachineScreenLayout.drawCard(graphics, leftPos + 7, topPos + 18, imageWidth - 14, 100);
        MachineScreenLayout.drawCard(
                graphics, leftPos + 23, topPos + SpentFuelPoolMenu.PLAYER_TOP - 2, 166, 82
        );
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, imageWidth - 18), 9, 6,
                MachineScreenLayout.TEXT_PRIMARY, false);
        line(graphics, 24, Component.translatable("gui.magneticraft.spent_fuel_pool.structure",
                menu.width(), menu.length(), menu.height(),
                Component.translatable(menu.formed() ? "gui.magneticraft.multiblock.formed"
                        : "gui.magneticraft.multiblock.unformed")), MachineScreenLayout.TEXT_PRIMARY);
        line(graphics, 36, Component.translatable("gui.magneticraft.spent_fuel_pool.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName())),
                MachineScreenLayout.TEXT_PRIMARY);
        line(graphics, 48, Component.translatable("gui.magneticraft.spent_fuel_pool.port",
                Component.translatable(menu.portPresent() ? "gui.magneticraft.state.connected"
                        : "gui.magneticraft.state.disconnected")), MachineScreenLayout.TEXT_PRIMARY);
        SpentFuelHandlingStatus status = menu.handlingStatus();
        line(graphics, 60, Component.translatable("gui.magneticraft.spent_fuel_pool.handling_status",
                Component.translatable(status.translationKey())), statusColor(status));
        String coolingKey = menu.fuelCount() == 0
                ? "gui.magneticraft.spent_fuel_pool.cooling_standby"
                : menu.cooling()
                        ? "gui.magneticraft.spent_fuel_pool.cooling"
                        : "gui.magneticraft.spent_fuel_pool.cooling_blocked";
        int coolingColor = menu.fuelCount() == 0 ? MachineScreenLayout.TEXT_PRIMARY
                : menu.cooling() ? MachineScreenLayout.SUCCESS : MachineScreenLayout.DANGER;
        line(graphics, 72, Component.translatable(coolingKey), coolingColor);
        line(graphics, 84, Component.translatable("gui.magneticraft.spent_fuel_pool.inventory",
                menu.transferableAssemblies(), menu.safeAssemblies(), menu.fuelCount()),
                MachineScreenLayout.TEXT_PRIMARY);
        line(graphics, 96, Component.translatable("gui.magneticraft.spent_fuel_pool.thermal",
                menu.waterBlocks(), menu.releasedHeat(), Math.round(menu.portTemperatureKelvin())),
                MachineScreenLayout.TEXT_PRIMARY);
        line(graphics, 108, Component.translatable("gui.magneticraft.spent_fuel_pool.radiation",
                String.format(java.util.Locale.ROOT, "%.3f", menu.doseRateMillisievertsPerHour())),
                MachineScreenLayout.WARNING);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    private void line(GuiGraphics graphics, int y, Component text, int color) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, text, INFO_WIDTH),
                INFO_X, y, color, false);
    }

    private static int statusColor(SpentFuelHandlingStatus status) {
        return switch (status) {
            case EMPTY -> MachineScreenLayout.TEXT_PRIMARY;
            case DANGEROUS -> MachineScreenLayout.DANGER;
            case COOLING -> MachineScreenLayout.WARNING;
            case TRANSFERABLE -> MachineScreenLayout.ACCENT;
            case SEALABLE -> MachineScreenLayout.SUCCESS;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(int imageWidth, int imageHeight) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "spent_fuel_pool", imageWidth, imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, imageWidth - 2, 15), "sections", 1)
                .element("telemetry", new LegacyMachineGuiLayout.Rect(7, 18, imageWidth - 14, 100))
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(
                        23, SpentFuelPoolMenu.PLAYER_TOP - 2, 166, 82
                ), "sections", 2);
        for (int index = 0; index < 16; index++) {
            builder.child("fuel_slot_" + index, LegacyMachineGuiLayout.slotBounds(
                    new LegacyMachineGuiLayout.Point(26 + index % 4 * 18, 34 + index / 4 * 18)
            ), "telemetry", "fuel_slots", 0);
        }
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                26, SpentFuelPoolMenu.PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }
}
