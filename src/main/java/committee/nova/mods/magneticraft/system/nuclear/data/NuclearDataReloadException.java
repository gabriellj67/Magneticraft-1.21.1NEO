package committee.nova.mods.magneticraft.system.nuclear.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.stream.Collectors;

/** Fails a reload only when no safe built-in definition can be published. */
public final class NuclearDataReloadException extends IllegalStateException {
    public NuclearDataReloadException(Set<ResourceLocation> missingBuiltIns) {
        super("Nuclear data is missing required built-in fuel definitions: "
                + missingBuiltIns.stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", ")));
    }
}
