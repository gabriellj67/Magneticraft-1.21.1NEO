package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu.ColumnView;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorAction;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorControlMode;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.NuclearReactorActionMessage;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorColumnEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Live control and per-column status screen for the variable-volume pressurized-water reactor. */
public final class NuclearReactorScreen extends AbstractContainerScreen<NuclearReactorMenu> {
    private static final int GRID_X = 10;
    private static final int GRID_Y = 28;
    private static final int GRID_SIZE = 112;
    private static final int METRIC_X = 130;
    private static final int METRIC_WIDTH = 170;

    private int cellSize;
    private int gridWidth;
    private int gridHeight;
    private ReactorColumnCoordinate selectedColumn;
    private ReactorRodGroup selectedGroup = ReactorRodGroup.A;
    private boolean overrideConfirmationArmed;
    private Button overrideButton;

    public NuclearReactorScreen(NuclearReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = NuclearReactorMenu.IMAGE_WIDTH;
        imageHeight = NuclearReactorMenu.IMAGE_HEIGHT;
        cellSize = Math.max(8, Math.min(GRID_SIZE / menu.activeWidth(), GRID_SIZE / menu.activeLength()));
        gridWidth = cellSize * menu.activeWidth();
        gridHeight = cellSize * menu.activeLength();
        selectedColumn = menu.columns().entrySet().stream()
                .filter(entry -> entry.getValue().type().isFuel())
                .map(java.util.Map.Entry::getKey).findFirst().orElse(new ReactorColumnCoordinate(0, 0));
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 8;
        addRenderableWidget(button(x, topPos + 148, 34, "gui.magneticraft.reactor.action.start",
                NuclearReactorAction.simple(NuclearReactorAction.Type.START)));
        addRenderableWidget(button(x + 36, topPos + 148, 34, "gui.magneticraft.reactor.action.stop",
                NuclearReactorAction.simple(NuclearReactorAction.Type.STOP)));
        addRenderableWidget(button(x + 72, topPos + 148, 44, "gui.magneticraft.reactor.action.scram",
                NuclearReactorAction.simple(NuclearReactorAction.Type.SCRAM)));
        addRenderableWidget(button(x, topPos + 170, 28, "gui.magneticraft.reactor.action.rod_down",
                null, ignored -> setRod(-0.05D)));
        addRenderableWidget(button(x + 30, topPos + 170, 52, "gui.magneticraft.reactor.action.rod_group",
                null, ignored -> selectedGroup = ReactorRodGroup.values()[
                        (selectedGroup.ordinal() + 1) % ReactorRodGroup.values().length]));
        addRenderableWidget(button(x + 84, topPos + 170, 28, "gui.magneticraft.reactor.action.rod_up",
                null, ignored -> setRod(0.05D)));
        addRenderableWidget(button(x, topPos + 192, 54, "gui.magneticraft.reactor.action.load",
                null, ignored -> send(new NuclearReactorAction(
                        NuclearReactorAction.Type.LOAD_FUEL_FROM_HAND, null, null,
                        selectedColumn, 0, false))));
        addRenderableWidget(button(x + 56, topPos + 192, 56, "gui.magneticraft.reactor.action.unload",
                null, ignored -> send(new NuclearReactorAction(
                        NuclearReactorAction.Type.UNLOAD_FUEL, null, null,
                        selectedColumn, 0, false))));
        addRenderableWidget(button(x, topPos + 214, 34, "gui.magneticraft.reactor.action.reset",
                NuclearReactorAction.simple(NuclearReactorAction.Type.RESET)));
        addRenderableWidget(button(x + 36, topPos + 214, 38, "gui.magneticraft.reactor.action.mode",
                null, ignored -> cycleMode()));
        addRenderableWidget(button(x + 76, topPos + 214, 36, "gui.magneticraft.reactor.action.upgrade",
                NuclearReactorAction.simple(NuclearReactorAction.Type.INSTALL_UPGRADE)));
        addRenderableWidget(button(x, topPos + 236, 30, "gui.magneticraft.reactor.action.target_down",
                null, ignored -> setTargetPower(-0.05D)));
        addRenderableWidget(button(x + 32, topPos + 236, 30, "gui.magneticraft.reactor.action.target_up",
                null, ignored -> setTargetPower(0.05D)));
        overrideButton = button(x + 64, topPos + 236, 48, "gui.magneticraft.reactor.action.override",
                null, ignored -> toggleOverride());
        addRenderableWidget(overrideButton);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawInset(graphics, leftPos + GRID_X - 2, topPos + GRID_Y - 2,
                gridWidth + 4, gridHeight + 4);
        MachineScreenLayout.drawInset(graphics, leftPos + METRIC_X - 4, topPos + 18,
                METRIC_WIDTH + 4, 298);
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
            if (coordinate.equals(selectedColumn)) {
                graphics.fill(x, y, x + cellSize - 1, y + 1, 0xFFFFFFFF);
                graphics.fill(x, y + cellSize - 2, x + cellSize - 1, y + cellSize - 1, 0xFFFFFFFF);
                graphics.fill(x, y, x + 1, y + cellSize - 1, 0xFFFFFFFF);
                graphics.fill(x + cellSize - 2, y, x + cellSize - 1, y + cellSize - 1, 0xFFFFFFFF);
            }
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
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.runtime_state",
                Component.translatable("gui.magneticraft.reactor.state." +
                        menu.operatingState().name().toLowerCase(Locale.ROOT)), stateColor());
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.ports",
                Component.literal(menu.ports().size() + "/" + NuclearReactorPortType.values().length),
                menu.ports().size() == NuclearReactorPortType.values().length ? 0xFF67D98B : 0xFFFFB74D);
        y += 13;
        drawMetric(graphics, x, y, "power_density", format(estimate.powerDensityJoulesPerTick(), " J/t"));
        drawMetric(graphics, x, y += 10, "fuel_energy", format(estimate.fuelEnergyJoulesPerColumn() / 1_000_000.0D, " MJ"));
        drawMetric(graphics, x, y += 10, "safety", format(estimate.safetyMarginPercent(), "%"));
        drawMetric(graphics, x, y += 10, "load_following", format(estimate.loadFollowingPercent(), "%"));
        drawMetric(graphics, x, y += 10, "shutdown", format(estimate.shutdownMarginPercent(), "%"));
        drawMetric(graphics, x, y += 10, "thermal", format(menu.totalThermalPowerJoulesPerTick(), " J/t"));
        drawMetric(graphics, x, y += 10, "target_power", format(menu.targetPowerFraction() * 100.0D, "%"));
        drawMetric(graphics, x, y += 10, "temperature", format(menu.hottestTemperatureKelvin(), " K"));
        drawMetric(graphics, x, y += 10, "coolant_live", format(menu.actualCoolantFlow(), " / ")
                + format(menu.requiredCoolantFlow(), " mB/t"));
        drawMetric(graphics, x, y += 10, "station_power",
                menu.stationJoulesAvailable() + " / " + menu.stationJoulesRequired() + " J/t");
        drawMetric(graphics, x, y += 10, "fuel_loaded",
                menu.loadedFuelCount() + " / " + menu.columns().values().stream()
                        .filter(column -> column.type().isFuel()).count());
        drawMetric(graphics, x, y += 10, "rod_group",
                selectedGroup.name() + " " + format(menu.rodInsertion(selectedGroup) * 100.0D, "%"));
        drawMetric(graphics, x, y += 10, "automation",
                Component.translatable("gui.magneticraft.reactor.automation." +
                        menu.automationLevel().name().toLowerCase(Locale.ROOT)).getString());
        drawMetric(graphics, x, y += 10, "mode",
                Component.translatable("gui.magneticraft.reactor.mode." +
                        menu.controlMode().name().toLowerCase(Locale.ROOT)).getString());
        y += 10;
        Component override = Component.translatable("gui.magneticraft.reactor.metric.override",
                Component.translatable(menu.engineeringOverride()
                        ? "gui.magneticraft.reactor.enabled" : "gui.magneticraft.reactor.disabled"));
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, override, METRIC_WIDTH - 8),
                x, y, menu.engineeringOverride() ? 0xFFFF6B6B : 0xFFD9DEE3, false);
        drawMetric(graphics, x, y += 10, "interlocks", interlockSummary());
        drawMetric(graphics, x, y += 10, "scram",
                Component.translatable("gui.magneticraft.reactor.scram." + menu.scramReason()).getString());
        y += 12;
        for (NuclearReactorPortType port : NuclearReactorPortType.values()) {
            int portColor = menu.ports().contains(port) ? 0xFF67D98B : 0xFFFF6B6B;
            Component line = Component.translatable("gui.magneticraft.reactor.port." +
                    port.name().toLowerCase(Locale.ROOT));
            graphics.drawString(font, MachineScreenLayout.fitToWidth(font, line, METRIC_WIDTH - 8),
                    x, y, portColor, false);
            y += 9;
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
        NuclearReactorMenu.FuelView fuel = menu.fuelView(coordinate);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(columnTranslation(column.type())));
        lines.add(Component.translatable("gui.magneticraft.reactor.column.position", coordinate.x(), coordinate.z()));
        if (column.type().isFuel()) {
            lines.add(Component.translatable(fuel.loaded()
                    ? "gui.magneticraft.reactor.column.loaded" : "gui.magneticraft.reactor.column.empty"));
            if (fuel.loaded()) {
                lines.add(Component.translatable("item.magneticraft.fuel_assembly.burnup",
                        Math.round(fuel.burnupFraction() * 100.0D)));
                lines.add(Component.translatable("gui.magneticraft.reactor.column.poison",
                        Math.round(fuel.poisonFraction() * 100.0D)));
                lines.add(Component.translatable("item.magneticraft.fuel_assembly.temperature",
                        Math.round(fuel.temperatureKelvin())));
                lines.add(Component.translatable("item.magneticraft.fuel_assembly.cladding",
                        Math.round(fuel.claddingIntegrity() * 100.0D)));
            }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relativeX = (int) mouseX - leftPos - GRID_X;
        int relativeY = (int) mouseY - topPos - GRID_Y;
        if (button == 0 && relativeX >= 0 && relativeY >= 0
                && relativeX < gridWidth && relativeY < gridHeight) {
            ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(
                    relativeX / cellSize, relativeY / cellSize);
            if (menu.columns().containsKey(coordinate)) {
                selectedColumn = coordinate;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Button button(int x, int y, int width, String key, NuclearReactorAction action) {
        return button(x, y, width, key, action, ignored -> send(action));
    }

    private Button button(
            int x, int y, int width, String key, NuclearReactorAction action,
            Button.OnPress press
    ) {
        return Button.builder(Component.translatable(key), press).bounds(x, y, width, 20).build();
    }

    private void setRod(double delta) {
        int value = (int) Math.round(Math.max(0.0D, Math.min(1.0D,
                menu.rodInsertion(selectedGroup) + delta)) * 1000.0D);
        send(new NuclearReactorAction(
                NuclearReactorAction.Type.SET_ROD_GROUP, selectedGroup, null, null, value, false));
    }

    private void cycleMode() {
        ReactorControlMode[] modes = ReactorControlMode.values();
        ReactorControlMode mode = modes[(menu.controlMode().ordinal() + 1) % modes.length];
        send(new NuclearReactorAction(NuclearReactorAction.Type.SET_MODE, null, mode, null, 0, false));
    }

    private void setTargetPower(double delta) {
        int value = (int) Math.round(Math.max(0.0D, Math.min(1.0D,
                menu.targetPowerFraction() + delta)) * 1000.0D);
        send(new NuclearReactorAction(
                NuclearReactorAction.Type.SET_TARGET_POWER, null, null, null, value, false));
    }

    private String interlockSummary() {
        if (menu.interlocks().isEmpty()) {
            return Component.translatable("gui.magneticraft.reactor.interlock.none").getString();
        }
        return menu.interlocks().stream()
                .map(interlock -> Component.translatable("gui.magneticraft.reactor.interlock." +
                        interlock.name().toLowerCase(Locale.ROOT)).getString())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private void toggleOverride() {
        if (menu.engineeringOverride()) {
            overrideConfirmationArmed = false;
            overrideButton.setMessage(Component.translatable("gui.magneticraft.reactor.action.override"));
            send(new NuclearReactorAction(
                    NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 0, false));
            return;
        }
        boolean confirmed = overrideConfirmationArmed;
        overrideConfirmationArmed = !confirmed;
        overrideButton.setMessage(Component.translatable(overrideConfirmationArmed
                ? "gui.magneticraft.reactor.action.confirm_override"
                : "gui.magneticraft.reactor.action.override"));
        send(new NuclearReactorAction(
                NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 1, confirmed));
    }

    private void send(NuclearReactorAction action) {
        if (action != null) {
            ModNetwork.controlNuclearReactor(new NuclearReactorActionMessage(menu.position(), action));
        }
    }

    private int stateColor() {
        return switch (menu.operatingState()) {
            case RUNNING -> 0xFF67D98B;
            case STARTUP, DECAY_HEAT -> 0xFFFFB74D;
            case SCRAMMED -> 0xFFFF6B6B;
            case SHUTDOWN -> 0xFFD9DEE3;
        };
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
