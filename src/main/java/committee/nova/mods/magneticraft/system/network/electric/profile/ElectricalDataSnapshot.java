package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Atomically published view of every electrical data-pack registry.
 */
public record ElectricalDataSnapshot(
        long generation,
        Map<ResourceLocation, VoltageTier> voltageTiers,
        Map<ResourceLocation, TransformerProfile> transformerProfiles,
        Map<ResourceLocation, MachineElectricalProfile> machineProfiles
) {
    public ElectricalDataSnapshot {
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must not be negative");
        }
        voltageTiers = Map.copyOf(Objects.requireNonNull(voltageTiers, "voltageTiers"));
        transformerProfiles = Map.copyOf(Objects.requireNonNull(transformerProfiles, "transformerProfiles"));
        machineProfiles = Map.copyOf(Objects.requireNonNull(machineProfiles, "machineProfiles"));
        if (voltageTiers.isEmpty() || transformerProfiles.isEmpty() || machineProfiles.isEmpty()) {
            throw new IllegalArgumentException("electrical snapshots require all three non-empty registries");
        }
    }

    public ElectricalDataSnapshot withGeneration(long nextGeneration) {
        return new ElectricalDataSnapshot(nextGeneration, voltageTiers, transformerProfiles, machineProfiles);
    }

    public Optional<VoltageTier> voltageTier(ResourceLocation id) {
        return Optional.ofNullable(voltageTiers.get(id));
    }

    public Optional<TransformerProfile> transformerProfile(ResourceLocation id) {
        return Optional.ofNullable(transformerProfiles.get(id));
    }

    public Optional<MachineElectricalProfile> machineProfile(ResourceLocation id) {
        return Optional.ofNullable(machineProfiles.get(id));
    }

    public List<VoltageTierDisplay> displayTiers() {
        return voltageTiers.values().stream()
                .sorted(Comparator.comparing(tier -> tier.id().toString()))
                .map(VoltageTierDisplay::from)
                .toList();
    }
}
