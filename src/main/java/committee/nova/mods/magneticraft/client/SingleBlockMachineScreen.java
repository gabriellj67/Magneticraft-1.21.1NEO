package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.StatusBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Functional, texture-independent screen shared by every Task 5 device.
 */
public final class SingleBlockMachineScreen extends AbstractContainerScreen<SingleBlockMachineMenu> {
    private static final String[] INSERTER_BUTTON_KEYS = {
            "gui.magneticraft.inserter.whitelist",
            "gui.magneticraft.inserter.match_damage",
            "gui.magneticraft.inserter.match_tags",
            "gui.magneticraft.inserter.match_nbt",
            "gui.magneticraft.inserter.allow_stacking",
            "gui.magneticraft.inserter.reverse"
    };
    private static final MachineIcon[] INSERTER_BUTTON_ICONS = {
            MachineIcon.FILTER,
            MachineIcon.DAMAGE,
            MachineIcon.TAG,
            MachineIcon.DATA,
            MachineIcon.STACK,
            MachineIcon.REVERSE
    };
    private final List<MachineIconButton> inserterButtons = new ArrayList<>();
    private final int baseImageWidth;
    private final LegacyMachineGuiLayout.Rect electricalPanel;

    public SingleBlockMachineScreen(SingleBlockMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size baseSize = LegacyMachineGuiLayout.singleBlockSize(menu.definition());
        baseImageWidth = baseSize.width();
        electricalPanel = menu.definition().usesElectricity()
                ? electricalPanelBounds(menu.definition())
                : null;
        LegacyMachineGuiLayout.Size size = electricalPanel == null
                ? baseSize
                : LegacyMachineGuiLayout.withElectricalPanel(baseSize);
        imageWidth = size.width();
        imageHeight = size.height();
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
        inserterButtons.clear();
        if (menu.definition() == SingleBlockMachineDefinition.INSERTER) {
            for (int id = 0; id < INSERTER_BUTTON_KEYS.length; id++) {
                int buttonId = id;
                LegacyMachineGuiLayout.Rect bounds = LegacyMachineGuiLayout.inserterButtons().get(id);
                MachineIconButton button = new MachineIconButton(
                        leftPos + bounds.x(),
                        topPos + bounds.y(),
                        bounds.width(),
                        bounds.height(),
                        INSERTER_BUTTON_ICONS[id],
                        Component.translatable(INSERTER_BUTTON_KEYS[id]),
                        inserterExplanation(id, false),
                        ignored -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                            }
                        }
                );
                inserterButtons.add(addRenderableWidget(button));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int id = 0; id < inserterButtons.size(); id++) {
            boolean enabled = (menu.flags() & (1 << id)) != 0;
            inserterButtons.get(id)
                    .selected(enabled)
                    .updateExplanation(
                            Component.translatable(INSERTER_BUTTON_KEYS[id]),
                            inserterExplanation(id, enabled)
                    );
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusTooltips(graphics, mouseX, mouseY);
        if (electricalPanel != null) {
            ElectricalStatePanel.renderTooltip(
                    graphics, font, mouseX, mouseY, leftPos, topPos, electricalPanel,
                    menu.machinePosition(), menu.energyStored(), menu.energyCapacity()
            );
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        int playerTop = LegacyMachineGuiLayout.singleBlockPlayerTop(menu.definition());
        int machineHeight = machineHeight(menu.definition(), playerTop);
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 3, baseImageWidth - 10, machineHeight);
        if (menu.definition() != SingleBlockMachineDefinition.BOX) {
            MachineScreenLayout.drawSectionHeader(
                    graphics,
                    leftPos,
                    topPos + LegacyMachineGuiLayout.CONTENT_TITLE_TOP - 2,
                    baseImageWidth,
                    12
            );
        }
        MachineScreenLayout.drawCard(
                graphics,
                leftPos + 5,
                topPos + playerTop - 2,
                baseImageWidth - 10,
                imageHeight - playerTop
        );
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawStatusBars(graphics);
        if (electricalPanel != null) {
            ElectricalStatePanel.render(
                    graphics, font, leftPos, topPos, electricalPanel,
                    menu.machinePosition(), menu.energyStored(), menu.energyCapacity()
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.definition() == SingleBlockMachineDefinition.BOX) {
            return;
        }
        Component status = statusLabel();
        if (status == null) {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, title, baseImageWidth - titleLabelX - 8),
                    titleLabelX,
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
                    MachineScreenLayout.TEXT_PRIMARY,
                    false
            );
        } else {
            Component fittedStatus = MachineScreenLayout.fitToWidth(font, status, 72);
            int statusWidth = font.width(fittedStatus);
            int statusX = baseImageWidth - 8 - statusWidth;
            int titleWidth = MachineScreenLayout.availableTitleWidth(baseImageWidth, titleLabelX, statusWidth);
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, title, titleWidth),
                    titleLabelX,
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
                    MachineScreenLayout.TEXT_PRIMARY,
                    false
            );
            graphics.drawString(
                    font,
                    fittedStatus,
                    statusX,
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
        }
    }

    private void drawStatusBars(GuiGraphics graphics) {
        for (StatusBar bar : statusBars()) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            int height = scaled(value(bar), capacity(bar), bounds.height() - 4);
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

    private Component statusLabel() {
        if (menu.temperatureKelvin() > 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.temperature_celsius",
                    String.format(Locale.ROOT, "%.1f", menu.temperatureKelvin() - 273.15D)
            );
        }
        if (menu.voltage() > 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.voltage",
                    String.format(Locale.ROOT, "%.1f", menu.voltage())
            );
        }
        return menu.working() ? Component.translatable("gui.magneticraft.state.running") : null;
    }

    private Component inserterExplanation(int id, boolean enabled) {
        return Component.translatable(INSERTER_BUTTON_KEYS[id])
                .append("\n")
                .append(Component.translatable(
                        "gui.magneticraft.control.current_state",
                        Component.translatable(enabled
                                ? "gui.magneticraft.control.enabled"
                                : "gui.magneticraft.control.disabled")
                ));
    }

    private void renderStatusTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (StatusBar bar : statusBars()) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            if (mouseX < leftPos + bounds.x() || mouseX >= leftPos + bounds.right()
                    || mouseY < topPos + bounds.y() || mouseY >= topPos + bounds.bottom()) {
                continue;
            }
            String key = switch (bar.kind()) {
                case ENERGY -> menu.definition() == SingleBlockMachineDefinition.RF_HEATER
                        ? "gui.magneticraft.forge_energy.tooltip"
                        : "gui.magneticraft.energy.tooltip";
                case PROGRESS -> "gui.magneticraft.progress.tooltip";
                case PRIMARY_FLUID, SECONDARY_FLUID, TANK -> "gui.magneticraft.fluid.tooltip";
                case BULK -> "gui.magneticraft.items.tooltip";
            };
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(
                    key,
                    value(bar),
                    capacity(bar)
            ));
            if (bar.kind() == LegacyMachineGuiLayout.StatusKind.PROGRESS
                    && (menu.lastConsumption() > 0 || menu.lastProduction() > 0)) {
                lines.add(Component.translatable(
                        "gui.magneticraft.machine.rate.tooltip",
                        menu.lastConsumption(),
                        menu.lastProduction()
                ));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
    }

    private List<StatusBar> statusBars() {
        return LegacyMachineGuiLayout.singleBlockStatusBars(
                menu.energyCapacity() > 0,
                menu.primaryCapacity() > 0,
                menu.secondaryCapacity() > 0,
                displayedProgressTotal() > 0
        );
    }

    private int value(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyStored();
            case PRIMARY_FLUID -> menu.primaryFluid();
            case SECONDARY_FLUID -> menu.secondaryFluid();
            case PROGRESS -> displayedProgress();
            case BULK, TANK -> 0;
        };
    }

    private int capacity(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyCapacity();
            case PRIMARY_FLUID -> menu.primaryCapacity();
            case SECONDARY_FLUID -> menu.secondaryCapacity();
            case PROGRESS -> displayedProgressTotal();
            case BULK, TANK -> 0;
        };
    }

    private int color(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> MachineScreenLayout.ENERGY;
            case PRIMARY_FLUID -> MachineScreenLayout.FLUID;
            case SECONDARY_FLUID -> MachineScreenLayout.TEXT_PRIMARY;
            case PROGRESS -> MachineScreenLayout.PROGRESS;
            case BULK, TANK -> MachineScreenLayout.ACCENT;
        };
    }

    MachineScreenBounds.Layout layout() {
        return layout(menu.definition(), imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(
            SingleBlockMachineDefinition definition,
            int imageWidth,
            int imageHeight
    ) {
        int baseImageWidth = LegacyMachineGuiLayout.singleBlockSize(definition).width();
        int playerTop = LegacyMachineGuiLayout.singleBlockPlayerTop(definition);
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "single_block/" + definition.id(), imageWidth, imageHeight
        ).element("machine", new LegacyMachineGuiLayout.Rect(5, 3, baseImageWidth - 10,
                machineHeight(definition, playerTop)))
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, playerTop - 2,
                        baseImageWidth - 10, imageHeight - playerTop), "sections", 0);
        if (definition != SingleBlockMachineDefinition.BOX) {
            builder.element("title", new LegacyMachineGuiLayout.Rect(
                    1, LegacyMachineGuiLayout.CONTENT_TITLE_TOP - 2, baseImageWidth - 2, 12
            ), "sections", 0);
        }
        if (definition.usesElectricity()) {
            builder.element("electrical", electricalPanelBounds(definition), "sections", 0);
        }
        List<LegacyMachineGuiLayout.Rect> buttons = LegacyMachineGuiLayout.inserterButtons();
        for (int index = 0; definition == SingleBlockMachineDefinition.INSERTER
                && index < buttons.size(); index++) {
            builder.child("inserter_button_" + index, buttons.get(index), "machine", "machine_controls", 2);
        }
        List<LegacyMachineGuiLayout.Point> machineSlots = LegacyMachineGuiLayout.singleBlockSlots(definition);
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
        return builder.build();
    }

    private static int machineHeight(SingleBlockMachineDefinition definition, int playerTop) {
        return definition == SingleBlockMachineDefinition.BOX
                ? playerTop - 5
                : Math.max(24, playerTop - 15);
    }

    private int displayedProgress() {
        return menu.totalProgress() > 0 ? menu.progress() : menu.burnProgress();
    }

    private int displayedProgressTotal() {
        return menu.totalProgress() > 0 ? menu.totalProgress() : menu.burnTotal();
    }

    static LegacyMachineGuiLayout.Rect electricalPanelBounds(SingleBlockMachineDefinition definition) {
        return LegacyMachineGuiLayout.electricalPanel(
                LegacyMachineGuiLayout.singleBlockSize(definition).width()
        );
    }

    private static int scaled(int value, int capacity, int size) {
        if (capacity <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, (long) value * size / capacity);
    }
}
