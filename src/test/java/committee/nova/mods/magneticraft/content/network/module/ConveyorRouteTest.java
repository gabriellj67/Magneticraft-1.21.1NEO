package committee.nova.mods.magneticraft.content.network.module;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConveyorRouteTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void preservesTheEightLegacyRouteOrdinals() {
        assertEquals(
                java.util.List.of(
                        ConveyorRoute.LEFT_FORWARD,
                        ConveyorRoute.RIGHT_FORWARD,
                        ConveyorRoute.LEFT_SHORT,
                        ConveyorRoute.LEFT_LONG,
                        ConveyorRoute.RIGHT_SHORT,
                        ConveyorRoute.RIGHT_LONG,
                        ConveyorRoute.LEFT_CORNER,
                        ConveyorRoute.RIGHT_CORNER
                ),
                java.util.List.of(ConveyorRoute.values())
        );
    }

    @Test
    void resolvesStraightTurnsAndCornersFromTheIncomingLane() {
        assertRoute(
                ConveyorRoute.LEFT_FORWARD,
                ConveyorRoute.IncomingDirection.SAME,
                ConveyorRoute.LEFT_LONG,
                false
        );
        assertRoute(
                ConveyorRoute.RIGHT_FORWARD,
                ConveyorRoute.IncomingDirection.SAME,
                ConveyorRoute.RIGHT_CORNER,
                true
        );
        assertTrue(ConveyorRoute.resolveIncoming(
                ConveyorRoute.IncomingDirection.OPPOSITE,
                ConveyorRoute.LEFT_FORWARD,
                false
        ).isEmpty());

        assertRoute(ConveyorRoute.LEFT_SHORT, ConveyorRoute.IncomingDirection.CLOCKWISE, ConveyorRoute.LEFT_FORWARD, false);
        assertRoute(ConveyorRoute.LEFT_LONG, ConveyorRoute.IncomingDirection.CLOCKWISE, ConveyorRoute.RIGHT_FORWARD, false);
        assertRoute(ConveyorRoute.LEFT_SHORT, ConveyorRoute.IncomingDirection.CLOCKWISE, ConveyorRoute.LEFT_FORWARD, true);
        assertRoute(ConveyorRoute.RIGHT_CORNER, ConveyorRoute.IncomingDirection.CLOCKWISE, ConveyorRoute.RIGHT_FORWARD, true);

        assertRoute(ConveyorRoute.RIGHT_LONG, ConveyorRoute.IncomingDirection.COUNTER_CLOCKWISE, ConveyorRoute.LEFT_FORWARD, false);
        assertRoute(ConveyorRoute.RIGHT_SHORT, ConveyorRoute.IncomingDirection.COUNTER_CLOCKWISE, ConveyorRoute.RIGHT_FORWARD, false);
        assertRoute(ConveyorRoute.LEFT_CORNER, ConveyorRoute.IncomingDirection.COUNTER_CLOCKWISE, ConveyorRoute.LEFT_FORWARD, true);
        assertRoute(ConveyorRoute.RIGHT_SHORT, ConveyorRoute.IncomingDirection.COUNTER_CLOCKWISE, ConveyorRoute.RIGHT_FORWARD, true);
    }

    @Test
    void usesTheLegacyPathSpeedsAndPixelGeometry() {
        assertEquals(2, ConveyorRoute.LEFT_SHORT.speedPixelsPerTick());
        assertEquals(2, ConveyorRoute.RIGHT_SHORT.speedPixelsPerTick());
        for (ConveyorRoute route : ConveyorRoute.values()) {
            if (route != ConveyorRoute.LEFT_SHORT && route != ConveyorRoute.RIGHT_SHORT) {
                assertEquals(1, route.speedPixelsPerTick(), route.name());
            }
        }

        assertPosition(ConveyorRoute.LEFT_FORWARD, 0, 5, 16);
        assertPosition(ConveyorRoute.LEFT_FORWARD, 16, 5, 0);
        assertPosition(ConveyorRoute.RIGHT_FORWARD, 0, 11, 16);
        assertPosition(ConveyorRoute.RIGHT_FORWARD, 16, 11, 0);

        assertPosition(ConveyorRoute.LEFT_SHORT, 0, 0, 5);
        assertPosition(ConveyorRoute.LEFT_SHORT, 8, 5, 5);
        assertPosition(ConveyorRoute.LEFT_SHORT, 16, 5, 0);
        assertPosition(ConveyorRoute.RIGHT_SHORT, 0, 16, 5);
        assertPosition(ConveyorRoute.RIGHT_SHORT, 8, 11, 5);
        assertPosition(ConveyorRoute.RIGHT_SHORT, 16, 11, 0);

        assertPosition(ConveyorRoute.LEFT_LONG, 0, 0, 11);
        assertPosition(ConveyorRoute.LEFT_LONG, 5, 5, 11);
        assertPosition(ConveyorRoute.LEFT_LONG, 16, 5, 0);
        assertPosition(ConveyorRoute.RIGHT_LONG, 0, 16, 11);
        assertPosition(ConveyorRoute.RIGHT_LONG, 5, 11, 11);
        assertPosition(ConveyorRoute.RIGHT_LONG, 16, 11, 0);

        assertPosition(ConveyorRoute.LEFT_CORNER, 0, 16, 11);
        assertPosition(ConveyorRoute.LEFT_CORNER, 8, 5, 11);
        assertPosition(ConveyorRoute.LEFT_CORNER, 16, 5, 0);
        assertPosition(ConveyorRoute.RIGHT_CORNER, 0, 0, 11);
        assertPosition(ConveyorRoute.RIGHT_CORNER, 8, 11, 11);
        assertPosition(ConveyorRoute.RIGHT_CORNER, 16, 11, 0);
    }

    @Test
    void clampsProgressAndFallsBackFromUnknownPersistedOrdinals() {
        assertPosition(ConveyorRoute.LEFT_FORWARD, -5, 5, 16);
        assertPosition(ConveyorRoute.LEFT_FORWARD, 30, 5, 0);
        assertEquals(
                ConveyorRoute.RIGHT_FORWARD,
                ConveyorRoute.byOrdinalOrDefault(-1, ConveyorRoute.RIGHT_FORWARD)
        );
        assertEquals(
                ConveyorRoute.LEFT_LONG,
                ConveyorRoute.byOrdinalOrDefault(ConveyorRoute.LEFT_LONG.ordinal(), ConveyorRoute.RIGHT_FORWARD)
        );
    }

    private static void assertRoute(
            ConveyorRoute expected,
            ConveyorRoute.IncomingDirection incoming,
            ConveyorRoute previous,
            boolean corner
    ) {
        assertEquals(Optional.of(expected), ConveyorRoute.resolveIncoming(incoming, previous, corner));
    }

    private static void assertPosition(ConveyorRoute route, double progress, double x, double z) {
        ConveyorRoute.PixelPosition position = route.position(progress);
        assertEquals(x, position.x(), EPSILON, route.name() + " x at " + progress);
        assertEquals(z, position.z(), EPSILON, route.name() + " z at " + progress);
    }
}
