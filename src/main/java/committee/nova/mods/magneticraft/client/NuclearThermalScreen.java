package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalMenu;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Direct structure, direction, port and bottleneck telemetry for every thermal multiblock. */
public final class NuclearThermalScreen extends AbstractContainerScreen<NuclearThermalMenu> {
    public NuclearThermalScreen(NuclearThermalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = NuclearThermalMenu.IMAGE_WIDTH;
        imageHeight = NuclearThermalMenu.IMAGE_HEIGHT;
        inventoryLabelX = 35;
        inventoryLabelY = NuclearThermalMenu.PLAYER_TOP - 12;
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
        MachineScreenLayout.drawCard(graphics, leftPos + 7, topPos + 18, imageWidth - 14, 73);
        MachineScreenLayout.drawCard(
                graphics, leftPos + 32, topPos + NuclearThermalMenu.PLAYER_TOP - 2, 166, 82
        );
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawBar(graphics, 12, 78, 96, menu.primaryAmount(),
                Math.max(1, menu.primaryAmount() + menu.secondaryAmount()), MachineScreenLayout.HEAT);
        drawBar(graphics, 124, 78, 96, menu.inputAmount(),
                Math.max(1, menu.inputAmount() + menu.outputAmount()), MachineScreenLayout.FLUID);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 214), 9, 6,
                MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.structure",
                menu.width(), menu.length(), menu.height(),
                Component.translatable(menu.formed() ? "gui.magneticraft.multiblock.formed" : "gui.magneticraft.multiblock.unformed")),
                10, 22, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName())), 10, 33,
                MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.state",
                Component.translatable(menu.backpressured()
                        ? "gui.magneticraft.nuclear_thermal.backpressure"
                        : menu.working() ? "gui.magneticraft.state.running" : "gui.magneticraft.state.stopped")),
                10, 44, menu.backpressured() ? MachineScreenLayout.DANGER
                        : menu.working() ? MachineScreenLayout.SUCCESS : MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.rate",
                menu.transferRate(), menu.transferredHeat()), 10, 55, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.core",
                menu.exchangerBlocks(), menu.fillBlocks(), menu.fanBlocks()), 10, 66,
                MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.temperature",
                String.format(Locale.ROOT, "%.1f", menu.heatTemperatureKelvin())), 126, 44,
                MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.energy",
                menu.storedJoules(), menu.capacityJoules()), 126, 55, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.ports", presentPorts()),
                126, 66, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private Component presentPorts() {
        List<Component> roles = new ArrayList<>();
        for (NuclearThermalPortRole role : NuclearThermalPortRole.values()) {
            if (menu.hasPort(role)) {
                roles.add(Component.translatable("gui.magneticraft.nuclear_thermal.port." + role.name().toLowerCase(Locale.ROOT)));
            }
        }
        return Component.literal(String.join(", ", roles.stream().map(Component::getString).toList()));
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, int value, int capacity, int color) {
        int filled = (int) Math.min(width - 4, (long) Math.max(0, value) * (width - 4) / Math.max(1, capacity));
        MachineScreenLayout.drawStatusBar(graphics, leftPos + x, topPos + y, width, 8, filled, color, false);
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(int imageWidth, int imageHeight) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "nuclear_thermal", imageWidth, imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, imageWidth - 2, 15), "sections", 1)
                .element("telemetry", new LegacyMachineGuiLayout.Rect(7, 18, imageWidth - 14, 73))
                .child("primary", new LegacyMachineGuiLayout.Rect(12, 78, 96, 8),
                        "telemetry", "status_bars", 4)
                .child("secondary", new LegacyMachineGuiLayout.Rect(124, 78, 96, 8),
                        "telemetry", "status_bars", 4)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(
                        32, NuclearThermalMenu.PLAYER_TOP - 2, 166, 82
                ), "sections", 2);
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                35, NuclearThermalMenu.PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }
}
