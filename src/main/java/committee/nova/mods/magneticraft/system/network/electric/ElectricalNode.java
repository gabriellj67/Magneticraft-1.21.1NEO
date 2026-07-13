package committee.nova.mods.magneticraft.system.network.electric;

/**
 * Electrical storage expressed as joules in a capacitive node.
 */
public final class ElectricalNode {
    private final double capacitance;
    private final double maxVoltage;
    private final double resistance;
    private double energyJoules;
    private double pendingChargeThroughput;
    private double activeTickChargeThroughput;
    private double lastCompletedTickCurrentAmps;
    private boolean networkTickActive;

    public ElectricalNode(double capacitance, double maxVoltage, double resistance) {
        if (!isPositiveFinite(capacitance)
                || !isPositiveFinite(maxVoltage)
                || !Double.isFinite(resistance)
                || resistance < 0.0
                || !Double.isFinite(capacitance * maxVoltage * maxVoltage)) {
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

    /**
     * Applies charge in coulombs and returns the signed amount accepted by this bounded node.
     */
    public double applyCharge(double coulombs, boolean simulate) {
        double requested = finite(coulombs);
        double voltage = voltage();
        double targetVoltage = Math.max(0.0, Math.min(maxVoltage, voltage + requested / capacitance));
        double accepted = capacitance * (targetVoltage - voltage);
        if (!simulate && accepted != 0.0) {
            energyJoules = capacitance * targetVoltage * targetVoltage;
            recordChargeThroughput(Math.abs(accepted));
        }
        return accepted;
    }

    public double addEnergy(double joules, boolean simulate) {
        double room = Math.max(0.0, maxEnergyJoules() - energyJoules);
        double accepted = Math.min(nonNegative(joules), room);
        if (!simulate && accepted > 0.0) {
            double initialVoltage = voltage();
            energyJoules += accepted;
            recordVoltageChange(initialVoltage);
        }
        return accepted;
    }

    public double removeEnergy(double joules, boolean simulate) {
        double removed = Math.min(nonNegative(joules), energyJoules);
        if (!simulate && removed > 0.0) {
            double initialVoltage = voltage();
            energyJoules -= removed;
            recordVoltageChange(initialVoltage);
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

    public void beginNetworkTick() {
        if (networkTickActive) {
            throw new IllegalStateException("Electrical node network tick already active");
        }
        activeTickChargeThroughput = pendingChargeThroughput;
        pendingChargeThroughput = 0.0;
        networkTickActive = true;
    }

    public void completeNetworkTick() {
        if (!networkTickActive) {
            throw new IllegalStateException("Electrical node network tick is not active");
        }
        lastCompletedTickCurrentAmps = activeTickChargeThroughput;
        activeTickChargeThroughput = 0.0;
        networkTickActive = false;
    }

    public double lastCompletedTickCurrentAmps() {
        return lastCompletedTickCurrentAmps;
    }

    private void recordVoltageChange(double initialVoltage) {
        recordChargeThroughput(capacitance * Math.abs(voltage() - initialVoltage));
    }

    private void recordChargeThroughput(double charge) {
        if (networkTickActive) {
            activeTickChargeThroughput += charge;
        } else {
            pendingChargeThroughput += charge;
        }
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finite(value));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }
}
