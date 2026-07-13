package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;

import java.util.Optional;

/**
 * Read-only electrical telemetry exposed to diagnostic instruments.
 */
public interface ElectricalDiagnosticSource {
    Optional<ElectricalReading> electricalReading(Direction side);

    /**
     * Immutable snapshot of the most recently completed/current network tick.
     */
    record ElectricalReading(double voltageVolts, double currentAmps, double powerWatts) {
    }
}
