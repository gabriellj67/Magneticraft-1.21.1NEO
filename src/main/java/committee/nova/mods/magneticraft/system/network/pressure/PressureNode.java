package committee.nova.mods.magneticraft.system.network.pressure;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Single-gas pressure volume. Gas amount is expressed in kPa·L and pressure
 * is derived from node volume.
 */
public final class PressureNode {
    static final double EPSILON = 1.0E-9D;

    private final double volumeLiters;
    private final double maxPressureKpa;
    private ResourceLocation gasId;
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

    public Optional<ResourceLocation> gasId() {
        return Optional.ofNullable(gasId);
    }

    public Optional<PressureGasStack> contents() {
        return gasId == null || gasKpaLiters <= EPSILON
                ? Optional.empty()
                : Optional.of(new PressureGasStack(gasId, gasKpaLiters));
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

    public double fillRatio() {
        return gasKpaLiters / capacityKpaLiters();
    }

    /**
     * Inserts matching gas and returns the accepted kPa·L. An empty node adopts
     * the offered gas type only when a non-zero amount is actually inserted.
     */
    public double insert(PressureGasStack stack, boolean simulate) {
        if (stack.isEmpty() || gasId != null && !gasId.equals(stack.gasId())) {
            return 0.0D;
        }
        double accepted = Math.min(stack.gasKpaLiters(), capacityKpaLiters() - gasKpaLiters);
        if (!simulate && accepted > EPSILON) {
            gasId = stack.gasId();
            gasKpaLiters += accepted;
        }
        return accepted;
    }

    /**
     * Extracts only the requested gas type. A depleted node becomes untyped.
     */
    public PressureGasStack extract(ResourceLocation requestedGas, double amount, boolean simulate) {
        double requested = Math.max(0.0D, finite(amount));
        if (gasId == null || !gasId.equals(requestedGas) || requested <= EPSILON) {
            return new PressureGasStack(requestedGas, 0.0D);
        }
        double removed = Math.min(requested, gasKpaLiters);
        if (!simulate) {
            gasKpaLiters -= removed;
            clearTypeIfEmpty();
        }
        return new PressureGasStack(requestedGas, removed);
    }

    public void setContents(PressureGasStack stack) {
        gasId = null;
        gasKpaLiters = 0.0D;
        insert(new PressureGasStack(stack.gasId(), Math.min(stack.gasKpaLiters(), capacityKpaLiters())), false);
    }

    public void clear() {
        gasId = null;
        gasKpaLiters = 0.0D;
    }

    public void setPressureKpa(ResourceLocation gas, double pressure) {
        setContents(new PressureGasStack(
                gas,
                Math.max(0.0D, finite(pressure)) * volumeLiters
        ));
    }

    private void clearTypeIfEmpty() {
        if (gasKpaLiters <= EPSILON) {
            gasKpaLiters = 0.0D;
            gasId = null;
        }
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
