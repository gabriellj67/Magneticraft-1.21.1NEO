package committee.nova.mods.magneticraft.content.machine.windturbine;

import java.util.Random;

/** Exact one-octave 2D simplex sampler used by the released 1.12 wind turbine. */
final class LegacyWindNoise {
    private static final int[][] GRADIENTS = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };
    private static final double SQRT_THREE = Math.sqrt(3.0D);
    private static final double SKEW = 0.5D * (SQRT_THREE - 1.0D);
    private static final double UNSKEW = (3.0D - SQRT_THREE) / 6.0D;

    private final int[] permutation = new int[512];

    LegacyWindNoise(long seed) {
        Random random = new Random(seed);
        random.nextDouble();
        random.nextDouble();
        random.nextDouble();
        for (int index = 0; index < 256; index++) {
            permutation[index] = index;
        }
        for (int index = 0; index < 256; index++) {
            int swapIndex = random.nextInt(256 - index) + index;
            int value = permutation[index];
            permutation[index] = permutation[swapIndex];
            permutation[swapIndex] = value;
            permutation[index + 256] = permutation[index];
        }
    }

    double sample(double x, double y) {
        double skew = (x + y) * SKEW;
        int cellX = fastFloor(x + skew);
        int cellY = fastFloor(y + skew);
        double unskew = (cellX + cellY) * UNSKEW;
        double localX = x - (cellX - unskew);
        double localY = y - (cellY - unskew);

        int middleX = localX > localY ? 1 : 0;
        int middleY = localX > localY ? 0 : 1;
        double middleLocalX = localX - middleX + UNSKEW;
        double middleLocalY = localY - middleY + UNSKEW;
        double farLocalX = localX - 1.0D + 2.0D * UNSKEW;
        double farLocalY = localY - 1.0D + 2.0D * UNSKEW;

        int wrappedX = cellX & 255;
        int wrappedY = cellY & 255;
        int nearGradient = permutation[wrappedX + permutation[wrappedY]] % 12;
        int middleGradient = permutation[wrappedX + middleX + permutation[wrappedY + middleY]] % 12;
        int farGradient = permutation[wrappedX + 1 + permutation[wrappedY + 1]] % 12;
        return 70.0D * (
                contribution(nearGradient, localX, localY)
                        + contribution(middleGradient, middleLocalX, middleLocalY)
                        + contribution(farGradient, farLocalX, farLocalY)
        );
    }

    private static double contribution(int gradient, double x, double y) {
        double attenuation = 0.5D - x * x - y * y;
        if (attenuation < 0.0D) {
            return 0.0D;
        }
        attenuation *= attenuation;
        int[] vector = GRADIENTS[gradient];
        return attenuation * attenuation * (vector[0] * x + vector[1] * y);
    }

    private static int fastFloor(double value) {
        return value > 0.0D ? (int) value : (int) value - 1;
    }
}
