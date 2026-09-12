package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;

import java.util.Optional;

/**
 * Read-only thermal telemetry exposed to diagnostic instruments.
 */
public interface ThermalDiagnosticSource {
    Optional<ThermalReading> thermalReading(Direction side);

    /**
     * Immutable thermal snapshot in absolute temperature.
     */
    record ThermalReading(double temperatureKelvin) {
    }
}
