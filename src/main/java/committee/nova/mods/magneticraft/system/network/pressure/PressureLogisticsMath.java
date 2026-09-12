package committee.nova.mods.magneticraft.system.network.pressure;

/**
 * Loader-independent pneumatic transport curves.
 */
public final class PressureLogisticsMath {
    private PressureLogisticsMath() {
    }

    public static double progressPerTick(double pressureKpa) {
        double pressure = Math.max(0.0D, Double.isFinite(pressureKpa) ? pressureKpa : 0.0D);
        if (pressure <= 100.0D) {
            return 2.0D + pressure * 0.06D;
        }
        if (pressure <= 400.0D) {
            return 8.0D + (pressure - 100.0D) * (8.0D / 300.0D);
        }
        return 16.0D;
    }

    public static double segmentCostKpaLiters(int itemCount) {
        return 0.25D * Math.ceil(Math.max(0, itemCount) / 8.0D);
    }
}
