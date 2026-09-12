package committee.nova.mods.magneticraft.system.nuclear.data;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Parsed valid entries plus the IDs rejected from the same candidate. */
public record NuclearDataLoadResult(
        Map<ResourceLocation, NuclearFuelDefinition> fuelDefinitions,
        List<NuclearDataValidationError> errors,
        Set<ResourceLocation> rejectedFuelIds
) {
    public NuclearDataLoadResult {
        fuelDefinitions = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(fuelDefinitions, "fuelDefinitions")));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        rejectedFuelIds = Set.copyOf(Objects.requireNonNull(rejectedFuelIds, "rejectedFuelIds"));
    }
}
