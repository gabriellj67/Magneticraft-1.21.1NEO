package committee.nova.mods.magneticraft.system.nuclear.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** One resource-anchored nuclear data rejection. */
public record NuclearDataValidationError(ResourceLocation resourceId, String message) {
    public NuclearDataValidationError {
        Objects.requireNonNull(resourceId, "resourceId");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
