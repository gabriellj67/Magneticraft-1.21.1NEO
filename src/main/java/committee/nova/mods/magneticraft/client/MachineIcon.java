package committee.nova.mods.magneticraft.client;

import net.minecraft.client.gui.GuiGraphics;

/** Small procedural pixel glyphs used by Magneticraft controls. */
enum MachineIcon {
    PREVIOUS,
    NEXT,
    UP,
    DOWN,
    START,
    PAUSE,
    STOP,
    SCRAM,
    LOAD,
    UNLOAD,
    PLUS,
    MINUS,
    MODE,
    REVERSE,
    REDSTONE,
    RESET,
    SWITCH,
    RESISTOR,
    LANGUAGE,
    UPLOAD,
    UPGRADE,
    FILTER,
    DAMAGE,
    TAG,
    DATA,
    STACK,
    CONFIRM;

    void render(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        switch (this) {
            case PREVIOUS -> arrowLeft(graphics, centerX, centerY, color);
            case NEXT -> arrowRight(graphics, centerX, centerY, color);
            case UP -> arrowUp(graphics, centerX, centerY, color);
            case DOWN -> arrowDown(graphics, centerX, centerY, color);
            case START -> triangleRight(graphics, centerX, centerY, color);
            case PAUSE -> {
                graphics.fill(centerX - 4, centerY - 4, centerX - 1, centerY + 5, color);
                graphics.fill(centerX + 1, centerY - 4, centerX + 4, centerY + 5, color);
            }
            case STOP -> graphics.fill(centerX - 3, centerY - 3, centerX + 4, centerY + 4, color);
            case SCRAM -> {
                graphics.fill(centerX - 4, centerY - 4, centerX + 5, centerY + 5, color);
                graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, MachineScreenLayout.BUTTON_BACKGROUND);
                h(graphics, centerX - 2, centerX + 2, centerY, color);
                v(graphics, centerX, centerY - 2, centerY + 2, color);
            }
            case LOAD -> {
                box(graphics, centerX - 4, centerY - 1, 9, 6, color);
                arrowDown(graphics, centerX, centerY - 3, color);
            }
            case UNLOAD -> {
                box(graphics, centerX - 4, centerY, 9, 5, color);
                arrowUp(graphics, centerX, centerY - 2, color);
            }
            case PLUS -> {
                h(graphics, centerX - 4, centerX + 4, centerY, color);
                v(graphics, centerX, centerY - 4, centerY + 4, color);
            }
            case MINUS -> h(graphics, centerX - 4, centerX + 4, centerY, color);
            case MODE -> {
                h(graphics, centerX - 4, centerX + 4, centerY - 3, color);
                h(graphics, centerX - 4, centerX + 4, centerY + 3, color);
                graphics.fill(centerX - 2, centerY - 4, centerX + 1, centerY - 1, color);
                graphics.fill(centerX + 1, centerY + 2, centerX + 4, centerY + 5, color);
            }
            case REVERSE -> {
                arrowRight(graphics, centerX, centerY - 3, color);
                arrowLeft(graphics, centerX, centerY + 3, color);
            }
            case REDSTONE -> {
                v(graphics, centerX - 3, centerY - 4, centerY + 2, color);
                v(graphics, centerX + 3, centerY - 2, centerY + 4, color);
                h(graphics, centerX - 3, centerX + 3, centerY - 2, color);
                h(graphics, centerX - 3, centerX + 3, centerY + 2, color);
            }
            case RESET -> {
                arrowLeft(graphics, centerX - 1, centerY - 3, color);
                v(graphics, centerX + 4, centerY - 2, centerY + 3, color);
                h(graphics, centerX - 2, centerX + 4, centerY + 3, color);
            }
            case SWITCH -> {
                h(graphics, centerX - 5, centerX - 2, centerY + 3, color);
                h(graphics, centerX + 2, centerX + 5, centerY + 3, color);
                diagonal(graphics, centerX - 2, centerY + 2, centerX + 3, centerY - 3, color);
            }
            case RESISTOR -> {
                h(graphics, centerX - 5, centerX - 3, centerY, color);
                h(graphics, centerX + 3, centerX + 5, centerY, color);
                box(graphics, centerX - 3, centerY - 2, 7, 5, color);
            }
            case LANGUAGE -> {
                box(graphics, centerX - 4, centerY - 4, 9, 9, color);
                h(graphics, centerX - 3, centerX + 3, centerY, color);
                v(graphics, centerX, centerY - 3, centerY + 3, color);
            }
            case UPLOAD -> {
                arrowUp(graphics, centerX, centerY - 2, color);
                h(graphics, centerX - 4, centerX + 4, centerY + 4, color);
                v(graphics, centerX - 4, centerY + 2, centerY + 4, color);
                v(graphics, centerX + 4, centerY + 2, centerY + 4, color);
            }
            case UPGRADE -> {
                arrowUp(graphics, centerX, centerY - 2, color);
                h(graphics, centerX - 4, centerX + 4, centerY + 4, color);
            }
            case FILTER -> {
                h(graphics, centerX - 4, centerX + 4, centerY - 4, color);
                diagonal(graphics, centerX - 4, centerY - 3, centerX, centerY + 1, color);
                diagonal(graphics, centerX + 4, centerY - 3, centerX, centerY + 1, color);
                v(graphics, centerX, centerY + 1, centerY + 4, color);
            }
            case DAMAGE -> {
                diagonal(graphics, centerX - 4, centerY + 4, centerX + 4, centerY - 4, color);
                h(graphics, centerX - 4, centerX, centerY - 4, color);
                v(graphics, centerX + 4, centerY, centerY + 4, color);
            }
            case TAG -> {
                diagonal(graphics, centerX - 4, centerY - 1, centerX, centerY - 5, color);
                diagonal(graphics, centerX, centerY - 5, centerX + 5, centerY, color);
                diagonal(graphics, centerX + 5, centerY, centerX, centerY + 5, color);
                diagonal(graphics, centerX, centerY + 5, centerX - 4, centerY + 1, color);
                graphics.fill(centerX - 1, centerY - 2, centerX + 1, centerY, color);
            }
            case DATA -> {
                v(graphics, centerX - 4, centerY - 4, centerY + 4, color);
                v(graphics, centerX + 4, centerY - 4, centerY + 4, color);
                h(graphics, centerX - 4, centerX - 2, centerY - 4, color);
                h(graphics, centerX - 4, centerX - 2, centerY + 4, color);
                h(graphics, centerX + 2, centerX + 4, centerY - 4, color);
                h(graphics, centerX + 2, centerX + 4, centerY + 4, color);
            }
            case STACK -> {
                box(graphics, centerX - 4, centerY - 4, 7, 5, color);
                box(graphics, centerX - 1, centerY, 7, 5, color);
            }
            case CONFIRM -> {
                diagonal(graphics, centerX - 4, centerY, centerX - 1, centerY + 3, color);
                diagonal(graphics, centerX - 1, centerY + 3, centerX + 5, centerY - 4, color);
            }
        }
    }

    private static void arrowLeft(GuiGraphics graphics, int x, int y, int color) {
        h(graphics, x - 4, x + 4, y, color);
        diagonal(graphics, x - 4, y, x, y - 4, color);
        diagonal(graphics, x - 4, y, x, y + 4, color);
    }

    private static void arrowRight(GuiGraphics graphics, int x, int y, int color) {
        h(graphics, x - 4, x + 4, y, color);
        diagonal(graphics, x + 4, y, x, y - 4, color);
        diagonal(graphics, x + 4, y, x, y + 4, color);
    }

    private static void arrowUp(GuiGraphics graphics, int x, int y, int color) {
        v(graphics, x, y - 4, y + 4, color);
        diagonal(graphics, x, y - 4, x - 4, y, color);
        diagonal(graphics, x, y - 4, x + 4, y, color);
    }

    private static void arrowDown(GuiGraphics graphics, int x, int y, int color) {
        v(graphics, x, y - 4, y + 4, color);
        diagonal(graphics, x, y + 4, x - 4, y, color);
        diagonal(graphics, x, y + 4, x + 4, y, color);
    }

    private static void triangleRight(GuiGraphics graphics, int x, int y, int color) {
        for (int row = -4; row <= 4; row++) {
            int halfWidth = 4 - Math.abs(row);
            graphics.fill(x - 3, y + row, x - 3 + halfWidth + 1, y + row + 1, color);
        }
    }

    private static void box(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        h(graphics, x, x + width - 1, y, color);
        h(graphics, x, x + width - 1, y + height - 1, color);
        v(graphics, x, y, y + height - 1, color);
        v(graphics, x + width - 1, y, y + height - 1, color);
    }

    private static void h(GuiGraphics graphics, int x1, int x2, int y, int color) {
        graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    private static void v(GuiGraphics graphics, int x, int y1, int y2, int color) {
        graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color);
    }

    private static void diagonal(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / Math.max(1, steps);
            int y = y1 + (y2 - y1) * step / Math.max(1, steps);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
