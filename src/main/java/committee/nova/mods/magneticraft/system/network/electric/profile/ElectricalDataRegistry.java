package committee.nova.mods.magneticraft.system.network.electric.profile;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic owner of the current server-authoritative electrical data snapshot.
 */
public final class ElectricalDataRegistry {
    public static final ElectricalDataRegistry INSTANCE = new ElectricalDataRegistry();

    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<ElectricalDataSnapshot> current = new AtomicReference<>();

    public Optional<ElectricalDataSnapshot> current() {
        return Optional.ofNullable(current.get());
    }

    public ElectricalDataSnapshot currentOrThrow() {
        ElectricalDataSnapshot snapshot = current.get();
        if (snapshot == null) {
            throw new IllegalStateException("Electrical data is not available");
        }
        return snapshot;
    }

    public synchronized ApplyResult apply(ElectricalDataLoadResult result) {
        Objects.requireNonNull(result, "result");
        if (!result.valid()) {
            throw new ElectricalDataReloadException(result.errors());
        }
        ElectricalDataSnapshot candidate = result.snapshot().orElseThrow();
        ElectricalDataSnapshot published = candidate.withGeneration(generation.incrementAndGet());
        ElectricalDataSnapshot previous = current.getAndSet(published);
        return new ApplyResult(Optional.ofNullable(previous), published);
    }

    /** Clears server-owned data without rewinding generation, preventing stale client packets winning later. */
    public void reset() {
        current.set(null);
    }

    public record ApplyResult(Optional<ElectricalDataSnapshot> previous, ElectricalDataSnapshot current) {
        public ApplyResult {
            previous = Objects.requireNonNull(previous, "previous");
            Objects.requireNonNull(current, "current");
        }
    }
}
