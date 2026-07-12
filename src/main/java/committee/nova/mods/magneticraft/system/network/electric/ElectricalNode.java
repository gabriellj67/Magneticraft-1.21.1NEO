package committee.nova.mods.magneticraft.system.network.electric;

/**
 * Electrical storage expressed as joules in a capacitive node.
 */
public final class ElectricalNode {
    private final double capacitance;
    private final double maxVoltage;
    private final double resistance;
    private double energyJoules;

    public ElectricalNode(double capacitance, double maxVoltage, double resistance) {
        if (!(capacitance > 0.0) || !(maxVoltage > 0.0) || resistance < 0.0) {
            throw new IllegalArgumentException("Invalid electrical node limits");
        }
        this.capacitance = capacitance;
        this.maxVoltage = maxVoltage;
        this.resistance = resistance;
    }

    public double voltage() {
        return Math.sqrt(energyJoules / capacitance);
    }

    public double energyJoules() {
        return energyJoules;
    }

    public double maxEnergyJoules() {
        return capacitance * maxVoltage * maxVoltage;
    }

    public double capacitance() {
        return capacitance;
    }

    public double maxVoltage() {
        return maxVoltage;
    }

    public double resistance() {
        return resistance;
    }

    public double addEnergy(double joules, boolean simulate) {
        double accepted = Math.min(nonNegative(joules), maxEnergyJoules() - energyJoules);
        if (!simulate) {
            energyJoules += accepted;
        }
        return accepted;
    }

    public double removeEnergy(double joules, boolean simulate) {
        double removed = Math.min(nonNegative(joules), energyJoules);
        if (!simulate) {
            energyJoules -= removed;
        }
        return removed;
    }

    public void setEnergyJoules(double joules) {
        energyJoules = Math.max(0.0, Math.min(maxEnergyJoules(), finite(joules)));
    }

    public void setVoltage(double voltage) {
        double bounded = Math.max(0.0, Math.min(maxVoltage, finite(voltage)));
        setEnergyJoules(capacitance * bounded * bounded);
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finite(value));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
