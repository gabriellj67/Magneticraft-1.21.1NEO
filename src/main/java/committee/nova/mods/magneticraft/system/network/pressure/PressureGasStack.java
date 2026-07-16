package committee.nova.mods.magneticraft.system.network.pressure;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Immutable typed gas quantity expressed in kPa·L.
 */
public record PressureGasStack(ResourceLocation gasId, double gasKpaLiters) {
    public PressureGasStack {
        Objects.requireNonNull(gasId, "gasId");
        if (!Double.isFinite(gasKpaLiters) || gasKpaLiters < 0.0D) {
            throw new IllegalArgumentException("Gas quantity must be finite and non-negative");
        }
    }

    public boolean isEmpty() {
        return gasKpaLiters <= PressureNode.EPSILON;
    }
}
