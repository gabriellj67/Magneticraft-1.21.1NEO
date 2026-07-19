package committee.nova.mods.magneticraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Shared visual language for every Magneticraft-owned screen. */
final class MachineScreenLayout {
    static final int PANEL_BORDER = 0xFF34495A;
    static final int PANEL_BACKGROUND = 0xFF0B1118;
    static final int HEADER_BACKGROUND = 0xFF111C27;
    static final int CARD_BACKGROUND = 0xFF101820;
    static final int INSET_BACKGROUND = 0xFF070B10;
    static final int SLOT_BORDER = 0xFF42586B;
    static final int SLOT_BACKGROUND = 0xFF17222D;
    static final int SLOT_HIGHLIGHT = 0xFF263746;

    static final int TEXT_PRIMARY = 0xFFE6EDF3;
    static final int TEXT_MUTED = 0xFF94A3B3;
    static final int TEXT_DISABLED = 0xFF667483;
    static final int ACCENT = 0xFF37D4E8;
    static final int SUCCESS = 0xFF58D68D;
    static final int WARNING = 0xFFFFB454;
    static final int DANGER = 0xFFFF6673;
    static final int ENERGY = 0xFF74D680;
    static final int HEAT = 0xFFFF8A4C;
    static final int FLUID = 0xFF4CBCEB;
    static final int PROGRESS = 0xFFB88CFF;

    static final int BUTTON_BACKGROUND = 0xFF16232E;
    static final int BUTTON_HOVER = 0xFF203846;
    static final int BUTTON_SELECTED = 0xFF174653;
    static final int BUTTON_DISABLED = 0xFF111820;
    static final int BUTTON_BORDER = 0xFF4A6071;
    static final int BUTTON_PRESSED = 0xFF0F3039;

    private MachineScreenLayout() {
    }

    static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, PANEL_BORDER);
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_BACKGROUND);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 3, ACCENT);
    }

    static void drawHeader(GuiGraphics graphics, int left, int top, int width) {
        graphics.fill(left + 1, top + 1, left + width - 1, top + 15, HEADER_BACKGROUND);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 3, ACCENT);
        graphics.fill(left + 1, top + 15, left + width - 1, top + 16, 0xFF243746);
    }

    static void drawSectionHeader(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left + 1, top, left + width - 1, top + height, HEADER_BACKGROUND);
        graphics.fill(left + 1, top, left + width - 1, top + 1, 0xFF243746);
        graphics.fill(left + 1, top + height - 1, left + width - 1, top + height, 0xFF243746);
        graphics.fill(left + 1, top + 1, left + 3, top + height - 1, ACCENT);
    }

    static void drawPlayerInventory(GuiGraphics graphics, int left, int top) {
        drawCard(graphics, left + 5, top + 78, 166, 86);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, left, top, 8 + column * 18, 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, left, top, 8 + column * 18, 142);
        }
    }

    static void drawSlot(GuiGraphics graphics, int left, int top, int slotX, int slotY) {
        int x = left + slotX - 1;
        int y = top + slotY - 1;
        graphics.fill(x, y, x + 18, y + 18, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BACKGROUND);
        graphics.fill(x + 2, y + 2, x + 16, y + 3, SLOT_HIGHLIGHT);
        graphics.fill(x + 2, y + 3, x + 3, y + 16, SLOT_HIGHLIGHT);
    }

    static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, INSET_BACKGROUND);
    }

    static void drawCard(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF263A49);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, CARD_BACKGROUND);
    }

    static void drawStatusBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int filled,
            int color,
            boolean vertical
    ) {
        drawInset(graphics, x, y, width, height);
        int innerWidth = Math.max(0, width - 4);
        int innerHeight = Math.max(0, height - 4);
        if (vertical) {
            int amount = Math.max(0, Math.min(innerHeight, filled));
            graphics.fill(x + 2, y + height - 2 - amount, x + width - 2, y + height - 2, color);
        } else {
            int amount = Math.max(0, Math.min(innerWidth, filled));
            graphics.fill(x + 2, y + 2, x + 2 + amount, y + height - 2, color);
        }
    }

    static Component fitToWidth(Font font, Component text, int maxWidth) {
        if (maxWidth <= 0) {
            return Component.empty();
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int contentWidth = Math.max(0, maxWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(text.getString(), contentWidth) + suffix)
                .withStyle(text.getStyle());
    }

    static int availableTitleWidth(int imageWidth, int titleX, int statusWidth) {
        int statusX = imageWidth - 8 - Math.max(0, statusWidth);
        return Math.max(0, statusX - titleX - 6);
    }

    static void validateInDevelopment(MachineScreenBounds.Layout layout) {
        if (!FMLEnvironment.production) {
            layout.validate();
        }
    }
}
