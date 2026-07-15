package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Locale;
import java.util.Optional;

/**
 * Read-only electrical telemetry exposed to diagnostic instruments.
 */
public interface ElectricalDiagnosticSource {
    Optional<ElectricalReading> electricalReading(Direction side);

    default Optional<NetworkSummary> electricalNetworkSummary(Direction side, int maxVisitedNodes) {
        return Optional.empty();
    }

    default Optional<FaultSearchResult> nearestElectricalFault(Direction side, int maxVisitedNodes) {
        return Optional.empty();
    }

    /**
     * Immutable snapshot of the most recently completed/current network tick.
     */
    record ElectricalReading(
            ResourceLocation tierId,
            ResourceLocation terminalId,
            double voltageVolts,
            double chargeCoulombsPerTick,
            double currentAmps,
            double joulesPerTick,
            double powerWatts,
            double storedJoules,
            double capacityJoules,
            double loadRatio,
            double thermalStress,
            FlowDirection flowDirection,
            FaultKind faultKind
    ) {
        public ElectricalReading {
            Objects.requireNonNull(tierId, "tierId");
            Objects.requireNonNull(terminalId, "terminalId");
            Objects.requireNonNull(flowDirection, "flowDirection");
            Objects.requireNonNull(faultKind, "faultKind");
            requireNonNegativeFinite(voltageVolts, "voltageVolts");
            requireNonNegativeFinite(chargeCoulombsPerTick, "chargeCoulombsPerTick");
            requireNonNegativeFinite(currentAmps, "currentAmps");
            requireNonNegativeFinite(joulesPerTick, "joulesPerTick");
            requireNonNegativeFinite(powerWatts, "powerWatts");
            requireNonNegativeFinite(storedJoules, "storedJoules");
            requireNonNegativeFinite(capacityJoules, "capacityJoules");
            requireNonNegativeFinite(loadRatio, "loadRatio");
            requireRange(thermalStress, 0.0D, 1.0D, "thermalStress");
        }
    }

    record NetworkSummary(
            int nodeCount,
            int edgeCount,
            double storedJoules,
            double generatedJoulesPerTick,
            double consumedJoulesPerTick,
            double lostJoulesPerTick,
            double maximumLoadRatio,
            int faultCount,
            int visitedNodes,
            boolean truncated
    ) {
        public NetworkSummary {
            if (nodeCount < 0 || edgeCount < 0 || faultCount < 0 || visitedNodes < 0) {
                throw new IllegalArgumentException("Electrical diagnostic counts must be non-negative");
            }
            requireNonNegativeFinite(storedJoules, "storedJoules");
            requireNonNegativeFinite(generatedJoulesPerTick, "generatedJoulesPerTick");
            requireNonNegativeFinite(consumedJoulesPerTick, "consumedJoulesPerTick");
            requireNonNegativeFinite(lostJoulesPerTick, "lostJoulesPerTick");
            requireNonNegativeFinite(maximumLoadRatio, "maximumLoadRatio");
        }
    }

    record FaultLocation(
            FaultKind faultKind,
            ResourceLocation terminalId,
            BlockPos position,
            Optional<Direction> direction,
            double distanceBlocks
    ) {
        public FaultLocation {
            Objects.requireNonNull(faultKind, "faultKind");
            Objects.requireNonNull(terminalId, "terminalId");
            Objects.requireNonNull(position, "position");
            direction = Objects.requireNonNull(direction, "direction");
            requireNonNegativeFinite(distanceBlocks, "distanceBlocks");
            if (faultKind == FaultKind.NONE) {
                throw new IllegalArgumentException("Fault locations require a concrete fault");
            }
        }
    }

    record FaultSearchResult(Optional<FaultLocation> location, int visitedNodes, boolean truncated) {
        public FaultSearchResult {
            location = Objects.requireNonNull(location, "location");
            if (visitedNodes < 1) {
                throw new IllegalArgumentException("Fault search must visit its starting terminal");
            }
        }
    }

    enum FlowDirection {
        INPUT,
        OUTPUT,
        BIDIRECTIONAL,
        IDLE;

        public String translationKey() {
            return "flow.magneticraft." + name().toLowerCase(Locale.ROOT);
        }
    }

    enum FaultKind {
        NONE,
        MISSING_PROFILE,
        MACHINE_FAULT,
        FUSE_BLOWN,
        BREAKER_TRIPPED,
        REDSTONE_OPEN;

        public String translationKey() {
            return "fault.magneticraft." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireRange(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " is outside the supported range");
        }
    }
}
