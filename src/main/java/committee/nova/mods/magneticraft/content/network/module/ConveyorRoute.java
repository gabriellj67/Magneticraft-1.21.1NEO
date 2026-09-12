package committee.nova.mods.magneticraft.content.network.module;

import java.util.Optional;

/**
 * Loader-independent horizontal conveyor path geometry reconstructed from the
 * released Nova implementation. Coordinates use one sixteenth of a block.
 */
public enum ConveyorRoute {
    LEFT_FORWARD(true, true, false),
    RIGHT_FORWARD(false, true, false),
    LEFT_SHORT(true, false, true),
    LEFT_LONG(true, false, false),
    RIGHT_SHORT(false, false, true),
    RIGHT_LONG(false, false, false),
    LEFT_CORNER(true, false, false),
    RIGHT_CORNER(false, false, false);

    private static final double PATH_LENGTH = 16.0D;

    private final boolean leftSide;
    private final boolean straight;
    private final boolean shortPath;

    ConveyorRoute(boolean leftSide, boolean straight, boolean shortPath) {
        this.leftSide = leftSide;
        this.straight = straight;
        this.shortPath = shortPath;
    }

    public boolean leftSide() {
        return leftSide;
    }

    public int speedPixelsPerTick() {
        return shortPath ? 2 : 1;
    }

    public PixelPosition position(double progress) {
        double value = Math.max(0.0D, Math.min(PATH_LENGTH, progress));
        if (straight) {
            return new PixelPosition(leftSide ? 5.0D : 11.0D, PATH_LENGTH - value);
        }
        return leftSide ? leftPosition(value) : rightPosition(value);
    }

    /**
     * Resolves the receiver-local route for a parcel arriving from another
     * horizontal conveyor. Opposite-facing conveyors are intentionally
     * incompatible.
     */
    public static Optional<ConveyorRoute> resolveIncoming(
            IncomingDirection incoming,
            ConveyorRoute previous,
            boolean corner
    ) {
        return switch (incoming) {
            case SAME -> Optional.of(previous.leftSide ? LEFT_FORWARD : RIGHT_FORWARD);
            case OPPOSITE -> Optional.empty();
            case CLOCKWISE -> Optional.of(corner
                    ? previous.leftSide ? LEFT_SHORT : RIGHT_CORNER
                    : previous.leftSide ? LEFT_SHORT : LEFT_LONG);
            case COUNTER_CLOCKWISE -> Optional.of(corner
                    ? previous.leftSide ? LEFT_CORNER : RIGHT_SHORT
                    : previous.leftSide ? RIGHT_LONG : RIGHT_SHORT);
        };
    }

    public static ConveyorRoute byOrdinalOrDefault(int ordinal, ConveyorRoute fallback) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : fallback;
    }

    private PixelPosition leftPosition(double progress) {
        if (shortPath) {
            return progress < 8.0D
                    ? new PixelPosition(interpolate(0.0D, 5.0D, progress / 8.0D), 5.0D)
                    : new PixelPosition(5.0D, interpolate(5.0D, 0.0D, (progress - 8.0D) / 8.0D));
        }
        if (this == LEFT_CORNER) {
            return progress < 8.0D
                    ? new PixelPosition(interpolate(16.0D, 5.0D, progress / 8.0D), 11.0D)
                    : new PixelPosition(5.0D, interpolate(11.0D, 0.0D, (progress - 8.0D) / 8.0D));
        }
        return progress < 5.0D
                ? new PixelPosition(interpolate(0.0D, 5.0D, progress / 5.0D), 11.0D)
                : new PixelPosition(5.0D, interpolate(11.0D, 0.0D, (progress - 5.0D) / 11.0D));
    }

    private PixelPosition rightPosition(double progress) {
        if (shortPath) {
            return progress < 8.0D
                    ? new PixelPosition(interpolate(16.0D, 11.0D, progress / 8.0D), 5.0D)
                    : new PixelPosition(11.0D, interpolate(5.0D, 0.0D, (progress - 8.0D) / 8.0D));
        }
        if (this == RIGHT_CORNER) {
            return progress < 8.0D
                    ? new PixelPosition(interpolate(0.0D, 11.0D, progress / 8.0D), 11.0D)
                    : new PixelPosition(11.0D, interpolate(11.0D, 0.0D, (progress - 8.0D) / 8.0D));
        }
        return progress < 5.0D
                ? new PixelPosition(interpolate(16.0D, 11.0D, progress / 5.0D), 11.0D)
                : new PixelPosition(11.0D, interpolate(11.0D, 0.0D, (progress - 5.0D) / 11.0D));
    }

    private static double interpolate(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public enum IncomingDirection {
        SAME,
        OPPOSITE,
        CLOCKWISE,
        COUNTER_CLOCKWISE
    }

    public record PixelPosition(double x, double z) {
    }
}
