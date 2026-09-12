package committee.nova.mods.magneticraft.system.network.fluid;

/**
 * Equalizes compatible pipe buffers without mixing fluid identities.
 */
public final class FluidLink {
    private FluidLink() {
    }

    public static Transfer transfer(FluidNode first, FluidNode second, int maxRate) {
        if (maxRate < 0) {
            throw new IllegalArgumentException("Fluid rate must be non-negative");
        }
        if (maxRate == 0 || (first.isEmpty() && second.isEmpty())) {
            return Transfer.ZERO;
        }

        FluidNode source;
        FluidNode target;
        if (fillRatio(first) >= fillRatio(second)) {
            source = first;
            target = second;
        } else {
            source = second;
            target = first;
        }
        if (source.isEmpty() || (!target.isEmpty() && !source.fluidKey().equals(target.fluidKey()))) {
            return Transfer.ZERO;
        }

        int combined = source.amount() + target.amount();
        int equilibriumSource = (int) Math.ceil(combined * (source.capacity() / (double) (source.capacity() + target.capacity())));
        int requested = Math.min(maxRate, Math.max(0, source.amount() - equilibriumSource));
        requested = Math.min(requested, target.capacity() - target.amount());
        if (requested <= 0) {
            return Transfer.ZERO;
        }

        String fluid = source.fluidKey();
        int accepted = target.fill(fluid, requested, true);
        int drained = source.drain(fluid, accepted, false);
        int filled = target.fill(fluid, drained, false);
        if (filled != drained) {
            source.fill(fluid, drained - filled, false);
        }
        return new Transfer(filled, source == first);
    }

    private static double fillRatio(FluidNode node) {
        return node.capacity() == 0 ? 0.0 : node.amount() / (double) node.capacity();
    }

    public record Transfer(int movedMillibuckets, boolean firstWasSource) {
        public static final Transfer ZERO = new Transfer(0, true);

        public boolean moved() {
            return movedMillibuckets > 0;
        }
    }
}
