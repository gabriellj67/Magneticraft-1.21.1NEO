package committee.nova.mods.magneticraft.system.network.pressure;

import net.minecraft.resources.ResourceLocation;

/**
 * Conservative gas equalization between two pressure volumes.
 */
public final class PressureLink {
    private static final double EPSILON = 1.0E-9;

    private PressureLink() {
    }

    public static Transfer transfer(
            PressureNode first,
            PressureNode second,
            double conductance,
            double maxGasPerTick,
            double leakFraction
    ) {
        if (conductance < 0.0 || maxGasPerTick < 0.0 || leakFraction < 0.0 || leakFraction > 1.0) {
            throw new IllegalArgumentException("Invalid pressure edge limits");
        }
        PressureNode source = first.pressureKpa() >= second.pressureKpa() ? first : second;
        PressureNode target = source == first ? second : first;
        ResourceLocation sourceGas = source.gasId().orElse(null);
        ResourceLocation targetGas = target.gasId().orElse(null);
        if (sourceGas == null || targetGas != null && !sourceGas.equals(targetGas)) {
            return Transfer.ZERO;
        }
        double difference = source.pressureKpa() - target.pressureKpa();
        if (difference <= EPSILON) {
            return Transfer.ZERO;
        }

        double totalGas = source.gasKpaLiters() + target.gasKpaLiters();
        double equilibriumPressure = totalGas / (source.volumeLiters() + target.volumeLiters());
        double equilibriumMove = source.gasKpaLiters() - equilibriumPressure * source.volumeLiters();
        double requested = Math.min(Math.min(difference * conductance, maxGasPerTick), equilibriumMove);
        double targetRoomWithLoss = leakFraction >= 1.0 - EPSILON
                ? 0.0
                : (target.capacityKpaLiters() - target.gasKpaLiters()) / (1.0 - leakFraction);
        double withdrawn = Math.min(requested, targetRoomWithLoss);
        if (withdrawn <= EPSILON) {
            return Transfer.ZERO;
        }

        withdrawn = source.extract(sourceGas, withdrawn, false).gasKpaLiters();
        double leaked = withdrawn * leakFraction;
        double delivered = target.insert(new PressureGasStack(sourceGas, withdrawn - leaked), false);
        return new Transfer(withdrawn, delivered, leaked, source == first);
    }

    public record Transfer(double withdrawn, double delivered, double leaked, boolean firstWasSource) {
        public static final Transfer ZERO = new Transfer(0.0, 0.0, 0.0, true);

        public boolean moved() {
            return withdrawn > EPSILON;
        }
    }
}
