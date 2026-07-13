package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;

import java.util.Optional;

/**
 * Read-only, face-aware diagnostic surface exposed by shared machine hosts.
 */
public interface DiagnosticHost {
    Optional<ElectricalDiagnosticSource.ElectricalReading> electricalReading(Direction side);

    Optional<ThermalDiagnosticSource.ThermalReading> thermalReading(Direction side);
}
