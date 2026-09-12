package committee.nova.mods.magneticraft.system.network.kinetic;

/** Lossless equalisation toward a shared angular velocity. */
public final class KineticLink {
    private static final double EPSILON = 1.0E-9D;

    private KineticLink() {
    }

    public static Transfer transfer(KineticNode first, KineticNode second, double maximumJoules) {
        double totalInertia = first.inertiaKgSquareMeters() + second.inertiaKgSquareMeters();
        double totalEnergy = first.energyJoules() + second.energyJoules();
        double firstTarget = totalEnergy * first.inertiaKgSquareMeters() / totalInertia;
        double difference = first.energyJoules() - firstTarget;
        double limit = Double.isFinite(maximumJoules) ? Math.max(0.0D, maximumJoules) : 0.0D;
        double requested = Math.min(Math.abs(difference), limit);
        if (requested <= EPSILON) {
            return Transfer.ZERO;
        }

        KineticNode source = difference > 0.0D ? first : second;
        KineticNode target = difference > 0.0D ? second : first;
        double moved = Math.min(
                source.extractJoules(requested, true),
                target.insertJoules(requested, true)
        );
        if (moved <= EPSILON) {
            return Transfer.ZERO;
        }
        source.extractJoules(moved, false);
        target.insertJoules(moved, false);
        return new Transfer(true, moved);
    }

    public record Transfer(boolean moved, double joules) {
        public static final Transfer ZERO = new Transfer(false, 0.0D);
    }
}
