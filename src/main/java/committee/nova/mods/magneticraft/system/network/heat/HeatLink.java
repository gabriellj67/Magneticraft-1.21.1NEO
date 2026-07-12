package committee.nova.mods.magneticraft.system.network.heat;

/**
 * Lossless conductive heat exchange across one edge for one server tick.
 */
public final class HeatLink {
    private static final double EPSILON = 1.0E-9;

    private HeatLink() {
    }

    public static Transfer transfer(HeatNode first, HeatNode second, double distance, double maxWatts) {
        if (!(distance > 0.0) || maxWatts < 0.0) {
            throw new IllegalArgumentException("Invalid heat edge limits");
        }
        HeatNode source = first.temperatureKelvin() >= second.temperatureKelvin() ? first : second;
        HeatNode target = source == first ? second : first;
        double difference = source.temperatureKelvin() - target.temperatureKelvin();
        if (difference <= EPSILON || maxWatts <= EPSILON) {
            return Transfer.ZERO;
        }

        double conductivity = harmonic(source.conductivity(), target.conductivity());
        double conductive = conductivity * difference / distance;
        double equilibrium = difference
                / (1.0 / source.heatCapacityJoulesPerKelvin() + 1.0 / target.heatCapacityJoulesPerKelvin());
        double moved = Math.min(Math.min(conductive, maxWatts), equilibrium);
        moved = source.removeHeat(moved, false);
        target.addHeat(moved, false);
        return new Transfer(moved, source == first);
    }

    private static double harmonic(double first, double second) {
        if (first <= EPSILON || second <= EPSILON) {
            return 0.0;
        }
        return 1.0 / (1.0 / first + 1.0 / second);
    }

    public record Transfer(double movedJoules, boolean firstWasSource) {
        public static final Transfer ZERO = new Transfer(0.0, true);

        public boolean moved() {
            return movedJoules > EPSILON;
        }
    }
}
