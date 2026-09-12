package committee.nova.mods.magneticraft.system.network.kinetic;

/**
 * Authoritative positive-direction rotary store. Energy is persisted in joules;
 * angular velocity is always derived from the configured inertia.
 */
public final class KineticNode {
    private final double inertiaKgSquareMeters;
    private final double capacityJoules;
    private double energyJoules;

    public KineticNode(double inertiaKgSquareMeters, double capacityJoules) {
        if (!Double.isFinite(inertiaKgSquareMeters) || inertiaKgSquareMeters <= 0.0D) {
            throw new IllegalArgumentException("Kinetic inertia must be positive and finite");
        }
        if (!Double.isFinite(capacityJoules) || capacityJoules <= 0.0D) {
            throw new IllegalArgumentException("Kinetic capacity must be positive and finite");
        }
        this.inertiaKgSquareMeters = inertiaKgSquareMeters;
        this.capacityJoules = capacityJoules;
    }

    public double inertiaKgSquareMeters() {
        return inertiaKgSquareMeters;
    }

    public double capacityJoules() {
        return capacityJoules;
    }

    public double energyJoules() {
        return energyJoules;
    }

    public double angularVelocityRadiansPerSecond() {
        return Math.sqrt(2.0D * energyJoules / inertiaKgSquareMeters);
    }

    public double revolutionsPerMinute() {
        return angularVelocityRadiansPerSecond() * 60.0D / (Math.PI * 2.0D);
    }

    public double insertJoules(double requestedJoules, boolean simulate) {
        double requested = finitePositive(requestedJoules);
        double accepted = Math.min(requested, capacityJoules - energyJoules);
        if (!simulate && accepted > 0.0D) {
            energyJoules += accepted;
        }
        return accepted;
    }

    public double extractJoules(double requestedJoules, boolean simulate) {
        double extracted = Math.min(finitePositive(requestedJoules), energyJoules);
        if (!simulate && extracted > 0.0D) {
            energyJoules -= extracted;
        }
        return extracted;
    }

    public void setEnergyJoules(double energyJoules) {
        this.energyJoules = Math.min(capacityJoules, finitePositive(energyJoules));
    }

    private static double finitePositive(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
