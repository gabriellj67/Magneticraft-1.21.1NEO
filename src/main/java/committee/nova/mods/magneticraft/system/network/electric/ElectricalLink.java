package committee.nova.mods.magneticraft.system.network.electric;

/** Exact one-tick RC charge response across a resistive electrical edge. */
public final class ElectricalLink {
    public static final double SECONDS_PER_TICK = 0.05D;
    private static final double EPSILON = 1.0E-9D;

    private ElectricalLink() {
    }

    public static Transfer transfer(ElectricalNode first, ElectricalNode second, double distance) {
        return transfer(first, second, distance, false);
    }

    /** Computes or commits one transfer. Simulation is byte-for-byte state neutral. */
    public static Transfer transfer(
            ElectricalNode first,
            ElectricalNode second,
            double distance,
            boolean simulate
    ) {
        if (!Double.isFinite(distance) || distance <= 0.0D) {
            throw new IllegalArgumentException("Electrical edge distance must be positive and finite");
        }
        double firstVoltage = first.voltage();
        double secondVoltage = second.voltage();
        double voltageDifference = firstVoltage - secondVoltage;
        if (Math.abs(voltageDifference) <= EPSILON) {
            return Transfer.ZERO;
        }

        double equivalentCapacitance = first.capacitance() * second.capacitance()
                / (first.capacitance() + second.capacitance());
        double equilibriumCharge = equivalentCapacitance * Math.abs(voltageDifference);
        double totalResistance = (first.resistance() + second.resistance()) * distance;
        double response = totalResistance == 0.0D
                ? 1.0D
                : -Math.expm1(-SECONDS_PER_TICK / (totalResistance * equivalentCapacitance));
        double requestedCharge = Math.min(equilibriumCharge, equilibriumCharge * response);

        boolean firstWasSource = voltageDifference > 0.0D;
        ElectricalNode source = firstWasSource ? first : second;
        ElectricalNode target = firstWasSource ? second : first;
        double removable = Math.abs(source.applyCharge(-requestedCharge, true));
        double acceptable = Math.abs(target.applyCharge(requestedCharge, true));
        double charge = Math.min(requestedCharge, Math.min(removable, acceptable));
        if (charge <= EPSILON) {
            return Transfer.ZERO;
        }

        double sourceBefore = source.energyJoules();
        double targetBefore = target.energyJoules();
        double sourceAfter = energyAfterCharge(source, -charge);
        double targetAfter = energyAfterCharge(target, charge);
        double withdrawn = Math.max(0.0D, sourceBefore - sourceAfter);
        double delivered = Math.max(0.0D, targetAfter - targetBefore);
        double loss = Math.max(0.0D, withdrawn - delivered);

        if (!simulate) {
            source.applyCharge(-charge, false);
            target.applyCharge(charge, false);
        }
        return new Transfer(
                withdrawn,
                delivered,
                loss,
                charge,
                charge * ElectricalNode.TICKS_PER_SECOND,
                firstWasSource
        );
    }

    private static double energyAfterCharge(ElectricalNode node, double charge) {
        double voltage = Math.max(0.0D, node.voltage() + charge / node.capacitance());
        return 0.5D * node.capacitance() * voltage * voltage;
    }

    public record Transfer(
            double withdrawnJoules,
            double deliveredJoules,
            double lostJoules,
            double chargeCoulombsPerTick,
            double currentAmps,
            boolean firstWasSource
    ) {
        public static final Transfer ZERO = new Transfer(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, true);

        public boolean moved() {
            return withdrawnJoules > EPSILON;
        }
    }
}
