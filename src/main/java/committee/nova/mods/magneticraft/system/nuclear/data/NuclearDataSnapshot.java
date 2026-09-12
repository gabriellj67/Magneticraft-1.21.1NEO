package committee.nova.mods.magneticraft.system.nuclear.data;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Atomically published, server-authoritative nuclear balance view. */
public record NuclearDataSnapshot(
        long generation,
        Map<ResourceLocation, NuclearFuelDefinition> fuelDefinitions
) {
    public NuclearDataSnapshot {
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        fuelDefinitions = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(
                fuelDefinitions,
                "fuelDefinitions"
        )));
    }

    public Optional<NuclearFuelDefinition> fuel(ResourceLocation id) {
        return Optional.ofNullable(fuelDefinitions.get(id));
    }

    NuclearDataSnapshot withGeneration(long nextGeneration) {
        return new NuclearDataSnapshot(nextGeneration, fuelDefinitions);
    }
}
