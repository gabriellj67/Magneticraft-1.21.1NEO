package committee.nova.mods.magneticraft.system.network.electric;

/**
 * Joule-authoritative capacitive node.
 *
 * <p>The rated maximum voltage is a damage threshold. New input is accepted up
 * to four times that voltage, while profile reconfiguration never rewrites the
 * already stored joules.</p>
 */
public final class ElectricalNode {
    public static final double ABSOLUTE_VOLTAGE_MULTIPLIER = 4.0D;
    public static final double TICKS_PER_SECOND = 20.0D;

    private double capacitance;
    private double maxVoltage;
    private double resistance;
    private double energyJoules;
    private double pendingChargeThroughput;
    private double pendingEnergyThroughput;
    private double activeTickChargeThroughput;
    private double activeTickEnergyThroughput;
    private double lastCompletedTickChargeCoulombs;
    private double lastCompletedTickJoules;
    private boolean networkTickActive;

    public ElectricalNode(double capacitance, double maxVoltage, double resistance) {
        validateConfiguration(capacitance, maxVoltage, resistance);
        this.capacitance = capacitance;
        this.maxVoltage = maxVoltage;
        this.resistance = resistance;
    }

    public double voltage() {
        return Math.sqrt(2.0D * energyJoules / capacitance);
    }

    public double energyJoules() {
        return energyJoules;
    }

    /** Absolute input boundary at four times the rated maximum voltage. */
    public double maxEnergyJoules() {
        return energyAtVoltage(absoluteMaximumVoltage());
    }

    public double ratedMaximumEnergyJoules() {
        return energyAtVoltage(maxVoltage);
    }

    public double absoluteMaximumVoltage() {
        return maxVoltage * ABSOLUTE_VOLTAGE_MULTIPLIER;
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

    /** Rebinds physical parameters without creating, deleting or clamping stored joules. */
    public void reconfigure(double capacitance, double maxVoltage, double resistance) {
        validateConfiguration(capacitance, maxVoltage, resistance);
        this.capacitance = capacitance;
        this.maxVoltage = maxVoltage;
        this.resistance = resistance;
    }

    /**
     * Applies charge in coulombs and returns the signed amount accepted.
     * Simulation is pure and does not alter state or telemetry.
     */
    public double applyCharge(double coulombs, boolean simulate) {
        double requested = finiteOrZero(coulombs);
        double initialVoltage = voltage();
        double requestedVoltage = initialVoltage + requested / capacitance;
        double targetVoltage = Math.max(0.0D, requestedVoltage);
        if (requested > 0.0D) {
            targetVoltage = Math.min(absoluteMaximumVoltage(), targetVoltage);
        }
        double accepted = capacitance * (targetVoltage - initialVoltage);
        if (!simulate && accepted != 0.0D) {
            double initialEnergy = energyJoules;
            energyJoules = energyAtVoltage(targetVoltage);
            recordThroughput(Math.abs(accepted), Math.abs(energyJoules - initialEnergy));
        }
        return accepted;
    }

    public double addEnergy(double joules, boolean simulate) {
        double room = Math.max(0.0D, maxEnergyJoules() - energyJoules);
        double accepted = Math.min(nonNegativeFinite(joules), room);
        if (!simulate && accepted > 0.0D) {
            double initialVoltage = voltage();
            energyJoules += accepted;
            recordThroughput(capacitance * Math.abs(voltage() - initialVoltage), accepted);
        }
        return accepted;
    }

    public double removeEnergy(double joules, boolean simulate) {
        double removed = Math.min(nonNegativeFinite(joules), energyJoules);
        if (!simulate && removed > 0.0D) {
            double initialVoltage = voltage();
            energyJoules -= removed;
            recordThroughput(capacitance * Math.abs(voltage() - initialVoltage), removed);
        }
        return removed;
    }

    /** Restores authoritative persistence without applying a profile-dependent clamp. */
    public void setEnergyJoules(double joules) {
        energyJoules = nonNegativeFinite(joules);
    }

    /** Test/admin helper bounded only by the four-times absolute input limit. */
    public void setVoltage(double voltage) {
        double bounded = Math.max(0.0D, Math.min(absoluteMaximumVoltage(), finiteOrZero(voltage)));
        setEnergyJoules(energyAtVoltage(bounded));
    }

    public void beginNetworkTick() {
        if (networkTickActive) {
            throw new IllegalStateException("Electrical node network tick already active");
        }
        activeTickChargeThroughput = pendingChargeThroughput;
        activeTickEnergyThroughput = pendingEnergyThroughput;
        pendingChargeThroughput = 0.0D;
        pendingEnergyThroughput = 0.0D;
        networkTickActive = true;
    }

    public void completeNetworkTick() {
        if (!networkTickActive) {
            throw new IllegalStateException("Electrical node network tick is not active");
        }
        lastCompletedTickChargeCoulombs = activeTickChargeThroughput;
        lastCompletedTickJoules = activeTickEnergyThroughput;
        activeTickChargeThroughput = 0.0D;
        activeTickEnergyThroughput = 0.0D;
        networkTickActive = false;
    }

    public double lastCompletedTickChargeCoulombs() {
        return lastCompletedTickChargeCoulombs;
    }

    public double lastCompletedTickCurrentAmps() {
        return lastCompletedTickChargeCoulombs * TICKS_PER_SECOND;
    }

    public double lastCompletedTickJoules() {
        return lastCompletedTickJoules;
    }

    public double lastCompletedTickPowerWatts() {
        return lastCompletedTickJoules * TICKS_PER_SECOND;
    }

    private double energyAtVoltage(double voltage) {
        return 0.5D * capacitance * voltage * voltage;
    }

    private void recordThroughput(double charge, double energy) {
        if (networkTickActive) {
            activeTickChargeThroughput += charge;
            activeTickEnergyThroughput += energy;
        } else {
            pendingChargeThroughput += charge;
            pendingEnergyThroughput += energy;
        }
    }

    private static void validateConfiguration(double capacitance, double maxVoltage, double resistance) {
        if (!isPositiveFinite(capacitance)
                || !isPositiveFinite(maxVoltage)
                || !Double.isFinite(resistance)
                || resistance < 0.0D
                || !Double.isFinite(0.5D * capacitance * maxVoltage * maxVoltage
                * ABSOLUTE_VOLTAGE_MULTIPLIER * ABSOLUTE_VOLTAGE_MULTIPLIER)) {
            throw new IllegalArgumentException("Invalid electrical node limits");
        }
    }

    private static double nonNegativeFinite(double value) {
        return Math.max(0.0D, finiteOrZero(value));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }
}
