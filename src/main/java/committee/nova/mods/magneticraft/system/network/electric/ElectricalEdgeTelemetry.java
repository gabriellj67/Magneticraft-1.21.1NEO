package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;

import java.util.Objects;

/** Immutable audit result for one electrical edge execution in the completed tick. */
public record ElectricalEdgeTelemetry(
        EdgeType edgeType,
        PhysicalNodeKey firstTerminal,
        PhysicalNodeKey secondTerminal,
        double chargeCoulombsPerTick,
        double currentAmps,
        double deliveredJoulesPerTick,
        double lostJoulesPerTick,
        boolean firstWasSource
) {
    public ElectricalEdgeTelemetry {
        Objects.requireNonNull(edgeType, "edgeType");
        Objects.requireNonNull(firstTerminal, "firstTerminal");
        Objects.requireNonNull(secondTerminal, "secondTerminal");
        requireNonNegativeFinite("chargeCoulombsPerTick", chargeCoulombsPerTick);
        requireNonNegativeFinite("currentAmps", currentAmps);
        requireNonNegativeFinite("deliveredJoulesPerTick", deliveredJoulesPerTick);
        requireNonNegativeFinite("lostJoulesPerTick", lostJoulesPerTick);
    }

    public enum EdgeType {
        ADJACENT,
        INTERNAL,
        LONG_DISTANCE,
        COUPLER
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(field + " must be finite and non-negative");
        }
    }
}
