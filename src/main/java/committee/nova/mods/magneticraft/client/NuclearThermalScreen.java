package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalMenu;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 91, 0xFF15191E);
        drawBar(graphics, 12, 78, 96, menu.primaryAmount(), Math.max(1, menu.primaryAmount() + menu.secondaryAmount()), 0xFFFF7A3D);
        drawBar(graphics, 124, 78, 96, menu.inputAmount(), Math.max(1, menu.inputAmount() + menu.outputAmount()), 0xFF4E9ED4);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 214), 9, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.structure",
                menu.width(), menu.length(), menu.height(),
                Component.translatable(menu.formed() ? "gui.magneticraft.multiblock.formed" : "gui.magneticraft.multiblock.unformed")),
                10, 22, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName())), 10, 33, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.state",
                Component.translatable(menu.backpressured()
                        ? "gui.magneticraft.nuclear_thermal.backpressure"
                        : menu.working() ? "gui.magneticraft.state.running" : "gui.magneticraft.state.stopped")),
                10, 44, menu.backpressured() ? 0xFFFF5E57 : menu.working() ? 0xFF43D96B : 0xFF9AA0A6, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.rate",
                menu.transferRate(), menu.transferredHeat()), 10, 55, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.core",
                menu.exchangerBlocks(), menu.fillBlocks(), menu.fanBlocks()), 10, 66, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.temperature",
                String.format(Locale.ROOT, "%.1f", menu.heatTemperatureKelvin())), 126, 44, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.energy",
                menu.storedJoules(), menu.capacityJoules()), 126, 55, 0xFFD7DCE2, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.nuclear_thermal.ports", presentPorts()),
                126, 66, 0xFFD7DCE2, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
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
        MachineScreenLayout.drawInset(graphics, leftPos + x, topPos + y, width, 8);
        int filled = (int) Math.min(width - 4, (long) Math.max(0, value) * (width - 4) / Math.max(1, capacity));
        graphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 2 + filled, topPos + y + 6, color);
    }
}
