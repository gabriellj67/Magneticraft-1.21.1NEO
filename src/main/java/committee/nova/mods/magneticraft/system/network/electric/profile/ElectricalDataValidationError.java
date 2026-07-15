package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** A validation failure anchored to the source resource and registry. */
public record ElectricalDataValidationError(
        RegistryKind registry,
        ResourceLocation resourceId,
        String message
) {
    public ElectricalDataValidationError {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(message, "message");
    }

    public enum RegistryKind {
        VOLTAGE_TIER,
        TRANSFORMER_PROFILE,
        MACHINE_PROFILE
    }
}
