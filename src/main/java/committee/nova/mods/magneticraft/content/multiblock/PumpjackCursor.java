package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Deterministic, world-independent traversal for the pumpjack's bounded searches.
 * X advances first, then Z, then Y; descending cursors visit the highest Y layer first.
 */
final class PumpjackCursor {
    static final int INITIAL_SEARCH_RADIUS = 3;
    static final int FIELD_SEARCH_RADIUS = 32;
    static final int EXTRACTION_INTERVAL_TICKS = 20;

    static final int DEPOSIT_SCAN_MAX_POSITIONS = 65 * 65 * 12;
    static final int SOURCE_SCAN_MAX_POSITIONS = 65 * 65 * 31;
    static final int MAX_VERTICAL_SPAN = 1 << 12;
    static final int INITIAL_SEARCH_MAX_POSITIONS = 7 * 7 * MAX_VERTICAL_SPAN;
    static final int DIGGING_MAX_POSITIONS = MAX_VERTICAL_SPAN;

    private final BlockPos min;
    private final BlockPos max;
    private final boolean descendingY;
    private final int sizeX;
    private final int sizeZ;
    private final int layerSize;
    private final int totalPositions;

    private PumpjackCursor(BlockPos min, BlockPos max, boolean descendingY) {
        this.min = min.immutable();
        this.max = max.immutable();
        this.descendingY = descendingY;
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            throw new IllegalArgumentException("Pumpjack cursor bounds must not be inverted");
        }

        sizeX = Math.addExact(Math.subtractExact(max.getX(), min.getX()), 1);
        int sizeY = Math.addExact(Math.subtractExact(max.getY(), min.getY()), 1);
        sizeZ = Math.addExact(Math.subtractExact(max.getZ(), min.getZ()), 1);
        layerSize = Math.multiplyExact(sizeX, sizeZ);
        totalPositions = Math.multiplyExact(layerSize, sizeY);
    }

    static PumpjackCursor searchingOil(BlockPos searchOrigin, int minBuildHeight) {
        Objects.requireNonNull(searchOrigin, "searchOrigin");
        if (searchOrigin.getY() < minBuildHeight) {
            throw new IllegalArgumentException("Pumpjack oil search origin is below the build height");
        }
        validateVerticalSpan(minBuildHeight, Math.addExact(searchOrigin.getY(), 1));
        return new PumpjackCursor(
                new BlockPos(
                        Math.subtractExact(searchOrigin.getX(), INITIAL_SEARCH_RADIUS),
                        minBuildHeight,
                        Math.subtractExact(searchOrigin.getZ(), INITIAL_SEARCH_RADIUS)
                ),
                new BlockPos(
                        Math.addExact(searchOrigin.getX(), INITIAL_SEARCH_RADIUS),
                        searchOrigin.getY(),
                        Math.addExact(searchOrigin.getZ(), INITIAL_SEARCH_RADIUS)
                ),
                true
        );
    }

    static PumpjackCursor depositScan(
            BlockPos depositOrigin,
            int minBuildHeight,
            int maxBuildHeightExclusive
    ) {
        Objects.requireNonNull(depositOrigin, "depositOrigin");
        validateWorldPosition(depositOrigin, minBuildHeight, maxBuildHeightExclusive);
        return centeredScan(
                depositOrigin,
                depositOrigin,
                -7,
                4,
                minBuildHeight,
                maxBuildHeightExclusive
        );
    }

    static PumpjackCursor sourceScan(
            BlockPos searchOrigin,
            BlockPos depositOrigin,
            int minBuildHeight,
            int maxBuildHeightExclusive
    ) {
        Objects.requireNonNull(searchOrigin, "searchOrigin");
        Objects.requireNonNull(depositOrigin, "depositOrigin");
        validateWorldPosition(searchOrigin, minBuildHeight, maxBuildHeightExclusive);
        validateWorldPosition(depositOrigin, minBuildHeight, maxBuildHeightExclusive);
        return centeredScan(
                searchOrigin,
                depositOrigin,
                -10,
                20,
                minBuildHeight,
                maxBuildHeightExclusive
        );
    }

    static PumpjackCursor digging(BlockPos drillOrigin, int targetY) {
        Objects.requireNonNull(drillOrigin, "drillOrigin");
        if (targetY > drillOrigin.getY()) {
            throw new IllegalArgumentException("Pumpjack drill target must not be above the drill origin");
        }
        if ((long) drillOrigin.getY() - targetY + 1L > MAX_VERTICAL_SPAN) {
            throw new IllegalArgumentException("Pumpjack drill path exceeds the supported vertical span");
        }
        return new PumpjackCursor(
                new BlockPos(drillOrigin.getX(), targetY, drillOrigin.getZ()),
                drillOrigin,
                true
        );
    }

    BlockPos min() {
        return min;
    }

    BlockPos max() {
        return max;
    }

    boolean descendingY() {
        return descendingY;
    }

    int totalPositions() {
        return totalPositions;
    }

    boolean containsIndex(int index) {
        return index >= 0 && index < totalPositions;
    }

    BlockPos positionAt(int index) {
        if (!containsIndex(index)) {
            throw new IndexOutOfBoundsException(
                    "Pumpjack cursor index " + index + " outside [0, " + totalPositions + ")"
            );
        }
        int yOffset = index / layerSize;
        int inLayer = index % layerSize;
        int zOffset = inLayer / sizeX;
        int xOffset = inLayer % sizeX;
        int y = descendingY ? max.getY() - yOffset : min.getY() + yOffset;
        return new BlockPos(min.getX() + xOffset, y, min.getZ() + zOffset);
    }

    static int maximumCursorIndex(PumpjackState.Phase phase) {
        return switch (Objects.requireNonNull(phase, "phase")) {
            case SEARCHING_OIL -> INITIAL_SEARCH_MAX_POSITIONS;
            case SEARCHING_DEPOSIT -> DEPOSIT_SCAN_MAX_POSITIONS;
            case DIGGING -> DIGGING_MAX_POSITIONS;
            case SEARCHING_SOURCE -> SOURCE_SCAN_MAX_POSITIONS;
            case EXTRACTING -> 0;
        };
    }

    private static PumpjackCursor centeredScan(
            BlockPos horizontalOrigin,
            BlockPos verticalOrigin,
            int minYOffset,
            int maxYOffset,
            int minBuildHeight,
            int maxBuildHeightExclusive
    ) {
        int minY = clampY(
                (long) verticalOrigin.getY() + minYOffset,
                minBuildHeight,
                maxBuildHeightExclusive
        );
        int maxY = clampY(
                (long) verticalOrigin.getY() + maxYOffset,
                minBuildHeight,
                maxBuildHeightExclusive
        );
        return new PumpjackCursor(
                new BlockPos(
                        Math.subtractExact(horizontalOrigin.getX(), FIELD_SEARCH_RADIUS),
                        minY,
                        Math.subtractExact(horizontalOrigin.getZ(), FIELD_SEARCH_RADIUS)
                ),
                new BlockPos(
                        Math.addExact(horizontalOrigin.getX(), FIELD_SEARCH_RADIUS),
                        maxY,
                        Math.addExact(horizontalOrigin.getZ(), FIELD_SEARCH_RADIUS)
                ),
                false
        );
    }

    private static void validateWorldPosition(
            BlockPos position,
            int minBuildHeight,
            int maxBuildHeightExclusive
    ) {
        validateVerticalSpan(minBuildHeight, maxBuildHeightExclusive);
        if (position.getY() < minBuildHeight || position.getY() >= maxBuildHeightExclusive) {
            throw new IllegalArgumentException("Pumpjack cursor origin is outside the build height");
        }
    }

    private static void validateVerticalSpan(int minBuildHeight, int maxBuildHeightExclusive) {
        long height = (long) maxBuildHeightExclusive - minBuildHeight;
        if (height <= 0L || height > MAX_VERTICAL_SPAN) {
            throw new IllegalArgumentException("Unsupported pumpjack build height span: " + height);
        }
    }

    private static int clampY(long value, int minBuildHeight, int maxBuildHeightExclusive) {
        validateVerticalSpan(minBuildHeight, maxBuildHeightExclusive);
        return (int) Math.max(minBuildHeight, Math.min((long) maxBuildHeightExclusive - 1L, value));
    }
}
