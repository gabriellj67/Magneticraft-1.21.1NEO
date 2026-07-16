package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceAction;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceKind;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import committee.nova.mods.magneticraft.network.ElectricalDeviceActionMessage;
import committee.nova.mods.magneticraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Dedicated controls for all isolated two-terminal electrical devices. */
public final class ElectricalDeviceScreen extends AbstractContainerScreen<ElectricalDeviceMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);
    private static final int[] RESISTOR_RING_COLORS = {
            0xFF161616, 0xFF6B3E26, 0xFFC62828, 0xFFEF6C00, 0xFFF9A825,
            0xFF2E7D32, 0xFF1565C0, 0xFF6A1B9A, 0xFF757575, 0xFFF5F5F5
    };

    private Button reverseButton;
    private Button redstoneButton;
    private Button resetButton;
    private Button controlRedstoneButton;
    private final Button[] resistorButtons = new Button[3];

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
        if (menu.kind() == ElectricalDeviceKind.BOX_TRANSFORMER) {
            reverseButton = addRenderableWidget(Button.builder(
                            directionLabel(),
                            ignored -> send(ElectricalDeviceAction.REVERSE_TRANSFORMER)
                    )
                    .bounds(leftPos + 8, topPos + 43, 78, 20)
                    .build());
            redstoneButton = addRenderableWidget(Button.builder(
                            redstoneLabel(),
                            ignored -> send(ElectricalDeviceAction.CYCLE_REDSTONE_MODE)
                    )
                    .bounds(leftPos + 90, topPos + 43, 78, 20)
                    .build());
        } else if (menu.kind() == ElectricalDeviceKind.CIRCUIT_BREAKER) {
            resetButton = addRenderableWidget(Button.builder(
                            Component.translatable("gui.magneticraft.electrical.reset"),
                            ignored -> send(ElectricalDeviceAction.RESET_BREAKER)
                    )
                    .bounds(leftPos + 48, topPos + 43, 80, 20)
                    .build());
        } else if (menu.kind() == ElectricalDeviceKind.ELECTRIC_SWITCH) {
            controlRedstoneButton = addRenderableWidget(Button.builder(
                            controlRedstoneLabel(),
                            ignored -> send(ElectricalDeviceAction.CYCLE_CONTROL_REDSTONE)
                    )
                    .bounds(leftPos + 8, topPos + 43, 160, 20)
                    .build());
        } else if (menu.kind() == ElectricalDeviceKind.RESISTOR) {
            ElectricalDeviceAction[] actions = {
                    ElectricalDeviceAction.CYCLE_RESISTOR_FIRST_RING,
                    ElectricalDeviceAction.CYCLE_RESISTOR_SECOND_RING,
                    ElectricalDeviceAction.CYCLE_RESISTOR_MULTIPLIER_RING
            };
            for (int ring = 0; ring < resistorButtons.length; ring++) {
                int index = ring;
                resistorButtons[ring] = addRenderableWidget(Button.builder(
                                resistorRingLabel(ring),
                                ignored -> send(actions[index])
                        )
                        .bounds(leftPos + 8 + ring * 46, topPos + 43, 40, 20)
                        .build());
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
        renderBackground(graphics);
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
            int barLeft = leftPos + 8;
            int barTop = topPos + 32;
            int width = 160;
            graphics.fill(barLeft, barTop, barLeft + width, barTop + 5, 0xFF252A31);
            int stressWidth = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, menu.thermalStress())));
            graphics.fill(barLeft, barTop, barLeft + stressWidth, barTop + 5, 0xFFE06044);
        }
        if (menu.kind() == ElectricalDeviceKind.RESISTOR) {
            for (int ring = 0; ring < resistorButtons.length; ring++) {
                int ringLeft = leftPos + 8 + ring * 46;
                graphics.fill(
                        ringLeft,
                        topPos + 33,
                        ringLeft + 40,
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
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
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
                    0x404040,
                    false
            );
            graphics.drawString(font, redstoneLabel(), 8, 31, 0x404040, false);
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
                    0x404040,
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
                        0x404040,
                        false
                );
            }
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.magneticraft.electrical.control_telemetry",
                            Component.translatable(menu.controlDirection().translationKey()),
                            format(menu.controlCurrentAmps()),
                            format(menu.controlLossJoules())
                    ),
                    8,
                    menu.kind() == ElectricalDeviceKind.DIODE ? 32 : 67,
                    0x404040,
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
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.electrical.protection_state", protectionState()),
                8,
                64,
                0x404040,
                false
        );
    }

    private void refreshControls() {
        if (reverseButton != null) {
            reverseButton.setMessage(directionLabel());
            reverseButton.active = menu.transformerProfileBound();
        }
        if (redstoneButton != null) {
            redstoneButton.setMessage(redstoneLabel());
            redstoneButton.active = menu.transformerProfileBound();
        }
        if (resetButton != null) {
            resetButton.active = menu.breakerTripped() && !menu.redstoneForcedOpen();
        }
        if (controlRedstoneButton != null) {
            controlRedstoneButton.setMessage(controlRedstoneLabel());
        }
        for (int ring = 0; ring < resistorButtons.length; ring++) {
            if (resistorButtons[ring] != null) {
                resistorButtons[ring].setMessage(resistorRingLabel(ring));
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

    private void send(ElectricalDeviceAction action) {
        ModNetwork.controlElectricalDevice(new ElectricalDeviceActionMessage(menu.position(), action));
    }
}
