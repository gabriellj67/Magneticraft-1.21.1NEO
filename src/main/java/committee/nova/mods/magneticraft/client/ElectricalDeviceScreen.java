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

/** Dedicated controls for transformers, fuse boxes and circuit breakers. */
public final class ElectricalDeviceScreen extends AbstractContainerScreen<ElectricalDeviceMenu> {
    static final int BASE_IMAGE_WIDTH = LegacyMachineGuiLayout.STANDARD_WIDTH;
    static final LegacyMachineGuiLayout.Rect ELECTRICAL_PANEL =
            LegacyMachineGuiLayout.electricalPanel(BASE_IMAGE_WIDTH);

    private Button reverseButton;
    private Button redstoneButton;
    private Button resetButton;

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
        if (menu.kind() != ElectricalDeviceKind.BOX_TRANSFORMER) {
            int barLeft = leftPos + 8;
            int barTop = topPos + 32;
            int width = 160;
            graphics.fill(barLeft, barTop, barLeft + width, barTop + 5, 0xFF252A31);
            int stressWidth = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, menu.thermalStress())));
            graphics.fill(barLeft, barTop, barLeft + stressWidth, barTop + 5, 0xFFE06044);
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
