package committee.nova.mods.magneticraft.system.network.pressure;

/**
 * Gas amount expressed in kPa·L; pressure is derived from node volume.
 */
public final class PressureNode {
    private final double volumeLiters;
    private final double maxPressureKpa;
    private double gasKpaLiters;

    public PressureNode(double volumeLiters, double maxPressureKpa) {
        if (!(volumeLiters > 0.0) || !(maxPressureKpa > 0.0)) {
            throw new IllegalArgumentException("Invalid pressure node limits");
        }
        this.volumeLiters = volumeLiters;
        this.maxPressureKpa = maxPressureKpa;
    }

    public double pressureKpa() {
        return gasKpaLiters / volumeLiters;
    }

    public double gasKpaLiters() {
        return gasKpaLiters;
    }

    public double volumeLiters() {
        return volumeLiters;
    }

    public double maxPressureKpa() {
        return maxPressureKpa;
    }

    public double capacityKpaLiters() {
        return volumeLiters * maxPressureKpa;
    }

    public double addGas(double amount, boolean simulate) {
        double accepted = Math.min(Math.max(0.0, finite(amount)), capacityKpaLiters() - gasKpaLiters);
        if (!simulate) {
            gasKpaLiters += accepted;
        }
        return accepted;
    }

    public double removeGas(double amount, boolean simulate) {
        double removed = Math.min(Math.max(0.0, finite(amount)), gasKpaLiters);
        if (!simulate) {
            gasKpaLiters -= removed;
        }
        return removed;
    }

    public void setGasKpaLiters(double amount) {
        gasKpaLiters = Math.max(0.0, Math.min(capacityKpaLiters(), finite(amount)));
    }

    public void setPressureKpa(double pressure) {
        setGasKpaLiters(Math.max(0.0, finite(pressure)) * volumeLiters);
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
