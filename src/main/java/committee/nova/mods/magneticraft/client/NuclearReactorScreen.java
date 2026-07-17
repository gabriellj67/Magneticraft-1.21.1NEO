package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu.ColumnView;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorColumnEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only layout preview for the variable-volume pressurized-water reactor. */
public final class NuclearReactorScreen extends AbstractContainerScreen<NuclearReactorMenu> {
    private static final int GRID_X = 10;
    private static final int GRID_Y = 28;
    private static final int GRID_SIZE = 112;
    private static final int METRIC_X = 130;
    private static final int METRIC_WIDTH = 170;

    private int cellSize;
    private int gridWidth;
    private int gridHeight;

    public NuclearReactorScreen(NuclearReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = NuclearReactorMenu.IMAGE_WIDTH;
        imageHeight = NuclearReactorMenu.IMAGE_HEIGHT;
        cellSize = Math.max(8, Math.min(GRID_SIZE / menu.activeWidth(), GRID_SIZE / menu.activeLength()));
        gridWidth = cellSize * menu.activeWidth();
        gridHeight = cellSize * menu.activeLength();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawInset(graphics, leftPos + GRID_X - 2, topPos + GRID_Y - 2,
                gridWidth + 4, gridHeight + 4);
        MachineScreenLayout.drawInset(graphics, leftPos + METRIC_X - 4, topPos + 18,
                METRIC_WIDTH + 4, 164);
        drawColumns(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, 170), 8, 6, 0xFF303030, false);
        Component state = Component.translatable("gui.magneticraft.reactor.formed");
        graphics.drawString(font, state, imageWidth - 8 - font.width(state), 6, 0xFF16793A, false);

        graphics.drawString(font, Component.translatable("gui.magneticraft.reactor.core_map"),
                GRID_X, GRID_Y - 12, 0xFF404040, false);
        drawMetricLines(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderColumnTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawColumns(GuiGraphics graphics) {
        for (var entry : menu.columns().entrySet()) {
            ReactorColumnCoordinate coordinate = entry.getKey();
            ColumnView column = entry.getValue();
            int x = leftPos + GRID_X + coordinate.x() * cellSize;
            int y = topPos + GRID_Y + coordinate.z() * cellSize;
            graphics.fill(x, y, x + cellSize - 1, y + cellSize - 1, columnColor(column));
            if (column.type().isFuel() && column.estimate().hotspotFactor() > 1.15D) {
                graphics.fill(x + 1, y + 1, x + cellSize - 2, y + 2, 0xFFFFD54F);
            }
        }
    }

    private void drawMetricLines(GuiGraphics graphics) {
        ReactorLayoutEstimate estimate = menu.estimate();
        int x = METRIC_X;
        int y = 24;
        int color = 0xFFD9DEE3;
        drawLine(graphics, x, y, "gui.magneticraft.reactor.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName()), color);
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.dimensions",
                Component.literal(menu.width() + "\u00d7" + menu.length() + "\u00d7" + menu.height()), color);
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.ports",
                Component.literal(menu.ports().size() + "/" + NuclearReactorPortType.values().length),
                menu.ports().size() == NuclearReactorPortType.values().length ? 0xFF67D98B : 0xFFFFB74D);
        y += 15;
        drawMetric(graphics, x, y, "power_density", format(estimate.powerDensityJoulesPerTick(), " J/t"));
        drawMetric(graphics, x, y += 12, "fuel_energy", format(estimate.fuelEnergyJoulesPerColumn() / 1_000_000.0D, " MJ"));
        drawMetric(graphics, x, y += 12, "safety", format(estimate.safetyMarginPercent(), "%"));
        drawMetric(graphics, x, y += 12, "load_following", format(estimate.loadFollowingPercent(), "%"));
        drawMetric(graphics, x, y += 12, "shutdown", format(estimate.shutdownMarginPercent(), "%"));
        drawMetric(graphics, x, y += 12, "coolant", format(estimate.requiredCoolantFlowMilliBucketsPerTick(), " mB/t"));
        y += 15;
        for (NuclearReactorPortType port : NuclearReactorPortType.values()) {
            int portColor = menu.ports().contains(port) ? 0xFF67D98B : 0xFFFF6B6B;
            Component line = Component.translatable("gui.magneticraft.reactor.port." +
                    port.name().toLowerCase(Locale.ROOT));
            graphics.drawString(font, MachineScreenLayout.fitToWidth(font, line, METRIC_WIDTH - 8),
                    x, y, portColor, false);
            y += 10;
        }
    }

    private void drawLine(
            GuiGraphics graphics, int x, int y, String labelKey, Component value, int color
    ) {
        Component label = Component.translatable(labelKey, value);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, label, METRIC_WIDTH - 8),
                x, y, color, false);
    }

    private void drawMetric(GuiGraphics graphics, int x, int y, String metric, String value) {
        Component label = Component.translatable("gui.magneticraft.reactor.metric." + metric, value);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, label, METRIC_WIDTH - 8),
                x, y, 0xFFD9DEE3, false);
    }

    private void renderColumnTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int relativeX = mouseX - leftPos - GRID_X;
        int relativeY = mouseY - topPos - GRID_Y;
        if (relativeX < 0 || relativeY < 0 || relativeX >= gridWidth || relativeY >= gridHeight) {
            return;
        }
        ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(relativeX / cellSize, relativeY / cellSize);
        ColumnView column = menu.columns().get(coordinate);
        if (column == null) {
            return;
        }
        ReactorColumnEstimate estimate = column.estimate();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(columnTranslation(column.type())));
        lines.add(Component.translatable("gui.magneticraft.reactor.column.position", coordinate.x(), coordinate.z()));
        if (column.type().isFuel()) {
            lines.add(Component.translatable("gui.magneticraft.reactor.column.power",
                    format(estimate.thermalPowerJoulesPerTick(), " J/t")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.heat",
                    format(estimate.heatFraction() * 100.0D, "%")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.hotspot",
                    format(estimate.hotspotFactor(), "\u00d7")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.coupling",
                    format(estimate.fuelCoupling(), "")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.cooling",
                    format(estimate.cooling(), "")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.control",
                    format(estimate.shutdownWorth(), "")));
            lines.add(Component.translatable("gui.magneticraft.reactor.column.instrumentation",
                    format(estimate.instrumentationCoverage(), "")));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static String columnTranslation(NuclearReactorColumnType type) {
        return "block.magneticraft.reactor_" + type.name().toLowerCase(Locale.ROOT);
    }

    private static int columnColor(ColumnView column) {
        if (column.type().isFuel()) {
            double heat = Math.max(0.0D, Math.min(1.0D, column.estimate().heatFraction() * 8.0D));
            int red = (int) (72 + heat * 183);
            int green = (int) (190 - heat * 105);
            return 0xFF000000 | red << 16 | green << 8 | 48;
        }
        return switch (column.type()) {
            case CONTROL_ROD_A -> 0xFF4A67B3;
            case CONTROL_ROD_B -> 0xFF8052B3;
            case CONTROL_ROD_C -> 0xFFB34A75;
            case CONTROL_ROD_D -> 0xFFB3753F;
            case COOLANT_CHANNEL -> 0xFF3F9DB3;
            case INSTRUMENTATION -> 0xFFE2C14C;
            case REFLECTOR -> 0xFF9CA6AD;
            default -> 0xFF626B70;
        };
    }

    private static String format(double value, String suffix) {
        return String.format(Locale.ROOT, "%.1f%s", value, suffix);
    }
}
