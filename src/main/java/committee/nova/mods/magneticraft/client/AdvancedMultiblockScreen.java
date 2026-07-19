package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.StatusBar;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockPortLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Legacy-sized status screen shared by all advanced multiblock controllers.
 */
public final class AdvancedMultiblockScreen extends AbstractContainerScreen<AdvancedMultiblockMenu> {
    private final int baseImageWidth;
    private final LegacyMachineGuiLayout.Rect electricalPanel;

    public AdvancedMultiblockScreen(AdvancedMultiblockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        baseImageWidth = menu.imageWidth();
        electricalPanel = menu.definition().usesElectricity()
                ? electricalPanelBounds(menu.definition())
                : null;
        LegacyMachineGuiLayout.Size baseSize = new LegacyMachineGuiLayout.Size(baseImageWidth, menu.imageHeight());
        LegacyMachineGuiLayout.Size size = electricalPanel == null
                ? baseSize
                : LegacyMachineGuiLayout.withElectricalPanel(baseSize);
        imageWidth = size.width();
        imageHeight = size.height();
        inventoryLabelX = LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT;
        inventoryLabelY = menu.playerInventoryTop() - 12;
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, baseImageWidth);
        int contentBottom = menu.definition() == MultiblockDefinition.SHELVING_UNIT ? 112 : 76;
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 14,
                baseImageWidth - 10, contentBottom - 14);
        MachineScreenLayout.drawCard(
                graphics,
                leftPos + 5,
                topPos + menu.playerInventoryTop() - 2,
                baseImageWidth - 10,
                imageHeight - menu.playerInventoryTop()
        );
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawStatusBars(graphics, statusBars());
        if (electricalPanel != null) {
            ElectricalStatePanel.render(
                    graphics, font, leftPos, topPos, electricalPanel,
                    menu.position(), menu.energyStored(), menu.energyCapacity()
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component status = statusLabel();
        Component fittedStatus = MachineScreenLayout.fitToWidth(font, status, 72);
        int statusWidth = font.width(fittedStatus);
        int statusX = baseImageWidth - 8 - statusWidth;
        int titleWidth = MachineScreenLayout.availableTitleWidth(baseImageWidth, titleLabelX, statusWidth);
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(font, title, titleWidth),
                titleLabelX,
                titleLabelY,
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        graphics.drawString(
                font,
                fittedStatus,
                statusX,
                titleLabelY,
                menu.working() ? MachineScreenLayout.SUCCESS : MachineScreenLayout.TEXT_MUTED,
                false
        );
        if (menu.definition() == MultiblockDefinition.SHELVING_UNIT) {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(
                            font,
                            Component.translatable(
                                    "gui.magneticraft.shelving_summary",
                                    menu.installedChests(),
                                    menu.shelvingSlots()
                            ),
                            baseImageWidth - 16
                    ),
                    8,
                    24,
                    MachineScreenLayout.WARNING,
                    false
            );
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
        Component structureState = MachineScreenLayout.fitToWidth(
                font,
                structureOverview(),
                baseImageWidth - inventoryLabelX - font.width(playerInventoryTitle) - 12
        );
        graphics.drawString(
                font,
                structureState,
                baseImageWidth - 8 - font.width(structureState),
                inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED,
                false
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusTooltip(graphics, mouseX, mouseY, statusBars());
        if (electricalPanel != null) {
            ElectricalStatePanel.renderTooltip(
                    graphics, font, mouseX, mouseY, leftPos, topPos, electricalPanel,
                    menu.position(), menu.energyStored(), menu.energyCapacity()
            );
        }
        renderStructureTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private List<StatusBar> statusBars() {
        return LegacyMachineGuiLayout.multiblockStatusBars(
                menu.energyCapacity() > 0,
                menu.totalProgress() > 0,
                menu.bulkCapacity() > 0,
                menu.tankCount()
        );
    }

    private void drawStatusBars(GuiGraphics graphics, List<StatusBar> bars) {
        for (StatusBar bar : bars) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            int value = value(bar);
            int capacity = capacity(bar);
            int height = scaled(value, capacity, bounds.height() - 4);
            MachineScreenLayout.drawStatusBar(
                    graphics,
                    leftPos + bounds.x(),
                    topPos + bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    height,
                    color(bar),
                    true
            );
        }
    }

    private void renderStatusTooltip(GuiGraphics graphics, int mouseX, int mouseY, List<StatusBar> bars) {
        for (StatusBar bar : bars) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            if (!inside(mouseX, mouseY, bounds)) {
                continue;
            }
            String key = switch (bar.kind()) {
                case ENERGY -> "gui.magneticraft.energy.tooltip";
                case PROGRESS -> "gui.magneticraft.progress.tooltip";
                case BULK -> "gui.magneticraft.items.tooltip";
                case TANK, PRIMARY_FLUID, SECONDARY_FLUID -> "gui.magneticraft.fluid.tooltip";
            };
            graphics.renderTooltip(
                    font,
                    Component.translatable(key, value(bar), capacity(bar)),
                    mouseX,
                    mouseY
            );
            return;
        }
    }

    private int value(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyStored();
            case PROGRESS -> menu.progress();
            case BULK -> menu.bulkAmount();
            case TANK -> menu.fluidAmount(bar.index());
            case PRIMARY_FLUID, SECONDARY_FLUID -> 0;
        };
    }

    private int capacity(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyCapacity();
            case PROGRESS -> menu.totalProgress();
            case BULK -> menu.bulkCapacity();
            case TANK -> menu.fluidCapacity(bar.index());
            case PRIMARY_FLUID, SECONDARY_FLUID -> 0;
        };
    }

    private int color(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> MachineScreenLayout.ENERGY;
            case PROGRESS -> MachineScreenLayout.PROGRESS;
            case BULK -> MachineScreenLayout.WARNING;
            case TANK, PRIMARY_FLUID, SECONDARY_FLUID -> MachineScreenLayout.FLUID;
        };
    }

    MachineScreenBounds.Layout layout() {
        return layout(menu.definition(), imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(
            MultiblockDefinition definition,
            int imageWidth,
            int imageHeight
    ) {
        int baseImageWidth = LegacyMachineGuiLayout.multiblockSize(definition).width();
        int playerTop = LegacyMachineGuiLayout.multiblockPlayerTop(definition);
        int contentBottom = definition == MultiblockDefinition.SHELVING_UNIT ? 112 : 76;
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "multiblock/" + definition.id(), imageWidth, imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, baseImageWidth - 2, 15), "sections", 0)
                .element("machine", new LegacyMachineGuiLayout.Rect(
                        5, 14, baseImageWidth - 10, contentBottom - 14
                ))
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(
                        5, playerTop - 2, baseImageWidth - 10,
                        imageHeight - playerTop
                ), "sections", 0);
        if (definition.usesElectricity()) {
            builder.element("electrical", electricalPanelBounds(definition), "sections", 0);
        }
        List<LegacyMachineGuiLayout.Point> machineSlots = LegacyMachineGuiLayout.multiblockSlots(definition);
        for (int index = 0; index < machineSlots.size(); index++) {
            builder.child("machine_slot_" + index, LegacyMachineGuiLayout.slotBounds(machineSlots.get(index)),
                    "machine", "machine_slots", 0);
        }
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT, playerTop
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        List<StatusBar> bars = LegacyMachineGuiLayout.multiblockStatusBars(
                definition.usesElectricity(), hasProgress(definition),
                definition.bulkItemCapacity() > 0, definition.tankCount()
        );
        for (int index = 0; index < bars.size(); index++) {
            builder.child("status_" + index, bars.get(index).bounds(), "machine", "status_bars", 3);
        }
        return builder.build();
    }

    private static boolean hasProgress(MultiblockDefinition definition) {
        return switch (definition) {
            case GRINDER, SIEVE, HYDRAULIC_PRESS, PUMPJACK, REFINERY,
                    BIG_ELECTRIC_FURNACE, BIG_COMBUSTION_CHAMBER,
                    BIG_STEAM_BOILER, OIL_HEATER -> true;
            default -> false;
        };
    }

    private Component statusLabel() {
        if (menu.definition() == MultiblockDefinition.STEAM_TURBINE && menu.turbineVentingActive()) {
            return Component.translatable("gui.magneticraft.steam_turbine.venting_warning");
        }
        if (menu.voltage() != 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.voltage",
                    String.format(Locale.ROOT, "%.1f", menu.voltage())
            );
        }
        if (menu.temperatureKelvin() != 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.temperature_kelvin",
                    String.format(Locale.ROOT, "%.1f", menu.temperatureKelvin())
            );
        }
        return Component.translatable(
                menu.working() ? "gui.magneticraft.state.running" : "gui.magneticraft.state.stopped"
        );
    }

    private Component structureOverview() {
        return Component.translatable(
                "gui.magneticraft.multiblock_overview",
                Component.translatable(menu.formed()
                        ? "gui.magneticraft.multiblock.formed"
                        : "gui.magneticraft.multiblock.unformed"),
                Component.translatable("direction.minecraft." + menu.facing().getName()),
                MultiblockPortLayout.ports(menu.definition()).size()
        );
    }

    private void renderStructureTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = leftPos + inventoryLabelX + font.width(playerInventoryTitle) + 4;
        int endX = leftPos + baseImageWidth - 8;
        int startY = topPos + inventoryLabelY - 2;
        if (mouseX < startX || mouseX >= endX || mouseY < startY || mouseY >= startY + 12) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "gui.magneticraft.multiblock_structure_detail",
                Component.translatable(menu.mirrored()
                        ? "message.magneticraft.multiblock_mirrored_state"
                        : "message.magneticraft.multiblock_normal_state")
        ));
        for (PortGroup group : groupedPorts().values()) {
            lines.add(Component.translatable(
                    "gui.magneticraft.multiblock_port_status",
                    Component.translatable("gui.magneticraft.multiblock.port_kind." + group.kind()),
                    Component.translatable("gui.magneticraft.multiblock.port_mode." + group.mode()),
                    Component.translatable("direction.minecraft." + group.side().getName()),
                    group.count(),
                    Component.translatable(menu.formed()
                            ? "gui.magneticraft.multiblock.port_active"
                            : "gui.magneticraft.multiblock.port_inactive")
            ));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private Map<String, PortGroup> groupedPorts() {
        Map<String, PortGroup> groups = new LinkedHashMap<>();
        for (MultiblockPortLayout.Port port : MultiblockPortLayout.ports(menu.definition())) {
            String kind = port.kind().name().toLowerCase(Locale.ROOT);
            String mode = portMode(port);
            Direction side = port.worldSide(menu.facing());
            String key = kind + ":" + mode + ":" + side.getName();
            groups.compute(key, (ignored, existing) -> existing == null
                    ? new PortGroup(kind, mode, side, 1)
                    : new PortGroup(kind, mode, side, existing.count() + 1));
        }
        return groups;
    }

    private static String portMode(MultiblockPortLayout.Port port) {
        return switch (port.kind()) {
            case FLUID -> port.fluidAccess().name().toLowerCase(Locale.ROOT);
            case ITEM -> {
                boolean input = port.itemAccess().insertSlots().length > 0;
                boolean output = port.itemAccess().extractSlots().length > 0;
                yield input && output ? "both" : input ? "input" : "output";
            }
            case ELECTRICITY, HEAT -> "connection";
        };
    }

    private record PortGroup(String kind, String mode, Direction side, int count) {
    }

    private boolean inside(int mouseX, int mouseY, LegacyMachineGuiLayout.Rect bounds) {
        return mouseX >= leftPos + bounds.x() && mouseX < leftPos + bounds.right()
                && mouseY >= topPos + bounds.y() && mouseY < topPos + bounds.bottom();
    }

    private static int scaled(int value, int capacity, int size) {
        if (capacity <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, (long) value * size / capacity);
    }

    static LegacyMachineGuiLayout.Rect electricalPanelBounds(MultiblockDefinition definition) {
        return LegacyMachineGuiLayout.electricalPanel(
                LegacyMachineGuiLayout.multiblockSize(definition).width()
        );
    }
}
