package committee.nova.mods.magneticraft.content.machine.framework;

/**
 * Loader-independent bounded integer energy storage.
 */
public final class EnergyBuffer {
    private int capacity;
    private int maxReceive;
    private int maxExtract;
    private int energy;

    public EnergyBuffer(int capacity, int maxReceive, int maxExtract) {
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("Energy limits must be non-negative");
        }
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public int receive(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(amount, 0), Math.min(maxReceive, Math.max(0, capacity - energy)));
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    public int extract(int amount, boolean simulate) {
        int extracted = Math.min(Math.max(amount, 0), Math.min(maxExtract, energy));
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    /** Rebinds limits without deleting already-stored joules when capacity shrinks. */
    public void reconfigure(int capacity, int maxReceive, int maxExtract) {
        validate(capacity, maxReceive, maxExtract);
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public int energy() {
        return energy;
    }

    public int capacity() {
        return capacity;
    }

    public int maxReceive() {
        return maxReceive;
    }

    public int maxExtract() {
        return maxExtract;
    }

    private static void validate(int capacity, int maxReceive, int maxExtract) {
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("Energy limits must be non-negative");
        }
    }
}
