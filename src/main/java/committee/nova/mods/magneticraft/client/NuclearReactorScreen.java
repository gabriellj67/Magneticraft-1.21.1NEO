package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorAction;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu.ColumnView;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorControlMode;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorOperatorStatus;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.NuclearReactorActionMessage;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorColumnEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorOperatorMetrics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Progressive operator and engineering views over one server-authoritative reactor state. */
public final class NuclearReactorScreen extends AbstractContainerScreen<NuclearReactorMenu> {
    private static final int GRID_X = 10;
    private static final int GRID_Y = 28;
    private static final int GRID_SIZE = 104;
    private static final int METRIC_X = 130;
    private static final int METRIC_WIDTH = 280;
    private static final int CARD_WIDTH = 66;
    private static final int CARD_HEIGHT = 43;
    private static final int CARD_GAP = 4;
    private static final int SUMMARY_Y = 80;
    private static final int TARGET_BAR_Y = 139;
    private static final int CONTROL_X = 8;
    private static final int CONTROL_Y = 136;
    private static final int CONTROL_SIZE = 20;
    private static final int CONTROL_GAP = 4;

    private int cellSize;
    private int gridWidth;
    private int gridHeight;
    private ReactorColumnCoordinate selectedColumn;
    private ReactorRodGroup selectedGroup = ReactorRodGroup.A;
    private ViewMode viewMode = ViewMode.BASIC;
    private boolean overrideConfirmationArmed;
    private MachineIconButton viewButton;
    private MachineIconButton rodGroupButton;
    private MachineIconButton overrideButton;

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
        MachineScreenLayout.validateInDevelopment(layout());
        overrideButton = null;
        rodGroupButton = null;
        String viewKey = viewMode == ViewMode.BASIC
                ? "gui.magneticraft.reactor.view.engineering"
                : "gui.magneticraft.reactor.view.basic";
        viewButton = addRenderableWidget(new MachineIconButton(
                leftPos + imageWidth - 28,
                topPos + 2,
                20,
                18,
                MachineIcon.MODE,
                Component.translatable(viewKey),
                controlExplanation(viewKey, viewModeTitle()),
                ignored -> toggleView()
        ));
        if (viewMode == ViewMode.BASIC) {
            addBasicButtons();
        } else {
            addEngineeringButtons();
        }
    }

    private void addBasicButtons() {
        addRenderableWidget(controlButton(0, 0, "gui.magneticraft.reactor.action.start", MachineIcon.START,
                NuclearReactorAction.simple(NuclearReactorAction.Type.START)));
        addRenderableWidget(controlButton(1, 0, "gui.magneticraft.reactor.action.stop", MachineIcon.STOP,
                NuclearReactorAction.simple(NuclearReactorAction.Type.STOP)));
        addRenderableWidget(controlButton(2, 0, "gui.magneticraft.reactor.action.scram", MachineIcon.SCRAM,
                NuclearReactorAction.simple(NuclearReactorAction.Type.SCRAM)).dangerous(true));
        addRenderableWidget(controlButton(0, 1, "gui.magneticraft.reactor.action.load", MachineIcon.LOAD,
                ignored -> loadSelectedFuel()));
        addRenderableWidget(controlButton(1, 1, "gui.magneticraft.reactor.action.unload", MachineIcon.UNLOAD,
                ignored -> unloadSelectedFuel()));
        addRenderableWidget(controlButton(2, 1, "gui.magneticraft.reactor.action.target_down", MachineIcon.MINUS,
                ignored -> setTargetPower(-0.05D)));
        addRenderableWidget(controlButton(3, 1, "gui.magneticraft.reactor.action.target_up", MachineIcon.PLUS,
                ignored -> setTargetPower(0.05D)));
        addRenderableWidget(controlButton(4, 1, "gui.magneticraft.reactor.action.reset", MachineIcon.RESET,
                NuclearReactorAction.simple(NuclearReactorAction.Type.RESET)));
    }

    private void addEngineeringButtons() {
        addRenderableWidget(controlButton(0, 0, "gui.magneticraft.reactor.action.start", MachineIcon.START,
                NuclearReactorAction.simple(NuclearReactorAction.Type.START)));
        addRenderableWidget(controlButton(1, 0, "gui.magneticraft.reactor.action.stop", MachineIcon.STOP,
                NuclearReactorAction.simple(NuclearReactorAction.Type.STOP)));
        addRenderableWidget(controlButton(2, 0, "gui.magneticraft.reactor.action.scram", MachineIcon.SCRAM,
                NuclearReactorAction.simple(NuclearReactorAction.Type.SCRAM)).dangerous(true));
        addRenderableWidget(controlButton(0, 1, "gui.magneticraft.reactor.action.rod_down", MachineIcon.DOWN,
                ignored -> setRod(-0.05D)));
        rodGroupButton = controlButton(1, 1, "gui.magneticraft.reactor.action.rod_group", MachineIcon.MODE,
                ignored -> {
                    selectedGroup = ReactorRodGroup.values()[
                            (selectedGroup.ordinal() + 1) % ReactorRodGroup.values().length];
                    refreshRodGroupButton();
                });
        addRenderableWidget(rodGroupButton);
        addRenderableWidget(controlButton(2, 1, "gui.magneticraft.reactor.action.rod_up", MachineIcon.UP,
                ignored -> setRod(0.05D)));
        addRenderableWidget(controlButton(3, 1, "gui.magneticraft.reactor.action.load", MachineIcon.LOAD,
                ignored -> loadSelectedFuel()));
        addRenderableWidget(controlButton(4, 1, "gui.magneticraft.reactor.action.unload", MachineIcon.UNLOAD,
                ignored -> unloadSelectedFuel()));
        addRenderableWidget(controlButton(0, 2, "gui.magneticraft.reactor.action.reset", MachineIcon.RESET,
                NuclearReactorAction.simple(NuclearReactorAction.Type.RESET)));
        addRenderableWidget(controlButton(1, 2, "gui.magneticraft.reactor.action.mode", MachineIcon.MODE,
                ignored -> cycleMode()));
        addRenderableWidget(controlButton(2, 2, "gui.magneticraft.reactor.action.upgrade", MachineIcon.UPGRADE,
                NuclearReactorAction.simple(NuclearReactorAction.Type.INSTALL_UPGRADE)));
        addRenderableWidget(controlButton(3, 2, "gui.magneticraft.reactor.action.target_down", MachineIcon.MINUS,
                ignored -> setTargetPower(-0.05D)));
        addRenderableWidget(controlButton(4, 2, "gui.magneticraft.reactor.action.target_up", MachineIcon.PLUS,
                ignored -> setTargetPower(0.05D)));
        overrideButton = controlButton(0, 3, "gui.magneticraft.reactor.action.override", MachineIcon.SCRAM,
                ignored -> toggleOverride()).dangerous(true);
        addRenderableWidget(overrideButton);
        refreshRodGroupButton();
        refreshOverrideButton();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawSectionHeader(graphics, leftPos, topPos, imageWidth, 22);
        MachineScreenLayout.drawInset(graphics, leftPos + GRID_X - 2, topPos + GRID_Y - 2,
                gridWidth + 4, gridHeight + 4);
        MachineScreenLayout.drawInset(graphics, leftPos + METRIC_X - 4, topPos + 18,
                METRIC_WIDTH + 4, imageHeight - 26);
        if (viewMode == ViewMode.BASIC) {
            drawBasicBackground(graphics);
        }
        drawColumns(graphics);
    }

    private void drawBasicBackground(GuiGraphics graphics) {
        for (int column = 0; column < 4; column++) {
            int x = leftPos + METRIC_X + column * (CARD_WIDTH + CARD_GAP);
            int y = topPos + SUMMARY_Y;
            MachineScreenLayout.drawCard(graphics, x - 2, y - 2, CARD_WIDTH + 2, CARD_HEIGHT + 2);
        }
        int barX = leftPos + METRIC_X;
        int barWidth = METRIC_WIDTH - 4;
        MachineScreenLayout.drawStatusBar(
                graphics,
                barX,
                topPos + TARGET_BAR_Y,
                barWidth,
                8,
                (int) Math.round((barWidth - 4) * menu.targetPowerFraction()),
                MachineScreenLayout.ACCENT,
                false
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, imageWidth - 48),
                8, 6, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable("gui.magneticraft.reactor.core_map"),
                GRID_X, GRID_Y - 12, MachineScreenLayout.TEXT_MUTED, false);
        if (viewMode == ViewMode.BASIC) {
            drawBasicLines(graphics);
        } else {
            drawEngineeringLines(graphics);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

    private void drawBasicLines(GuiGraphics graphics) {
        int x = METRIC_X;
        int color = 0xFFD9DEE3;
        drawLine(graphics, x, 24, "gui.magneticraft.reactor.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName()), color);
        drawLine(graphics, x, 35, "gui.magneticraft.reactor.dimensions",
                Component.literal(menu.width() + "×" + menu.length() + "×" + menu.height()), color);
        drawLine(graphics, x, 46, "gui.magneticraft.reactor.runtime_state",
                Component.translatable("gui.magneticraft.reactor.state."
                        + menu.operatingState().name().toLowerCase(Locale.ROOT)), stateColor());
        drawLine(graphics, x, 57, "gui.magneticraft.reactor.structure",
                Component.translatable("gui.magneticraft.reactor.formed"), 0xFF67D98B);
        drawLine(graphics, x, 68, "gui.magneticraft.reactor.ports",
                Component.literal(menu.ports().size() + "/" + NuclearReactorPortType.values().length),
                menu.ports().size() == NuclearReactorPortType.values().length ? 0xFF67D98B : 0xFFFFB74D);

        ReactorOperatorMetrics metrics = operatorMetrics();
        ReactorOperatorStatus status = ReactorOperatorStatus.from(menu.accidentStage());
        drawCard(graphics, x, SUMMARY_Y, "output", formatPower(menu.totalThermalPowerJoulesPerTick()),
                format(metrics.outputPercent(), "%"), 0xFF67B7FF);
        drawCard(graphics, x + CARD_WIDTH + CARD_GAP, SUMMARY_Y, "efficiency",
                format(metrics.efficiencyPercent(), "%"),
                Component.translatable("gui.magneticraft.reactor.summary.fuel_use").getString(), 0xFFB4E26A);
        drawCard(graphics, x + (CARD_WIDTH + CARD_GAP) * 2, SUMMARY_Y, "safety",
                format(metrics.safetyPercent(), "%"),
                Component.translatable(status.translationKey()).getString(), statusColor(status));
        drawCard(graphics, x + (CARD_WIDTH + CARD_GAP) * 3, SUMMARY_Y, "response",
                format(metrics.responsePercent(), "%"),
                Component.translatable("gui.magneticraft.reactor.mode."
                        + menu.controlMode().name().toLowerCase(Locale.ROOT)).getString(), 0xFFC8A6FF);

        graphics.drawString(font, Component.translatable("gui.magneticraft.reactor.metric.target_power",
                        format(menu.targetPowerFraction() * 100.0D, "%")),
                x, 126, 0xFFD9DEE3, false);
        int y = 151;
        drawMetric(graphics, x, y, "fuel_loaded", menu.loadedFuelCount() + " / "
                + menu.columns().values().stream().filter(column -> column.type().isFuel()).count());
        drawMetric(graphics, x, y += 10, "coolant_live", format(menu.actualCoolantFlow(), " / ")
                + format(menu.requiredCoolantFlow(), " mB/t"));
        drawMetric(graphics, x, y += 10, "station_power",
                menu.stationJoulesAvailable() + " / " + menu.stationJoulesRequired() + " J/t");
        drawMetric(graphics, x, y += 10, "interlocks", interlockSummary());
        drawMetric(graphics, x, y += 10, "operator_status",
                Component.translatable(status.translationKey()).getString());
        NuclearReactorPortType[] ports = NuclearReactorPortType.values();
        int portWidth = METRIC_WIDTH / 2 - 6;
        for (int index = 0; index < ports.length; index++) {
            NuclearReactorPortType port = ports[index];
            int portColor = menu.ports().contains(port) ? 0xFF67D98B : 0xFFFF6B6B;
            Component line = Component.translatable("gui.magneticraft.reactor.port."
                    + port.name().toLowerCase(Locale.ROOT));
            int portX = x + index % 2 * (METRIC_WIDTH / 2);
            int portY = 207 + index / 2 * 10;
            graphics.drawString(font, MachineScreenLayout.fitToWidth(font, line, portWidth),
                    portX, portY, portColor, false);
        }
    }

    private void drawCard(
            GuiGraphics graphics, int x, int y, String metric, String value, String detail, int color
    ) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font,
                        Component.translatable("gui.magneticraft.reactor.summary." + metric), CARD_WIDTH - 5),
                x + 3, y + 3, 0xFFADB6C0, false);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font,
                        Component.literal(value), CARD_WIDTH - 5),
                x + 3, y + 16, color, false);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font,
                        Component.literal(detail), CARD_WIDTH - 5),
                x + 3, y + 29, 0xFFD9DEE3, false);
    }

    private void drawEngineeringLines(GuiGraphics graphics) {
        ReactorLayoutEstimate estimate = menu.estimate();
        int x = METRIC_X;
        int y = 24;
        int color = 0xFFD9DEE3;
        drawLine(graphics, x, y, "gui.magneticraft.reactor.direction",
                Component.translatable("direction.minecraft." + menu.facing().getName()), color);
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.dimensions",
                Component.literal(menu.width() + "×" + menu.length() + "×" + menu.height()), color);
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.runtime_state",
                Component.translatable("gui.magneticraft.reactor.state."
                        + menu.operatingState().name().toLowerCase(Locale.ROOT)), stateColor());
        drawLine(graphics, x, y += 11, "gui.magneticraft.reactor.ports",
                Component.literal(menu.ports().size() + "/" + NuclearReactorPortType.values().length),
                menu.ports().size() == NuclearReactorPortType.values().length ? 0xFF67D98B : 0xFFFFB74D);
        int columnWidth = (METRIC_WIDTH - 8) / 2;
        int rightX = x + METRIC_WIDTH / 2;
        y = 72;
        drawMetric(graphics, x, y, "power_density", format(estimate.powerDensityJoulesPerTick(), " J/t"), columnWidth);
        drawMetric(graphics, x, y += 10, "fuel_energy",
                format(estimate.fuelEnergyJoulesPerColumn() / 1_000_000.0D, " MJ"), columnWidth);
        drawMetric(graphics, x, y += 10, "safety", format(estimate.safetyMarginPercent(), "%"), columnWidth);
        drawMetric(graphics, x, y += 10, "load_following", format(estimate.loadFollowingPercent(), "%"), columnWidth);
        drawMetric(graphics, x, y += 10, "shutdown", format(estimate.shutdownMarginPercent(), "%"), columnWidth);
        drawMetric(graphics, x, y += 10, "thermal", format(menu.totalThermalPowerJoulesPerTick(), " J/t"), columnWidth);
        drawMetric(graphics, x, y += 10, "target_power", format(menu.targetPowerFraction() * 100.0D, "%"), columnWidth);
        drawMetric(graphics, x, y += 10, "temperature", format(menu.hottestTemperatureKelvin(), " K"), columnWidth);
        drawMetric(graphics, x, y += 10, "coolant_live", format(menu.actualCoolantFlow(), " / ")
                + format(menu.requiredCoolantFlow(), " mB/t"), columnWidth);
        drawMetric(graphics, x, y += 10, "station_power",
                menu.stationJoulesAvailable() + " / " + menu.stationJoulesRequired() + " J/t", columnWidth);
        drawMetric(graphics, x, y += 10, "fuel_loaded",
                menu.loadedFuelCount() + " / " + menu.columns().values().stream()
                        .filter(column -> column.type().isFuel()).count(), columnWidth);

        int rightY = 72;
        drawMetric(graphics, rightX, rightY, "rod_group",
                selectedGroup.name() + " " + format(menu.rodInsertion(selectedGroup) * 100.0D, "%"), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "automation",
                Component.translatable("gui.magneticraft.reactor.automation."
                        + menu.automationLevel().name().toLowerCase(Locale.ROOT)).getString(), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "mode",
                Component.translatable("gui.magneticraft.reactor.mode."
                        + menu.controlMode().name().toLowerCase(Locale.ROOT)).getString(), columnWidth);
        Component override = Component.translatable("gui.magneticraft.reactor.metric.override",
                Component.translatable(menu.engineeringOverride()
                        ? "gui.magneticraft.reactor.enabled" : "gui.magneticraft.reactor.disabled"));
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, override, columnWidth),
                rightX, rightY += 10, menu.engineeringOverride() ? 0xFFFF6B6B : 0xFFD9DEE3, false);
        drawMetric(graphics, rightX, rightY += 10, "interlocks", interlockSummary(), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "scram",
                Component.translatable("gui.magneticraft.reactor.scram." + menu.scramReason()).getString(), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "accident_stage",
                Component.translatable("gui.magneticraft.reactor.accident."
                        + menu.accidentStage().name().toLowerCase(Locale.ROOT)).getString(), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "pressure", format(menu.corePressureMegapascals(), " MPa"), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "barriers",
                format(menu.vesselIntegrity() * 100.0D, "% / ")
                        + format(menu.containmentIntegrity() * 100.0D, "%"), columnWidth);
        drawMetric(graphics, rightX, rightY += 10, "radiation",
                format(menu.doseRateMillisievertsPerHour(), " mSv/h"), columnWidth);
        int portY = 178;
        for (NuclearReactorPortType port : NuclearReactorPortType.values()) {
            int portColor = menu.ports().contains(port) ? 0xFF67D98B : 0xFFFF6B6B;
            Component line = Component.translatable("gui.magneticraft.reactor.port."
                    + port.name().toLowerCase(Locale.ROOT));
            graphics.drawString(font, MachineScreenLayout.fitToWidth(font, line, columnWidth),
                    rightX, portY, portColor, false);
            portY += 10;
        }
    }

    private ReactorOperatorMetrics operatorMetrics() {
        return ReactorOperatorMetrics.calculate(
                menu.estimate(),
                menu.totalThermalPowerJoulesPerTick(),
                menu.columns().values().stream().map(ColumnView::type).toList(),
                menu.vesselIntegrity(),
                menu.containmentIntegrity(),
                !menu.interlocks().isEmpty()
        );
    }

    private void drawLine(
            GuiGraphics graphics, int x, int y, String labelKey, Component value, int color
    ) {
        Component label = Component.translatable(labelKey, value);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, label, METRIC_WIDTH - 8),
                x, y, color, false);
    }

    private void drawMetric(GuiGraphics graphics, int x, int y, String metric, String value) {
        drawMetric(graphics, x, y, metric, value, METRIC_WIDTH - 8);
    }

    private void drawMetric(
            GuiGraphics graphics, int x, int y, String metric, String value, int maximumWidth
    ) {
        Component label = Component.translatable("gui.magneticraft.reactor.metric." + metric, value);
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, label, maximumWidth),
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
        NuclearReactorMenu.FuelView fuel = menu.fuelView(coordinate);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(columnTranslation(column.type())));
        lines.add(Component.translatable("gui.magneticraft.reactor.column.position", coordinate.x(), coordinate.z()));
        if (column.type().isFuel()) {
            lines.add(Component.translatable(fuel.loaded()
                    ? "gui.magneticraft.reactor.column.loaded" : "gui.magneticraft.reactor.column.empty"));
        }
        if (viewMode == ViewMode.ENGINEERING && column.type().isFuel()) {
            ReactorColumnEstimate estimate = column.estimate();
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
                    format(estimate.hotspotFactor(), "×")));
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

    private MachineIconButton controlButton(
            int column,
            int row,
            String key,
            MachineIcon icon,
            NuclearReactorAction action
    ) {
        return controlButton(column, row, key, icon, ignored -> send(action));
    }

    private MachineIconButton controlButton(
            int column,
            int row,
            String key,
            MachineIcon icon,
            MachineIconButton.OnPress press
    ) {
        Component action = Component.translatable(key);
        return new MachineIconButton(
                leftPos + CONTROL_X + column * (CONTROL_SIZE + CONTROL_GAP),
                topPos + CONTROL_Y + row * (CONTROL_SIZE + CONTROL_GAP),
                CONTROL_SIZE,
                CONTROL_SIZE,
                icon,
                action,
                action,
                press
        );
    }

    private void toggleView() {
        viewMode = viewMode == ViewMode.BASIC ? ViewMode.ENGINEERING : ViewMode.BASIC;
        overrideConfirmationArmed = false;
        clearWidgets();
        init();
    }

    private void loadSelectedFuel() {
        send(new NuclearReactorAction(NuclearReactorAction.Type.LOAD_FUEL_FROM_HAND,
                null, null, selectedColumn, 0, false));
    }

    private void unloadSelectedFuel() {
        send(new NuclearReactorAction(NuclearReactorAction.Type.UNLOAD_FUEL,
                null, null, selectedColumn, 0, false));
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
                .map(interlock -> Component.translatable("gui.magneticraft.reactor.interlock."
                        + interlock.name().toLowerCase(Locale.ROOT)).getString())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private void toggleOverride() {
        if (overrideButton == null) {
            return;
        }
        if (menu.engineeringOverride()) {
            overrideConfirmationArmed = false;
            refreshOverrideButton();
            send(new NuclearReactorAction(
                    NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 0, false));
            return;
        }
        boolean confirmed = overrideConfirmationArmed;
        overrideConfirmationArmed = !confirmed;
        refreshOverrideButton();
        send(new NuclearReactorAction(
                NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 1, confirmed));
    }

    private void refreshRodGroupButton() {
        if (rodGroupButton == null) {
            return;
        }
        Component group = Component.literal(selectedGroup.name());
        rodGroupButton.badge(selectedGroup.name());
        rodGroupButton.updateExplanation(
                Component.translatable("gui.magneticraft.reactor.action.rod_group"),
                controlExplanation("gui.magneticraft.reactor.action.rod_group", group)
        );
    }

    private void refreshOverrideButton() {
        if (overrideButton == null) {
            return;
        }
        String key = overrideConfirmationArmed
                ? "gui.magneticraft.reactor.action.confirm_override"
                : "gui.magneticraft.reactor.action.override";
        overrideButton.badge(overrideConfirmationArmed ? "!" : "");
        overrideButton.selected(menu.engineeringOverride());
        overrideButton.updateExplanation(
                Component.translatable(key),
                controlExplanation(
                        key,
                        Component.translatable(menu.engineeringOverride()
                                ? "gui.magneticraft.control.enabled"
                                : "gui.magneticraft.control.disabled")
                )
        );
    }

    private Component viewModeTitle() {
        return Component.translatable(viewMode == ViewMode.BASIC
                ? "gui.magneticraft.reactor.view.basic"
                : "gui.magneticraft.reactor.view.engineering");
    }

    private static Component controlExplanation(String actionKey, Component currentState) {
        return Component.translatable(actionKey)
                .append("\n")
                .append(Component.translatable("gui.magneticraft.control.current_state", currentState));
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight, gridWidth, gridHeight, viewMode == ViewMode.ENGINEERING);
    }

    static MachineScreenBounds.Layout layout(
            int imageWidth,
            int imageHeight,
            int gridWidth,
            int gridHeight,
            boolean engineering
    ) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "nuclear_reactor/" + (engineering ? "engineering" : "basic"),
                imageWidth,
                imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(0, 0, imageWidth, 22))
                .child("view", new LegacyMachineGuiLayout.Rect(imageWidth - 28, 2, 20, 18),
                        "header", null, 0)
                .element("core_grid", new LegacyMachineGuiLayout.Rect(
                        GRID_X - 2, GRID_Y - 2, gridWidth + 4, gridHeight + 4
                ))
                .element("metrics", new LegacyMachineGuiLayout.Rect(
                        METRIC_X - 4, 18, METRIC_WIDTH + 4, imageHeight - 26
                ));
        int[] rowCounts = engineering ? new int[]{3, 5, 5, 1} : new int[]{3, 5};
        for (int row = 0; row < rowCounts.length; row++) {
            for (int column = 0; column < rowCounts[row]; column++) {
                builder.element(
                        "control_" + row + "_" + column,
                        new LegacyMachineGuiLayout.Rect(
                                CONTROL_X + column * (CONTROL_SIZE + CONTROL_GAP),
                                CONTROL_Y + row * (CONTROL_SIZE + CONTROL_GAP),
                                CONTROL_SIZE,
                                CONTROL_SIZE
                        ),
                        "controls",
                        CONTROL_GAP
                );
            }
        }
        return builder.build();
    }

    private void send(NuclearReactorAction action) {
        if (action != null) {
            ModNetwork.controlNuclearReactor(new NuclearReactorActionMessage(menu.position(), action));
        }
    }

    private int stateColor() {
        ReactorOperatorStatus status = ReactorOperatorStatus.from(menu.accidentStage());
        if (status != ReactorOperatorStatus.NORMAL) {
            return statusColor(status);
        }
        return switch (menu.operatingState()) {
            case RUNNING -> 0xFF67D98B;
            case STARTUP, DECAY_HEAT -> 0xFFFFB74D;
            case SCRAMMED -> 0xFFFF6B6B;
            case SHUTDOWN -> 0xFFD9DEE3;
        };
    }

    private static int statusColor(ReactorOperatorStatus status) {
        return switch (status) {
            case NORMAL -> 0xFF67D98B;
            case COOLING_WARNING -> 0xFFFFB74D;
            case CORE_DAMAGE -> 0xFFFF6B6B;
            case MELTDOWN_OR_RELEASE -> 0xFFFF3B30;
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

    private static String formatPower(double joulesPerTick) {
        if (joulesPerTick >= 1_000_000.0D) {
            return format(joulesPerTick / 1_000_000.0D, " MJ/t");
        }
        if (joulesPerTick >= 1_000.0D) {
            return format(joulesPerTick / 1_000.0D, " kJ/t");
        }
        return format(joulesPerTick, " J/t");
    }

    private enum ViewMode {
        BASIC,
        ENGINEERING
    }
}
