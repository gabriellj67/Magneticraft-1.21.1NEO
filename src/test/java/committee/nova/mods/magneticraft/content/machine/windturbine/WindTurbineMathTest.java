package committee.nova.mods.magneticraft.content.machine.windturbine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindTurbineMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void bladePlaneMatchesLegacyDisc() {
        assertEquals(109, WindTurbineMath.bladeCells().size());
        assertEquals(
                WindTurbineMath.bladeCells().size(),
                new HashSet<>(WindTurbineMath.bladeCells()).size()
        );
        assertTrue(WindTurbineMath.bladeCells().contains(new WindTurbineMath.BladeCell(5, 3)));
        assertFalse(WindTurbineMath.bladeCells().contains(new WindTurbineMath.BladeCell(5, 4)));
        assertTrue(WindTurbineMath.bladeCells().stream().allMatch(cell ->
                cell.horizontal() * cell.horizontal() + cell.vertical() * cell.vertical() < 35
        ));
    }

    @Test
    void openSpaceUsesSixteenStepsPerBladeCell() {
        int totalSteps = WindTurbineMath.bladeCells().size() * WindTurbineMath.CLEARANCE_DEPTH;
        assertEquals(0.0D, WindTurbineMath.openSpace(0), EPSILON);
        assertEquals(0.5D, WindTurbineMath.openSpace(totalSteps / 2), EPSILON);
        assertEquals(1.0D, WindTurbineMath.openSpace(totalSteps), EPSILON);
        assertEquals(1.0D, WindTurbineMath.openSpace(Integer.MAX_VALUE), EPSILON);
    }

    @Test
    void productionClampsEveryFactor() {
        assertEquals(40.0D, WindTurbineMath.productionJoulesPerTick(0.5D, 0.8D, 128), EPSILON);
        assertEquals(0.0D, WindTurbineMath.productionJoulesPerTick(1.0D, 1.0D, -64), EPSILON);
        assertEquals(200.0D, WindTurbineMath.productionJoulesPerTick(2.0D, 3.0D, 400), EPSILON);
        assertEquals(0.0D, WindTurbineMath.productionJoulesPerTick(Double.NaN, 1.0D, 256), EPSILON);
    }

    @Test
    void rotorTiersScaleSweptAreaAndRatedOutput() {
        assertTrue(WindTurbineMath.bladeCells(WindTurbineRotorTier.SMALL).size()
                < WindTurbineMath.bladeCells(WindTurbineRotorTier.MEDIUM).size());
        assertTrue(WindTurbineMath.bladeCells(WindTurbineRotorTier.MEDIUM).size()
                < WindTurbineMath.bladeCells(WindTurbineRotorTier.LARGE).size());
        assertEquals(66.0D, WindTurbineMath.productionJoulesPerTick(
                1.0D, 1.0D, 256, WindTurbineRotorTier.SMALL), EPSILON);
        assertEquals(200.0D, WindTurbineMath.productionJoulesPerTick(
                1.0D, 1.0D, 256, WindTurbineRotorTier.MEDIUM), EPSILON);
        assertEquals(400.0D, WindTurbineMath.productionJoulesPerTick(
                1.0D, 1.0D, 256, WindTurbineRotorTier.LARGE), EPSILON);
    }

    @Test
    void windMovesOnePercentTowardBoundedTarget() {
        assertEquals(0.01D, WindTurbineMath.smoothWind(0.0D, 1.0D), EPSILON);
        assertEquals(0.99D, WindTurbineMath.smoothWind(1.0D, 0.0D), EPSILON);
        assertEquals(0.01D, WindTurbineMath.smoothWind(Double.NaN, 2.0D), EPSILON);
    }

    @Test
    void targetWindIsDeterministicAndBounded() {
        double first = WindTurbineMath.targetWind(32, 128, -48, 12_345L);
        double second = WindTurbineMath.targetWind(32, 128, -48, 12_345L);
        assertEquals(first, second, EPSILON);
        assertEquals(0.4328541157010336D, first, EPSILON);
        assertTrue(first >= 0.0D && first <= 1.0D);
        for (long time = 0; time <= 100_000L; time += 977L) {
            double value = WindTurbineMath.targetWind(-2_000, 320, 4_000, time);
            assertTrue(value >= 0.0D && value <= 1.0D);
        }
    }

    @Test
    void rotationViewIsBoundedAndStopsWhenUnavailable() {
        assertEquals(0.0D, WindTurbineMath.rotationSpeed(1.0D, 1.0D, false), EPSILON);
        assertEquals(1.25D, WindTurbineMath.rotationSpeed(0.5D, 0.5D, true), EPSILON);
        assertEquals(WindTurbineMath.MAX_ROTATION_SPEED,
                WindTurbineMath.rotationSpeed(2.0D, 2.0D, true), EPSILON);
    }
}
