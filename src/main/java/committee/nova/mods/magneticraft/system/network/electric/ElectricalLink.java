package committee.nova.mods.magneticraft.system.network.electric;

/**
 * Conservative one-tick transfer across a resistive electrical edge.
 */
public final class ElectricalLink {
    private static final double EPSILON = 1.0E-9;

    private ElectricalLink() {
    }

    public static Transfer transfer(
            ElectricalNode first,
            ElectricalNode second,
            double edgeResistance,
            double maxCurrent
    ) {
        if (edgeResistance < 0.0 || maxCurrent < 0.0) {
            throw new IllegalArgumentException("Electrical edge limits must be non-negative");
        }
        ElectricalNode source = first.voltage() >= second.voltage() ? first : second;
        ElectricalNode target = source == first ? second : first;
        double difference = source.voltage() - target.voltage();
        if (difference <= EPSILON || maxCurrent <= EPSILON) {
            return Transfer.ZERO;
        }

        double resistance = Math.max(EPSILON, source.resistance() + target.resistance() + edgeResistance);
        double current = Math.min(maxCurrent, difference / resistance);
        double gross = Math.min(source.energyJoules(), current * source.voltage());
        if (gross <= EPSILON) {
            return Transfer.ZERO;
        }

        double theoreticalLoss = Math.min(gross, current * current * resistance);
        double lossFraction = theoreticalLoss / gross;
        double targetRoom = target.maxEnergyJoules() - target.energyJoules();
        double grossForRoom = lossFraction >= 1.0 - EPSILON
                ? 0.0
                : targetRoom / (1.0 - lossFraction);
        double usedGross = Math.min(gross, grossForRoom);
        double loss = usedGross * lossFraction;
        double delivered = usedGross - loss;
        if (delivered <= EPSILON) {
            return Transfer.ZERO;
        }

        source.removeEnergy(usedGross, false);
        target.addEnergy(delivered, false);
        return new Transfer(usedGross, delivered, loss, current, source == first);
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
