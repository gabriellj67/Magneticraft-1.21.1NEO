package committee.nova.mods.magneticraft.content.machine.windturbine;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader-independent wind, blade geometry and production rules.
 */
final class WindTurbineMath {
    static final int SCAN_INTERVAL_TICKS = 200;
    static final int CLEARANCE_DEPTH = 16;
    static final int MAX_HEIGHT = 256;
    static final double RATED_OUTPUT_JOULES_PER_TICK = 200.0D;
    static final double MAX_ROTATION_SPEED = 5.0D;
    static final double WIND_SMOOTHING_FACTOR = 0.01D;

    private static final int BLADE_RADIUS = 5;
    private static final int BLADE_RADIUS_SQUARED = 35;
    private static final LegacyWindNoise WIND_NOISE = new LegacyWindNoise(1_234L);
    private static final List<BladeCell> BLADE_CELLS = createBladeCells();

    private WindTurbineMath() {
    }

    static List<BladeCell> bladeCells() {
        return BLADE_CELLS;
    }

    static double openSpace(int openSteps) {
        int totalSteps = BLADE_CELLS.size() * CLEARANCE_DEPTH;
        return clamp01((double) Math.max(0, openSteps) / totalSteps);
    }

    static double productionJoulesPerTick(double openSpace, double currentWind, int heightY) {
        return RATED_OUTPUT_JOULES_PER_TICK
                * clamp01(openSpace)
                * clamp01(currentWind)
                * clamp01((double) heightY / MAX_HEIGHT);
    }

    static double smoothWind(double currentWind, double targetWind) {
        double current = clamp01(currentWind);
        double target = clamp01(targetWind);
        return clamp01(current + (target - current) * WIND_SMOOTHING_FACTOR);
    }

    static double targetWind(int x, int y, int z, long gameTime) {
        double spatial = WIND_NOISE.sample(x / 16.0D, z / 16.0D);
        double temporal = WIND_NOISE.sample((double) gameTime, y);
        return clamp01((spatial + temporal) * 0.25D + 0.5D);
    }

    static double rotationSpeed(double openSpace, double currentWind, boolean operational) {
        if (!operational) {
            return 0.0D;
        }
        return Math.min(MAX_ROTATION_SPEED, MAX_ROTATION_SPEED * clamp01(openSpace) * clamp01(currentWind));
    }

    static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static List<BladeCell> createBladeCells() {
        List<BladeCell> cells = new ArrayList<>();
        for (int horizontal = -BLADE_RADIUS; horizontal <= BLADE_RADIUS; horizontal++) {
            for (int vertical = -BLADE_RADIUS; vertical <= BLADE_RADIUS; vertical++) {
                if (horizontal * horizontal + vertical * vertical < BLADE_RADIUS_SQUARED) {
                    cells.add(new BladeCell(horizontal, vertical));
                }
            }
        }
        return List.copyOf(cells);
    }

    record BladeCell(int horizontal, int vertical) {
    }
}
