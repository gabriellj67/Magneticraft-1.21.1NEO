package committee.nova.mods.magneticraft.client.electrical;

import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-authoritative client display cache. Simulation code must never depend on this type.
 */
public final class ClientVoltageTierRegistry {
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(Snapshot.empty());

    private ClientVoltageTierRegistry() {
    }

    public static Snapshot current() {
        return CURRENT.get();
    }

    public static boolean apply(long generation, List<VoltageTierDisplay> tiers) {
        Snapshot candidate = Snapshot.of(generation, tiers);
        while (true) {
            Snapshot previous = CURRENT.get();
            if (candidate.generation() < previous.generation()) {
                return false;
            }
            if (CURRENT.compareAndSet(previous, candidate)) {
                return true;
            }
        }
    }

    public static void reset() {
        CURRENT.set(Snapshot.empty());
    }

    public record Snapshot(long generation, Map<ResourceLocation, VoltageTierDisplay> tiers) {
        public Snapshot {
            if (generation < 0L) {
                throw new IllegalArgumentException("generation must not be negative");
            }
            tiers = Map.copyOf(tiers);
        }

        public Optional<VoltageTierDisplay> tier(ResourceLocation id) {
            return Optional.ofNullable(tiers.get(id));
        }

        private static Snapshot empty() {
            return new Snapshot(0L, Map.of());
        }

        private static Snapshot of(long generation, List<VoltageTierDisplay> tiers) {
            if (generation <= 0L) {
                throw new IllegalArgumentException("synced generations must be positive");
            }
            if (tiers.isEmpty() || tiers.size() > VoltageTier.MAX_SYNCED_TIERS) {
                throw new IllegalArgumentException("invalid synced tier count");
            }
            LinkedHashMap<ResourceLocation, VoltageTierDisplay> indexed = new LinkedHashMap<>();
            for (VoltageTierDisplay tier : tiers) {
                if (indexed.put(tier.id(), tier) != null) {
                    throw new IllegalArgumentException("duplicate synced tier " + tier.id());
                }
            }
            return new Snapshot(generation, indexed);
        }
    }

}
