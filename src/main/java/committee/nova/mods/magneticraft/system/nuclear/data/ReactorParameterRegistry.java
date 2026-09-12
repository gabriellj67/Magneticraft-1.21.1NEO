package committee.nova.mods.magneticraft.system.nuclear.data;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Atomic current PWR balance; malformed reloads retain the last valid definition. */
public final class ReactorParameterRegistry {
    public static final ReactorParameterRegistry INSTANCE = new ReactorParameterRegistry();

    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(0L, ReactorParameters.DEFAULT));

    private ReactorParameterRegistry() {
    }

    public Snapshot current() {
        return current.get();
    }

    public synchronized Snapshot apply(ReactorParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        if (!parameters.id().equals(ReactorParameters.PWR_ID)) {
            throw new IllegalArgumentException("Unsupported reactor parameter id " + parameters.id());
        }
        Snapshot next = new Snapshot(generation.incrementAndGet(), parameters);
        current.set(next);
        return next;
    }

    public void reset() {
        generation.set(0L);
        current.set(new Snapshot(0L, ReactorParameters.DEFAULT));
    }

    public record Snapshot(long generation, ReactorParameters parameters) {
    }
}
