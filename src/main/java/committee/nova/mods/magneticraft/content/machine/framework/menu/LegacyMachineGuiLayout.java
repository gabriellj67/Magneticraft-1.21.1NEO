package committee.nova.mods.magneticraft.content.machine.framework.menu;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared geometry contract ported from the Nova 1.12 GUI configuration.
 * Menus and screens consume the same coordinates so interactive slots and
 * rendered controls cannot drift apart.
 */
public final class LegacyMachineGuiLayout {
    public static final int STANDARD_WIDTH = 176;
    public static final int STANDARD_HEIGHT = 166;
    public static final int BOX_HEIGHT = 150;
    public static final int SHELVING_WIDTH = 194;
    public static final int SHELVING_HEIGHT = 207;
    public static final int PROGRAMMABLE_WIDTH = 350;
    public static final int COMPUTER_HEIGHT = 240;
    public static final int MINING_ROBOT_HEIGHT = 240;
    public static final int STANDARD_PLAYER_LEFT = 8;
    public static final int STANDARD_PLAYER_TOP = 84;
    public static final int BOX_PLAYER_TOP = 68;
    public static final int SHELVING_PLAYER_TOP = 125;
    public static final int PROGRAMMABLE_PLAYER_LEFT = 95;
    public static final int COMPUTER_PLAYER_TOP = 160;
    public static final int MINING_ROBOT_PLAYER_TOP = 160;
    public static final int PROGRAMMABLE_EDITOR_LEFT = 26;
    public static final int PROGRAMMABLE_EDITOR_TOP = 18;
    public static final int PROGRAMMABLE_EDITOR_WIDTH = 208;
    public static final int PROGRAMMABLE_EDITOR_HEIGHT = 11;
    public static final int PROGRAMMABLE_PAGE_TOP = 116;
    public static final int PROGRAMMABLE_BUTTON_TOP = 128;
    public static final int MINING_ROBOT_INVENTORY_LEFT = 268;
    public static final int CONTENT_TITLE_TOP = 72;
    public static final int STATUS_TOP = 18;
    public static final int STATUS_HEIGHT = 52;
    public static final int STATUS_WIDTH = 10;
    public static final int STATUS_GAP = 3;
    public static final int TANK_WIDTH = 18;
    public static final int ELECTRICAL_PANEL_GAP = 4;
    public static final int ELECTRICAL_PANEL_WIDTH = 112;
    public static final int ELECTRICAL_PANEL_TOP = 14;
    public static final int ELECTRICAL_PANEL_HEIGHT = 132;
    public static final int ELECTRICAL_PANEL_MARGIN = 4;

    public static final Point BATTERY_INPUT = new Point(103, 16);
    public static final Point BATTERY_OUTPUT = new Point(103, 48);
    public static final Point ELECTRIC_FURNACE_INPUT = new Point(108, 17);
    public static final Point ELECTRIC_FURNACE_OUTPUT = new Point(108, 49);

    private LegacyMachineGuiLayout() {
    }

    public static Size singleBlockSize(SingleBlockMachineDefinition definition) {
        return new Size(STANDARD_WIDTH, definition == SingleBlockMachineDefinition.BOX
                ? BOX_HEIGHT
                : STANDARD_HEIGHT);
    }

    public static int singleBlockPlayerTop(SingleBlockMachineDefinition definition) {
        return definition == SingleBlockMachineDefinition.BOX ? BOX_PLAYER_TOP : STANDARD_PLAYER_TOP;
    }

    public static Size withElectricalPanel(Size base) {
        return withElectricalPanel(base, ELECTRICAL_PANEL_WIDTH);
    }

    public static Size withElectricalPanel(Size base, int panelWidth) {
        return new Size(
                base.width() + ELECTRICAL_PANEL_GAP + panelWidth + ELECTRICAL_PANEL_MARGIN,
                Math.max(base.height(), ELECTRICAL_PANEL_TOP + ELECTRICAL_PANEL_HEIGHT + ELECTRICAL_PANEL_MARGIN)
        );
    }

    public static Rect electricalPanel(int baseWidth) {
        return electricalPanel(baseWidth, ELECTRICAL_PANEL_WIDTH);
    }

    public static Rect electricalPanel(int baseWidth, int panelWidth) {
        return new Rect(
                baseWidth + ELECTRICAL_PANEL_GAP,
                ELECTRICAL_PANEL_TOP,
                panelWidth,
                ELECTRICAL_PANEL_HEIGHT
        );
    }

    public static List<Point> singleBlockSlots(SingleBlockMachineDefinition definition) {
        List<Point> positions = new ArrayList<>();
        switch (definition) {
            case BOX -> addGrid(positions, 3, 9, 8, 8);
            case FABRICATOR -> {
                addGrid(positions, 3, 3, 24, 5);
                positions.add(new Point(80, 23));
                addGrid(positions, 3, 3, 100, 5);
            }
            case INSERTER -> {
                positions.add(new Point(89, 32));
                positions.add(new Point(79, 5));
                positions.add(new Point(97, 5));
                addGrid(positions, 3, 3, 23, 5);
            }
            case BLOCK_BREAKER -> {
                addGrid(positions, 3, 3, 46, 5);
                addGrid(positions, 3, 3, 105, 5);
            }
            case RELAY, FILTER, TRANSPOSER -> addGrid(positions, 3, 3, 62, 5);
            case COMBUSTION_CHAMBER -> positions.add(new Point(94, 32));
            case GASIFICATION_UNIT -> {
                positions.add(new Point(91, 17));
                positions.add(new Point(91, 49));
            }
            case BRICK_FURNACE -> {
                positions.add(new Point(101, 17));
                positions.add(new Point(101, 49));
            }
            default -> {
            }
        }
        return List.copyOf(positions);
    }

    public static Size multiblockSize(MultiblockDefinition definition) {
        return definition == MultiblockDefinition.SHELVING_UNIT
                ? new Size(SHELVING_WIDTH, SHELVING_HEIGHT)
                : new Size(STANDARD_WIDTH, STANDARD_HEIGHT);
    }

    public static int multiblockPlayerTop(MultiblockDefinition definition) {
        return definition == MultiblockDefinition.SHELVING_UNIT
                ? SHELVING_PLAYER_TOP
                : STANDARD_PLAYER_TOP;
    }

    public static List<Point> multiblockSlots(MultiblockDefinition definition) {
        return switch (definition) {
            case GRINDER -> List.of(new Point(108, 17), new Point(97, 49), new Point(119, 49));
            case SIEVE -> List.of(
                    new Point(108, 17),
                    new Point(86, 49),
                    new Point(108, 49),
                    new Point(130, 49)
            );
            case HYDRAULIC_PRESS -> List.of(new Point(98, 17), new Point(98, 49));
            case POLYMERIZER -> List.of(new Point(98, 17), new Point(98, 49));
            case BIG_ELECTRIC_FURNACE -> List.of(new Point(108, 17), new Point(108, 49));
            case BIG_COMBUSTION_CHAMBER -> List.of(new Point(104, 33));
            case STIRLING_GENERATOR -> List.of(new Point(104, 33));
            default -> List.of();
        };
    }

    public static Size programmableSize(boolean miningRobot) {
        return new Size(PROGRAMMABLE_WIDTH, miningRobot ? MINING_ROBOT_HEIGHT : COMPUTER_HEIGHT);
    }

    public static int programmablePlayerTop(boolean miningRobot) {
        return miningRobot ? MINING_ROBOT_PLAYER_TOP : COMPUTER_PLAYER_TOP;
    }

    public static List<Point> miningRobotSlots() {
        List<Point> positions = new ArrayList<>(16);
        addGrid(positions, 4, 4, MINING_ROBOT_INVENTORY_LEFT, MINING_ROBOT_PLAYER_TOP);
        return List.copyOf(positions);
    }

    public static List<Rect> programmableEditorRows() {
        List<Rect> rows = new ArrayList<>(8);
        for (int row = 0; row < 8; row++) {
            rows.add(new Rect(
                    PROGRAMMABLE_EDITOR_LEFT,
                    PROGRAMMABLE_EDITOR_TOP + row * 12,
                    PROGRAMMABLE_EDITOR_WIDTH,
                    PROGRAMMABLE_EDITOR_HEIGHT
            ));
        }
        return List.copyOf(rows);
    }

    public static List<Rect> programmableButtons() {
        return List.of(
                new Rect(8, PROGRAMMABLE_BUTTON_TOP, 20, 18),
                new Rect(32, PROGRAMMABLE_BUTTON_TOP, 20, 18),
                new Rect(56, PROGRAMMABLE_BUTTON_TOP, 20, 18),
                new Rect(80, PROGRAMMABLE_BUTTON_TOP, 20, 18)
        );
    }

    public static List<StatusBar> singleBlockStatusBars(
            boolean energy,
            boolean primaryFluid,
            boolean secondaryFluid,
            boolean progress
    ) {
        StatusBarBuilder builder = new StatusBarBuilder();
        builder.addIf(energy, StatusKind.ENERGY, 0, STATUS_WIDTH);
        builder.addIf(primaryFluid, StatusKind.PRIMARY_FLUID, 0, STATUS_WIDTH);
        builder.addIf(secondaryFluid, StatusKind.SECONDARY_FLUID, 0, STATUS_WIDTH);
        builder.addIf(progress, StatusKind.PROGRESS, 0, STATUS_WIDTH);
        return builder.build();
    }

    public static List<StatusBar> multiblockStatusBars(
            boolean energy,
            boolean progress,
            boolean bulk,
            int tankCount
    ) {
        StatusBarBuilder builder = new StatusBarBuilder();
        builder.addIf(energy, StatusKind.ENERGY, 0, STATUS_WIDTH);
        builder.addIf(progress, StatusKind.PROGRESS, 0, STATUS_WIDTH);
        builder.addIf(bulk, StatusKind.BULK, 0, STATUS_WIDTH);
        for (int tank = 0; tank < tankCount; tank++) {
            builder.addIf(true, StatusKind.TANK, tank, TANK_WIDTH);
        }
        return builder.build();
    }

    public static List<Rect> playerInventorySlots(int left, int top) {
        List<Rect> slots = new ArrayList<>(36);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slots.add(slotBounds(new Point(left + column * 18, top + row * 18)));
            }
        }
        for (int column = 0; column < 9; column++) {
            slots.add(slotBounds(new Point(left + column * 18, top + 58)));
        }
        return List.copyOf(slots);
    }

    public static Rect slotBounds(Point point) {
        return new Rect(point.x() - 1, point.y() - 1, 18, 18);
    }

    public static List<Rect> inserterButtons() {
        List<Rect> buttons = new ArrayList<>(6);
        for (int id = 0; id < 6; id++) {
            buttons.add(new Rect(122 + id % 2 * 20, 14 + id / 2 * 20, 18, 18));
        }
        return List.copyOf(buttons);
    }

    private static void addGrid(List<Point> positions, int rows, int columns, int left, int top) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                positions.add(new Point(left + column * 18, top + row * 18));
            }
        }
    }

    public record Point(int x, int y) {
    }

    public record Size(int width, int height) {
        public Rect bounds() {
            return new Rect(0, 0, width, height);
        }
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(Rect other) {
            return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
        }

        public boolean overlaps(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    public enum StatusKind {
        ENERGY,
        PRIMARY_FLUID,
        SECONDARY_FLUID,
        PROGRESS,
        BULK,
        TANK
    }

    public record StatusBar(StatusKind kind, int index, Rect bounds) {
    }

    private static final class StatusBarBuilder {
        private final List<StatusBar> bars = new ArrayList<>();
        private int left = 8;

        private void addIf(boolean condition, StatusKind kind, int index, int width) {
            if (!condition) {
                return;
            }
            bars.add(new StatusBar(kind, index, new Rect(left, STATUS_TOP, width, STATUS_HEIGHT)));
            left += width + STATUS_GAP;
        }

        private List<StatusBar> build() {
            return List.copyOf(bars);
        }
    }
}
