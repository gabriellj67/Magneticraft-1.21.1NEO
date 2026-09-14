package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceAction;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceKind;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import committee.nova.mods.magneticraft.network.ElectricalDeviceActionMessage;
import committee.nova.mods.magneticraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;
import java.util.List;

/** Dedicated controls for all isolated two-terminal electrical devices. */
public final class ElectricalDeviceScreen extends AbstractContainerScreen<ElectricalDeviceMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);
    private static final int BOTTOM_STATUS_Y = 63;
    private static final int INVENTORY_LABEL_Y = LegacyMachineGuiLayout.STANDARD_PLAYER_TOP - 12;
    private static final int TEXT_LINE_HEIGHT = 9;
    private static final int[] RESISTOR_RING_COLORS = {
            0xFF161616, 0xFF6B3E26, 0xFFC62828, 0xFFEF6C00, 0xFFF9A825,
            0xFF2E7D32, 0xFF1565C0, 0xFF6A1B9A, 0xFF757575, 0xFFF5F5F5
    };

    private MachineIconButton reverseButton;
    private MachineIconButton redstoneButton;
    private MachineIconButton resetButton;
    private MachineIconButton controlRedstoneButton;
    private final MachineIconButton[] resistorButtons = new MachineIconButton[3];

    public ElectricalDeviceScreen(ElectricalDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.withElectricalPanel(
                new LegacyMachineGuiLayout.Size(BASE_IMAGE_WIDTH, LegacyMachineGuiLayout.STANDARD_HEIGHT)
        );
        imageWidth = size.width();
        imageHeight = size.height();
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
        if (menu.kind() == ElectricalDeviceKind.BOX_TRANSFORMER) {
            reverseButton = addRenderableWidget(iconButton(
                    8, 43, MachineIcon.REVERSE,
                    "gui.magneticraft.electrical.action.reverse_transformer",
                    directionLabel(),
                    ignored -> send(ElectricalDeviceAction.REVERSE_TRANSFORMER)
            ));
            redstoneButton = addRenderableWidget(iconButton(
                    32, 43, MachineIcon.REDSTONE,
                    "gui.magneticraft.electrical.action.redstone_mode",
                    redstoneLabel(),
                    ignored -> send(ElectricalDeviceAction.CYCLE_REDSTONE_MODE)
            ));
        } else if (menu.kind() == ElectricalDeviceKind.CIRCUIT_BREAKER) {
            resetButton = addRenderableWidget(iconButton(
                    8, 43, MachineIcon.RESET,
                    "gui.magneticraft.electrical.reset",
                    protectionState(),
                    ignored -> send(ElectricalDeviceAction.RESET_BREAKER)
            ));
        } else if (menu.kind() == ElectricalDeviceKind.ELECTRIC_SWITCH) {
            controlRedstoneButton = addRenderableWidget(iconButton(
                    8, 43, MachineIcon.SWITCH,
                    "gui.magneticraft.electrical.action.switch_mode",
                    controlRedstoneLabel(),
                    ignored -> send(ElectricalDeviceAction.CYCLE_CONTROL_REDSTONE)
            ));
        } else if (menu.kind() == ElectricalDeviceKind.RESISTOR) {
            ElectricalDeviceAction[] actions = {
                    ElectricalDeviceAction.CYCLE_RESISTOR_FIRST_RING,
                    ElectricalDeviceAction.CYCLE_RESISTOR_SECOND_RING,
                    ElectricalDeviceAction.CYCLE_RESISTOR_MULTIPLIER_RING
            };
            for (int ring = 0; ring < resistorButtons.length; ring++) {
                int index = ring;
                resistorButtons[ring] = addRenderableWidget(iconButton(
                        8 + ring * 24, 43, MachineIcon.RESISTOR,
                        "gui.magneticraft.electrical.action.resistor_ring",
                        resistorRingLabel(ring),
                        ignored -> send(actions[index])
                ).badge(Integer.toString(ring + 1)));
            }
        }
        refreshControls();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ElectricalStatePanel.renderTooltip(
                graphics,
                font,
                mouseX,
                mouseY,
                leftPos,
                topPos,
                ELECTRICAL_PANEL,
                menu.position(),
                0,
                0
        );
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, BASE_IMAGE_WIDTH);
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 17, BASE_IMAGE_WIDTH - 10, 55);
        MachineScreenLayout.drawPlayerInventory(graphics, leftPos, topPos);
        if (menu.kind() == ElectricalDeviceKind.FUSE_BOX) {
            MachineScreenLayout.drawSlot(
                    graphics,
                    leftPos,
                    topPos,
                    ElectricalDeviceMenu.FUSE_SLOT_X,
                    ElectricalDeviceMenu.FUSE_SLOT_Y
            );
        }
        if (menu.kind() == ElectricalDeviceKind.FUSE_BOX
                || menu.kind() == ElectricalDeviceKind.CIRCUIT_BREAKER) {
            int width = 160;
            int stressWidth = (int) Math.round((width - 4) * Math.max(0.0D, Math.min(1.0D, menu.thermalStress())));
            MachineScreenLayout.drawStatusBar(
                    graphics, leftPos + 8, topPos + 32, width, 7,
                    stressWidth, MachineScreenLayout.DANGER, false
            );
        }
        if (menu.kind() == ElectricalDeviceKind.RESISTOR) {
            for (int ring = 0; ring < resistorButtons.length; ring++) {
                int ringLeft = leftPos + 8 + ring * 24;
                graphics.fill(
                        ringLeft,
                        topPos + 33,
                        ringLeft + 20,
                        topPos + 39,
                        RESISTOR_RING_COLORS[resistorRing(ring)]
                );
            }
        }
        ElectricalStatePanel.render(
                graphics,
                font,
                leftPos,
                topPos,
                ELECTRICAL_PANEL,
                menu.position(),
                0,
                0
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, BASE_IMAGE_WIDTH - 16),
                titleLabelX, titleLabelY, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
        if (menu.kind() == ElectricalDeviceKind.BOX_TRANSFORMER) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.magneticraft.electrical.profile_state",
                            Component.translatable(menu.transformerProfileBound()
                                    ? "gui.magneticraft.electrical.bound"
                                    : "gui.magneticraft.electrical.missing_profile")
                    ),
                    8,
                    20,
                    MachineScreenLayout.TEXT_PRIMARY,
                    false
            );
            graphics.drawString(font, redstoneLabel(), 8, 31, MachineScreenLayout.TEXT_MUTED, false);
            return;
        }
        if (isControlDevice()) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.magneticraft.electrical.control_state",
                            Component.translatable(menu.controlEnabled()
                                    ? "gui.magneticraft.electrical.state.conducting"
                                    : "gui.magneticraft.electrical.state.open")
                    ),
                    8,
                    18,
                    MachineScreenLayout.TEXT_PRIMARY,
                    false
            );
            if (menu.kind() == ElectricalDeviceKind.RESISTOR) {
                graphics.drawString(
                        font,
                        Component.translatable(
                                "gui.magneticraft.electrical.resistance",
                                format(menu.resistorOhms())
                        ),
                        8,
                        25,
                        MachineScreenLayout.TEXT_MUTED,
                        false
                );
            }
            Component telemetry = Component.translatable(
                    "gui.magneticraft.electrical.control_telemetry",
                    Component.translatable(menu.controlDirection().translationKey()),
                    format(menu.controlCurrentAmps()),
                    format(menu.controlLossJoules())
            );
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, telemetry, BASE_IMAGE_WIDTH - 16),
                    8,
                    menu.kind() == ElectricalDeviceKind.DIODE ? 32 : BOTTOM_STATUS_Y,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
            return;
        }
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.magneticraft.electrical.rating",
                        Component.translatable(menu.heavyRating()
                                ? "gui.magneticraft.electrical.rating.heavy"
                                : "gui.magneticraft.electrical.rating.standard")
                ),
                8,
                18,
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        Component protection = Component.translatable(
                "gui.magneticraft.electrical.protection_state", protectionState()
        );
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(font, protection, BASE_IMAGE_WIDTH - 16),
                8,
                BOTTOM_STATUS_Y,
                MachineScreenLayout.TEXT_MUTED,
                false
        );
    }

    private void refreshControls() {
        if (reverseButton != null) {
            reverseButton.selected(menu.transformerReversed());
            reverseButton.updateExplanation(
                    Component.translatable("gui.magneticraft.electrical.action.reverse_transformer"),
                    explanation("gui.magneticraft.electrical.action.reverse_transformer", directionLabel())
            );
            reverseButton.active = menu.transformerProfileBound();
        }
        if (redstoneButton != null) {
            redstoneButton.updateExplanation(
                    Component.translatable("gui.magneticraft.electrical.action.redstone_mode"),
                    explanation("gui.magneticraft.electrical.action.redstone_mode", redstoneLabel())
            );
            redstoneButton.active = menu.transformerProfileBound();
        }
        if (resetButton != null) {
            resetButton.active = menu.breakerTripped() && !menu.redstoneForcedOpen();
            resetButton.updateExplanation(
                    Component.translatable("gui.magneticraft.electrical.reset"),
                    explanation("gui.magneticraft.electrical.reset", protectionState())
            );
        }
        if (controlRedstoneButton != null) {
            controlRedstoneButton.updateExplanation(
                    Component.translatable("gui.magneticraft.electrical.action.switch_mode"),
                    explanation("gui.magneticraft.electrical.action.switch_mode", controlRedstoneLabel())
            );
        }
        for (int ring = 0; ring < resistorButtons.length; ring++) {
            if (resistorButtons[ring] != null) {
                resistorButtons[ring].updateExplanation(
                        Component.translatable("gui.magneticraft.electrical.action.resistor_ring"),
                        explanation("gui.magneticraft.electrical.action.resistor_ring", resistorRingLabel(ring))
                );
            }
        }
    }

    private Component directionLabel() {
        return Component.translatable(menu.transformerReversed()
                ? "gui.magneticraft.electrical.direction.reverse"
                : "gui.magneticraft.electrical.direction.forward");
    }

    private Component redstoneLabel() {
        String id = menu.transformerRedstoneMode().name().toLowerCase(Locale.ROOT);
        return Component.translatable("gui.magneticraft.electrical.redstone." + id);
    }

    private Component controlRedstoneLabel() {
        String id = menu.controlRedstoneMode().name().toLowerCase(Locale.ROOT);
        return Component.translatable("gui.magneticraft.electrical.switch_redstone." + id);
    }

    private Component resistorRingLabel(int ring) {
        return Component.translatable(
                "gui.magneticraft.electrical.resistor_ring",
                ring + 1,
                resistorRing(ring)
        );
    }

    private int resistorRing(int ring) {
        return switch (ring) {
            case 0 -> menu.resistorFirstRing();
            case 1 -> menu.resistorSecondRing();
            case 2 -> menu.resistorMultiplierRing();
            default -> 0;
        };
    }

    private boolean isControlDevice() {
        return menu.kind() == ElectricalDeviceKind.ELECTRIC_SWITCH
                || menu.kind() == ElectricalDeviceKind.DIODE
                || menu.kind() == ElectricalDeviceKind.RESISTOR;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private Component protectionState() {
        if (menu.redstoneForcedOpen()) {
            return Component.translatable("gui.magneticraft.electrical.state.redstone_open");
        }
        if (menu.breakerTripped()) {
            return Component.translatable("gui.magneticraft.electrical.state.tripped");
        }
        if (menu.fuseBlown()) {
            return Component.translatable("gui.magneticraft.electrical.state.blown");
        }
        return Component.translatable("gui.magneticraft.electrical.state.ready");
    }

    private MachineIconButton iconButton(
            int x,
            int y,
            MachineIcon icon,
            String actionKey,
            Component currentState,
            MachineIconButton.OnPress onPress
    ) {
        Component action = Component.translatable(actionKey);
        return new MachineIconButton(
                leftPos + x,
                topPos + y,
                20,
                20,
                icon,
                action,
                explanation(actionKey, currentState),
                onPress
        );
    }

    private static Component explanation(String actionKey, Component currentState) {
        return Component.translatable(actionKey)
                .append("\n")
                .append(Component.translatable("gui.magneticraft.control.current_state", currentState));
    }

    MachineScreenBounds.Layout layout() {
        return layout(menu.kind(), imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(
            ElectricalDeviceKind kind,
            int imageWidth,
            int imageHeight
    ) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                "electrical_device/" + kind.name().toLowerCase(Locale.ROOT),
                imageWidth,
                imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, BASE_IMAGE_WIDTH - 2, 15), "sections", 1)
                .element("controls", new LegacyMachineGuiLayout.Rect(5, 17, BASE_IMAGE_WIDTH - 10, 55))
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, 78, 166, 86), "sections", 1)
                .element("electrical", ELECTRICAL_PANEL, "sections", 1)
                .element("inventory_label", new LegacyMachineGuiLayout.Rect(
                        8, INVENTORY_LABEL_Y, BASE_IMAGE_WIDTH - 16, TEXT_LINE_HEIGHT
                ), "bottom_text", 0);
        int count = kind == ElectricalDeviceKind.RESISTOR ? 3
                : kind == ElectricalDeviceKind.BOX_TRANSFORMER ? 2
                : kind == ElectricalDeviceKind.CIRCUIT_BREAKER
                        || kind == ElectricalDeviceKind.ELECTRIC_SWITCH ? 1 : 0;
        for (int index = 0; index < count; index++) {
            builder.child("button_" + index, new LegacyMachineGuiLayout.Rect(8 + index * 24, 43, 20, 20),
                    "controls", "control_buttons", 4);
        }
        if (kind == ElectricalDeviceKind.FUSE_BOX) {
            builder.child("fuse_slot", new LegacyMachineGuiLayout.Rect(
                    ElectricalDeviceMenu.FUSE_SLOT_X - 1,
                    ElectricalDeviceMenu.FUSE_SLOT_Y - 1,
                    18,
                    18
            ), "controls", "control_slots", 0);
        }
        if (kind == ElectricalDeviceKind.FUSE_BOX
                || kind == ElectricalDeviceKind.CIRCUIT_BREAKER
                || kind == ElectricalDeviceKind.ELECTRIC_SWITCH
                || kind == ElectricalDeviceKind.RESISTOR) {
            builder.child("bottom_status", new LegacyMachineGuiLayout.Rect(
                    8, BOTTOM_STATUS_Y, BASE_IMAGE_WIDTH - 16, TEXT_LINE_HEIGHT
            ), "controls", "bottom_text", 0);
        }
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT, LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }

    private void send(ElectricalDeviceAction action) {
        ModNetwork.controlElectricalDevice(new ElectricalDeviceActionMessage(menu.position(), action));
    }
}
