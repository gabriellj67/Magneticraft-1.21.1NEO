package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;

import java.util.Optional;

/**
 * Read-only, face-aware diagnostic surface exposed by shared machine hosts.
 */
public interface DiagnosticHost {
    Optional<ElectricalDiagnosticSource.ElectricalReading> electricalReading(Direction side);

    default Optional<ElectricalDiagnosticSource.NetworkSummary> electricalNetworkSummary(
            Direction side,
            int maxVisitedNodes
    ) {
        return Optional.empty();
    }

    default Optional<ElectricalDiagnosticSource.FaultSearchResult> nearestElectricalFault(
            Direction side,
            int maxVisitedNodes
    ) {
        return Optional.empty();
    }

    Optional<ThermalDiagnosticSource.ThermalReading> thermalReading(Direction side);

    default Optional<PressureDiagnosticSource.PressureReading> pressureReading(Direction side) {
        return Optional.empty();
    }
}
