package committee.nova.mods.magneticraft.system.network.electric;

/**
 * One-tick RC charge balancing across a resistive electrical edge.
 */
public final class ElectricalLink {
    private static final double EPSILON = 1.0E-9;

    private ElectricalLink() {
    }

    public static Transfer transfer(
            ElectricalNode first,
            ElectricalNode second,
            double distance
    ) {
        if (!Double.isFinite(distance) || distance <= 0.0) {
            throw new IllegalArgumentException("Electrical edge distance must be positive and finite");
        }
        double firstVoltage = first.voltage();
        double secondVoltage = second.voltage();
        double voltageDifference = firstVoltage - secondVoltage;
        if (Math.abs(voltageDifference) <= EPSILON) {
            return Transfer.ZERO;
        }

        double equivalentCapacitance = 1.0 / (1.0 / first.capacitance() + 1.0 / second.capacitance());
        double resistance = (first.resistance() + second.resistance()) * distance;
        double response = resistance == 0.0
                ? 1.0
                : -Math.expm1(-1.0 / (resistance * equivalentCapacitance));
        double equilibriumVoltage = (
                firstVoltage * first.capacitance() + secondVoltage * second.capacitance()
        ) / (first.capacitance() + second.capacitance());
        double desiredCurrent = response
                * (equilibriumVoltage - secondVoltage)
                * second.capacitance()
                / first.capacitance()
                * equivalentCapacitance
                * 2.0;

        // The legacy equation can overshoot for unlike capacitances. Stop at the
        // shared-voltage equilibrium so a bounded port never manufactures energy.
        double equilibriumCharge = equivalentCapacitance * voltageDifference;
        double balancedCurrent = Math.copySign(
                Math.min(Math.abs(desiredCurrent), Math.abs(equilibriumCharge)),
                desiredCurrent
        );
        double acceptedByFirst = first.applyCharge(-balancedCurrent, true);
        double acceptedBySecond = second.applyCharge(balancedCurrent, true);
        double currentMagnitude = Math.min(Math.abs(acceptedByFirst), Math.abs(acceptedBySecond));
        if (currentMagnitude <= EPSILON) {
            return Transfer.ZERO;
        }
        double current = Math.copySign(currentMagnitude, balancedCurrent);

        double firstBefore = first.energyJoules();
        double secondBefore = second.energyJoules();
        first.applyCharge(-current, false);
        second.applyCharge(current, false);

        boolean firstWasSource = current > 0.0;
        double withdrawn = firstWasSource
                ? firstBefore - first.energyJoules()
                : secondBefore - second.energyJoules();
        double delivered = firstWasSource
                ? second.energyJoules() - secondBefore
                : first.energyJoules() - firstBefore;
        double loss = Math.max(0.0, withdrawn - delivered);
        return new Transfer(withdrawn, delivered, loss, currentMagnitude, firstWasSource);
    }

    public record Transfer(
            double withdrawnJoules,
            double deliveredJoules,
            double lostJoules,
            double currentAmps,
            boolean firstWasSource
    ) {
        public static final Transfer ZERO = new Transfer(0.0, 0.0, 0.0, 0.0, true);

        public boolean moved() {
            return withdrawnJoules > EPSILON;
        }
    }
}
