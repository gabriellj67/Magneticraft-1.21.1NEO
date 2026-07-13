package committee.nova.mods.magneticraft.system.network.fluid;

import java.util.List;
import java.util.Objects;

/**
 * Transactional concatenated view over an ordered set of pipe-local buffers.
 * The caller owns membership ordering and loaded-chunk lifecycle.
 */
public final class FluidComponentStorage {
    private final List<FluidNode> nodes;

    public FluidComponentStorage(List<FluidNode> nodes) {
        this.nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        if (this.nodes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Fluid component cannot contain null nodes");
        }
    }

    public int tankCount() {
        return nodes.size();
    }

    public String fluidKey(int tank) {
        return node(tank).fluidKey();
    }

    public int amount(int tank) {
        return node(tank).amount();
    }

    public int capacity(int tank) {
        return node(tank).capacity();
    }

    public boolean isFluidValid(int tank, String fluidKey) {
        FluidNode target = node(tank);
        Objects.requireNonNull(fluidKey, "fluidKey");
        return !fluidKey.isBlank() && (target.isEmpty() || target.fluidKey().equals(fluidKey));
    }

    public int fill(String fluidKey, int offered, boolean simulate) {
        Objects.requireNonNull(fluidKey, "fluidKey");
        if (offered <= 0 || fluidKey.isBlank()) {
            return 0;
        }
        int remaining = offered;
        for (FluidNode node : nodes) {
            remaining -= node.fill(fluidKey, remaining, simulate);
            if (remaining == 0) {
                break;
            }
        }
        return offered - remaining;
    }

    public int drain(String fluidKey, int requested, boolean simulate) {
        Objects.requireNonNull(fluidKey, "fluidKey");
        if (fluidKey.isBlank() || requested <= 0) {
            return 0;
        }
        int remaining = requested;
        for (FluidNode node : nodes) {
            remaining -= node.drain(fluidKey, remaining, simulate);
            if (remaining == 0) {
                break;
            }
        }
        return requested - remaining;
    }

    public Drain drainAny(int requested, boolean simulate) {
        if (requested <= 0) {
            return Drain.EMPTY;
        }
        for (FluidNode node : nodes) {
            if (!node.isEmpty()) {
                String fluidKey = node.fluidKey();
                return new Drain(fluidKey, drain(fluidKey, requested, simulate));
            }
        }
        return Drain.EMPTY;
    }

    private FluidNode node(int tank) {
        if (tank < 0 || tank >= nodes.size()) {
            throw new IndexOutOfBoundsException("Fluid tank index: " + tank);
        }
        return nodes.get(tank);
    }

    public record Drain(String fluidKey, int amount) {
        public static final Drain EMPTY = new Drain("", 0);
    }
}
