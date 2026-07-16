package committee.nova.mods.magneticraft.system.network.pressure;

/**
 * Fixed 293.15 K boundary conversion between Forge millibuckets and kPa·L.
 */
public final class PressureFluidConversion {
    public static final double KPA_LITERS_PER_MILLIBUCKET = 0.2976D;

    private PressureFluidConversion() {
    }

    public static double toGasKpaLiters(int millibuckets) {
        return Math.max(0, millibuckets) * KPA_LITERS_PER_MILLIBUCKET;
    }

    public static int wholeMillibuckets(double gasKpaLiters) {
        if (!Double.isFinite(gasKpaLiters) || gasKpaLiters <= 0.0D) {
            return 0;
        }
        return Math.max(0, (int) Math.floor(gasKpaLiters / KPA_LITERS_PER_MILLIBUCKET));
    }
}
