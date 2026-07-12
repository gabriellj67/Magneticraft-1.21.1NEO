package committee.nova.mods.magneticraft.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared vanilla-sized panel and slot rendering for texture-free machine screens.
 */
final class MachineScreenLayout {
    private static final int PANEL_BORDER = 0xFF202020;
    private static final int PANEL_BACKGROUND = 0xFFC6C6C6;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_BACKGROUND = 0xFF8B8B8B;

    private MachineScreenLayout() {
    }

    static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, PANEL_BORDER);
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_BACKGROUND);
    }

    static void drawPlayerInventory(GuiGraphics graphics, int left, int top) {
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
    }

    static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF111416);
    }
}
