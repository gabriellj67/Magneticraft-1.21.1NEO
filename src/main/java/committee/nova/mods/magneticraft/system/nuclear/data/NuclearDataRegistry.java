package committee.nova.mods.magneticraft.system.nuclear.data;

import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Atomic owner of the current nuclear data snapshot with per-ID fallback on malformed overrides. */
public final class NuclearDataRegistry {
    public static final NuclearDataRegistry INSTANCE = new NuclearDataRegistry();

    private static final Set<ResourceLocation> REQUIRED_FUELS = Arrays.stream(NuclearFuelGrade.values())
            .map(NuclearFuelGrade::definitionId)
            .collect(Collectors.toUnmodifiableSet());

    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<NuclearDataSnapshot> current = new AtomicReference<>();

    public Optional<NuclearDataSnapshot> current() {
        return Optional.ofNullable(current.get());
    }

    public NuclearDataSnapshot currentOrThrow() {
        NuclearDataSnapshot snapshot = current.get();
        if (snapshot == null) {
            throw new IllegalStateException("Nuclear data is not available");
        }
        return snapshot;
    }

    public synchronized ApplyResult apply(NuclearDataLoadResult result) {
        Objects.requireNonNull(result, "result");
        NuclearDataSnapshot previous = current.get();
        LinkedHashMap<ResourceLocation, NuclearFuelDefinition> merged =
                new LinkedHashMap<>(result.fuelDefinitions());
        if (previous != null) {
            for (ResourceLocation rejectedId : result.rejectedFuelIds()) {
                NuclearFuelDefinition fallback = previous.fuelDefinitions().get(rejectedId);
                if (fallback != null) {
                    merged.put(rejectedId, fallback);
                }
            }
        }
        LinkedHashSet<ResourceLocation> missing = new LinkedHashSet<>(REQUIRED_FUELS);
        missing.removeAll(merged.keySet());
        if (!missing.isEmpty()) {
            throw new NuclearDataReloadException(missing);
        }
        NuclearDataSnapshot published = new NuclearDataSnapshot(
                generation.incrementAndGet(),
                Map.copyOf(merged)
        );
        current.set(published);
        return new ApplyResult(Optional.ofNullable(previous), published, result.rejectedFuelIds().size());
    }

    public void reset() {
        current.set(null);
    }

    public record ApplyResult(
            Optional<NuclearDataSnapshot> previous,
            NuclearDataSnapshot current,
            int rejectedEntries
    ) {
        public ApplyResult {
            previous = Objects.requireNonNull(previous, "previous");
            Objects.requireNonNull(current, "current");
            if (rejectedEntries < 0) {
                throw new IllegalArgumentException("rejectedEntries must be non-negative");
            }
        }
    }
}
