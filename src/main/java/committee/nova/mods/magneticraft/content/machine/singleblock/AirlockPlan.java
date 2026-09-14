package committee.nova.mods.magneticraft.content.machine.singleblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loader-independent snapshot and mutation plan for the released 1.12 airlock geometry.
 */
final class AirlockPlan {
    static final int SCAN_RANGE = 10;
    static final int ACTIVE_RADIUS_SQUARED = 9 * 9;

    private static final int CUBE_SIZE = SCAN_RANGE * 2 + 1;
    private static final int BUBBLE_COST_JOULES = 1;
    private static final int AIR_COST_JOULES = 2;
    private static final List<Offset> SCAN_OFFSETS = createOffsets(false);
    private static final List<Offset> ACTIVE_OFFSETS = createOffsets(true);

    private final List<Operation> operations;
    private final int totalCostJoules;

    private AirlockPlan(List<Operation> operations, int totalCostJoules) {
        this.operations = List.copyOf(operations);
        this.totalCostJoules = totalCostJoules;
    }

    static AirlockPlan build(CellLookup lookup) {
        Objects.requireNonNull(lookup, "lookup");
        Cell[] snapshot = new Cell[CUBE_SIZE * CUBE_SIZE * CUBE_SIZE];
        for (Offset offset : SCAN_OFFSETS) {
            snapshot[index(offset)] = Objects.requireNonNull(
                    lookup.cell(offset.x(), offset.y(), offset.z()),
                    "cell at " + offset
            );
        }

        List<Operation> operations = new ArrayList<>();
        int totalCost = 0;
        for (Offset offset : ACTIVE_OFFSETS) {
            Cell cell = snapshot[index(offset)];
            if (!cell.isWaterOrBubble()) {
                continue;
            }
            boolean boundary = hasOutsideWaterNeighbor(snapshot, offset);
            Target target = boundary ? Target.STABLE_BUBBLE : Target.AIR;
            int cost = boundary ? BUBBLE_COST_JOULES : AIR_COST_JOULES;
            operations.add(new Operation(offset, target, cost));
            totalCost += cost;
        }
        return new AirlockPlan(operations, totalCost);
    }

    List<Operation> operations() {
        return operations;
    }

    int totalCostJoules() {
        return totalCostJoules;
    }

    boolean canAfford(double availableJoules) {
        return Double.isFinite(availableJoules) && availableJoules >= totalCostJoules;
    }

    static List<Offset> scanOffsets() {
        return SCAN_OFFSETS;
    }

    static List<Offset> activeOffsets() {
        return ACTIVE_OFFSETS;
    }

    private static boolean hasOutsideWaterNeighbor(Cell[] snapshot, Offset offset) {
        return isOutsideWater(snapshot, offset.x() - 1, offset.y(), offset.z())
                || isOutsideWater(snapshot, offset.x() + 1, offset.y(), offset.z())
                || isOutsideWater(snapshot, offset.x(), offset.y() - 1, offset.z())
                || isOutsideWater(snapshot, offset.x(), offset.y() + 1, offset.z())
                || isOutsideWater(snapshot, offset.x(), offset.y(), offset.z() - 1)
                || isOutsideWater(snapshot, offset.x(), offset.y(), offset.z() + 1);
    }

    private static boolean isOutsideWater(Cell[] snapshot, int x, int y, int z) {
        Offset neighbor = new Offset(x, y, z);
        return neighbor.distanceSquared() > ACTIVE_RADIUS_SQUARED
                && snapshot[index(neighbor)] == Cell.WATER;
    }

    private static int index(Offset offset) {
        int x = offset.x() + SCAN_RANGE;
        int y = offset.y() + SCAN_RANGE;
        int z = offset.z() + SCAN_RANGE;
        return (y * CUBE_SIZE + z) * CUBE_SIZE + x;
    }

    private static List<Offset> createOffsets(boolean activeOnly) {
        List<Offset> offsets = new ArrayList<>();
        for (int y = -SCAN_RANGE; y <= SCAN_RANGE; y++) {
            for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z++) {
                for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x++) {
                    Offset offset = new Offset(x, y, z);
                    if (!activeOnly || offset.distanceSquared() <= ACTIVE_RADIUS_SQUARED) {
                        offsets.add(offset);
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    @FunctionalInterface
    interface CellLookup {
        Cell cell(int x, int y, int z);
    }

    enum Cell {
        AIR,
        WATER,
        STABLE_BUBBLE,
        DECAYING_BUBBLE,
        OTHER;

        boolean isWaterOrBubble() {
            return this == WATER || this == STABLE_BUBBLE || this == DECAYING_BUBBLE;
        }
    }

    enum Target {
        AIR,
        STABLE_BUBBLE
    }

    record Offset(int x, int y, int z) {
        int distanceSquared() {
            return x * x + y * y + z * z;
        }
    }

    record Operation(Offset offset, Target target, int costJoules) {
        Operation {
            Objects.requireNonNull(offset, "offset");
            Objects.requireNonNull(target, "target");
            if (costJoules <= 0) {
                throw new IllegalArgumentException("Airlock operation cost must be positive");
            }
        }
    }
}
