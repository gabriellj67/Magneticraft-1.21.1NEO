package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.Rect;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.ElectricalReading;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared read-only electrical telemetry panel used by every machine screen. */
final class ElectricalStatePanel {
    private static final int TEXT_COLOR = MachineScreenLayout.TEXT_PRIMARY;
    private static final int MUTED_COLOR = MachineScreenLayout.TEXT_MUTED;
    private static final int WARNING_COLOR = MachineScreenLayout.WARNING;
    private static final int LINE_HEIGHT = 9;
    private static final int MAX_TERMINALS = 2;

    private ElectricalStatePanel() {
    }

    static void render(
            GuiGraphics graphics,
            Font font,
            int left,
            int top,
            Rect bounds,
            BlockPos position,
            long storedJoules,
            long ratedCapacityJoules
    ) {
        MachineScreenLayout.drawInset(
                graphics,
                left + bounds.x(),
                top + bounds.y(),
                bounds.width(),
                bounds.height()
        );
        int textLeft = left + bounds.x() + 7;
        int textWidth = bounds.width() - 11;
        int y = top + bounds.y() + 5;
        drawLine(graphics, font, Component.translatable("gui.magneticraft.electrical.title"),
                textLeft, y, textWidth, TEXT_COLOR);
        y += 11;

        List<ElectricalReading> readings = readings(position);
        if (readings.isEmpty()) {
            drawLine(graphics, font, Component.translatable("gui.magneticraft.electrical.unavailable"),
                    textLeft, y, textWidth, WARNING_COLOR);
            return;
        }

        int displayed = Math.min(MAX_TERMINALS, readings.size());
        for (int index = 0; index < displayed; index++) {
            ElectricalReading reading = readings.get(index);
            TierDisplay tier = tier(reading.tierId());
            graphics.fill(
                    left + bounds.x() + 3,
                    y,
                    left + bounds.x() + 5,
                    y + 34,
                    0xFF000000 | tier.colorRgb()
            );
            drawLine(graphics, font, Component.translatable(
                            "gui.magneticraft.electrical.terminal",
                            reading.terminalId().getPath(),
                            tier.name()
                    ), textLeft, y, textWidth, tier.colorRgb() | 0xFF000000);
            drawLine(graphics, font, Component.translatable(
                            "gui.magneticraft.electrical.voltage_current",
                            format(reading.voltageVolts()),
                            format(reading.currentAmps())
                    ), textLeft, y + LINE_HEIGHT, textWidth, TEXT_COLOR);
            drawLine(graphics, font, Component.translatable(
                            "gui.magneticraft.electrical.throughput",
                            format(reading.joulesPerTick()),
                            format(reading.powerWatts())
                    ), textLeft, y + LINE_HEIGHT * 2, textWidth, TEXT_COLOR);
            drawLine(graphics, font, Component.translatable(
                            "gui.magneticraft.electrical.load_stress",
                            percent(reading.loadRatio()),
                            percent(reading.thermalStress())
                    ), textLeft, y + LINE_HEIGHT * 3, textWidth,
                    reading.faultKind() == FaultKind.NONE ? MUTED_COLOR : WARNING_COLOR);
            y += 38;
        }

        ElectricalReading primary = readings.get(0);
        long shownStored = ratedCapacityJoules > 0L
                ? Math.max(0L, storedJoules)
                : Math.round(primary.storedJoules());
        long shownCapacity = ratedCapacityJoules > 0L
                ? ratedCapacityJoules
                : Math.round(primary.capacityJoules());
        drawLine(graphics, font, Component.translatable(
                        "gui.magneticraft.electrical.storage",
                        shownStored,
                        shownCapacity
                ), textLeft, y, textWidth, TEXT_COLOR);
        y += LINE_HEIGHT;
        FaultKind fault = readings.stream()
                .map(ElectricalReading::faultKind)
                .filter(value -> value != FaultKind.NONE)
                .findFirst()
                .orElse(FaultKind.NONE);
        drawLine(graphics, font, Component.translatable(
                        "gui.magneticraft.electrical.state",
                        Component.translatable(fault.translationKey()),
                        Component.translatable(primary.flowDirection().translationKey())
                ), textLeft, y, textWidth, fault == FaultKind.NONE ? MUTED_COLOR : WARNING_COLOR);
    }

    static void renderTooltip(
            GuiGraphics graphics,
            Font font,
            int mouseX,
            int mouseY,
            int left,
            int top,
            Rect bounds,
            BlockPos position,
            long storedJoules,
            long ratedCapacityJoules
    ) {
        if (mouseX < left + bounds.x() || mouseX >= left + bounds.right()
                || mouseY < top + bounds.y() || mouseY >= top + bounds.bottom()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.magneticraft.electrical.title"));
        List<ElectricalReading> readings = readings(position);
        for (int index = 0; index < readings.size(); index++) {
            ElectricalReading reading = readings.get(index);
            TierDisplay tier = tier(reading.tierId());
            lines.add(Component.translatable(
                    "gui.magneticraft.electrical.terminal",
                    reading.terminalId(),
                    tier.name()
            ));
            lines.add(Component.translatable(
                    "gui.magneticraft.electrical.point_tooltip",
                    format(reading.voltageVolts()),
                    format(reading.chargeCoulombsPerTick()),
                    format(reading.currentAmps()),
                    format(reading.joulesPerTick()),
                    format(reading.powerWatts())
            ));
            double shownStored = index == 0 && ratedCapacityJoules > 0L
                    ? Math.max(0L, storedJoules)
                    : reading.storedJoules();
            double shownCapacity = index == 0 && ratedCapacityJoules > 0L
                    ? ratedCapacityJoules
                    : reading.capacityJoules();
            lines.add(Component.translatable(
                    "gui.magneticraft.electrical.node_tooltip",
                    format(shownStored),
                    format(shownCapacity),
                    percent(reading.loadRatio()),
                    percent(reading.thermalStress())
            ));
            lines.add(Component.translatable(
                    "gui.magneticraft.electrical.state",
                    Component.translatable(reading.faultKind().translationKey()),
                    Component.translatable(reading.flowDirection().translationKey())
            ));
        }
        if (readings.isEmpty()) {
            lines.add(Component.translatable("gui.magneticraft.electrical.unavailable"));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    static List<ElectricalReading> readings(BlockPos position) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return List.of();
        }
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) {
            return List.of();
        }
        BlockEntity blockEntity = chunk.getBlockEntity(position);
        if (!(blockEntity instanceof MachineBlockEntity machine)) {
            return List.of();
        }
        return machine.electricalTerminals().stream()
                .map(terminal -> terminal.displayReading())
                .limit(MAX_TERMINALS)
                .toList();
    }

    private static TierDisplay tier(ResourceLocation id) {
        return ClientVoltageTierRegistry.current().tier(id)
                .map(ElectricalStatePanel::tier)
                .orElseGet(() -> new TierDisplay(Component.literal(id.toString()), 0x808080));
    }

    private static TierDisplay tier(VoltageTierDisplay tier) {
        return new TierDisplay(Component.translatable(tier.translationKey()), tier.colorRgb());
    }

    private static void drawLine(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int width,
            int color
    ) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, text, width), x, y, color, false);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f", value * 100.0D);
    }

    private record TierDisplay(Component name, int colorRgb) {
    }
}
