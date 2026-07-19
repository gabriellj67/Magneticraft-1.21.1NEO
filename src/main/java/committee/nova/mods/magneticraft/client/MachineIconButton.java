package committee.nova.mods.magneticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Accessible icon-only control with a stable hit box and full hover/focus explanation. */
final class MachineIconButton extends AbstractButton {
    private final MachineIcon icon;
    private final OnPress onPress;
    private boolean selected;
    private boolean dangerous;
    private String badge = "";

    MachineIconButton(
            int x,
            int y,
            int width,
            int height,
            MachineIcon icon,
            Component narration,
            Component tooltip,
            OnPress onPress
    ) {
        super(x, y, width, height, narration);
        this.icon = icon;
        this.onPress = onPress;
        setTooltip(Tooltip.create(tooltip, narration));
        setTooltipDelay(150);
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }

    MachineIconButton selected(boolean value) {
        selected = value;
        return this;
    }

    MachineIconButton badge(String value) {
        badge = value == null ? "" : value;
        return this;
    }

    MachineIconButton dangerous(boolean value) {
        dangerous = value;
        return this;
    }

    void updateExplanation(Component narration, Component tooltip) {
        setMessage(narration);
        setTooltip(Tooltip.create(tooltip, narration));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean pressed = active && isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
        int background = !active
                ? MachineScreenLayout.BUTTON_DISABLED
                : pressed
                        ? MachineScreenLayout.BUTTON_PRESSED
                        : selected
                                ? MachineScreenLayout.BUTTON_SELECTED
                                : isHoveredOrFocused()
                                        ? MachineScreenLayout.BUTTON_HOVER
                                        : MachineScreenLayout.BUTTON_BACKGROUND;
        int border = !active
                ? 0xFF2B3843
                : dangerous
                        ? MachineScreenLayout.DANGER
                        : isFocused()
                        ? MachineScreenLayout.ACCENT
                        : selected
                                ? MachineScreenLayout.ACCENT
                                : MachineScreenLayout.BUTTON_BORDER;
        int foreground = !active
                ? MachineScreenLayout.TEXT_DISABLED
                : dangerous ? MachineScreenLayout.DANGER : MachineScreenLayout.TEXT_PRIMARY;

        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, background);
        if (isHoveredOrFocused() && active) {
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, MachineScreenLayout.ACCENT);
        }
        if (selected) {
            graphics.fill(getX() + 2, getY() + height - 3, getX() + width - 2, getY() + height - 2,
                    MachineScreenLayout.ACCENT);
        }
        icon.render(graphics, getX(), getY(), width, height, foreground);
        if (!badge.isEmpty()) {
            graphics.drawString(
                    Minecraft.getInstance().font,
                    badge,
                    getX() + width - 2 - Minecraft.getInstance().font.width(badge),
                    getY() + height - 9,
                    active ? MachineScreenLayout.ACCENT : MachineScreenLayout.TEXT_DISABLED,
                    false
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    interface OnPress {
        void onPress(MachineIconButton button);
    }
}
