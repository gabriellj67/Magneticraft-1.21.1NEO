package committee.nova.mods.magneticraft.system.network.fluid;

import java.util.Objects;

/**
 * Loader-independent single-fluid buffer measured in millibuckets.
 */
public final class FluidNode {
    private final int capacity;
    private String fluidKey = "";
    private int amount;

    public FluidNode(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Fluid capacity must be non-negative");
        }
        this.capacity = capacity;
    }

    public int fill(String key, int offered, boolean simulate) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank() || offered <= 0 || (!isEmpty() && !fluidKey.equals(key))) {
            return 0;
        }
        int accepted = Math.min(offered, capacity - amount);
        if (!simulate && accepted > 0) {
            fluidKey = key;
            amount += accepted;
        }
        return accepted;
    }

    public int drain(String key, int requested, boolean simulate) {
        Objects.requireNonNull(key, "key");
        if (requested <= 0 || !fluidKey.equals(key)) {
            return 0;
        }
        int drained = Math.min(requested, amount);
        if (!simulate && drained > 0) {
            amount -= drained;
            if (amount == 0) {
                fluidKey = "";
            }
        }
        return drained;
    }

    public void set(String key, int amount) {
        Objects.requireNonNull(key, "key");
        int bounded = Math.max(0, Math.min(capacity, amount));
        if (key.isBlank() || bounded == 0) {
            this.fluidKey = "";
            this.amount = 0;
            return;
        }
        this.fluidKey = key;
        this.amount = bounded;
    }

    public String fluidKey() {
        return fluidKey;
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return amount == 0;
    }
}
