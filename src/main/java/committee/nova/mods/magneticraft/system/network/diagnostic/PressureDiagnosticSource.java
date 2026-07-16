package committee.nova.mods.magneticraft.system.network.diagnostic;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Read-only, face-aware pressure telemetry.
 */
public interface PressureDiagnosticSource {
    Optional<PressureReading> pressureReading(Direction side);

    record PressureReading(
            Optional<ResourceLocation> gasId,
            double pressureKpa,
            double gasKpaLiters,
            double capacityKpaLiters,
            double fillRatio
    ) {
        public boolean warning() {
            return fillRatio >= 0.9D;
        }
    }
}
