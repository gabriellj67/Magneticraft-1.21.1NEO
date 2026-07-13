package committee.nova.mods.magneticraft.system.energy;

/**
 * Loader-independent state and transfer rules for portable energy items.
 */
public final class PortableEnergyState {
    private final int capacity;
    private int energy;

    public PortableEnergyState(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Energy capacity must be positive");
        }
        this.capacity = capacity;
    }

    public int receive(int requested, boolean simulate) {
        int accepted = Math.min(Math.max(requested, 0), capacity - energy);
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    public int extract(int requested, boolean simulate) {
        int extracted = Math.min(Math.max(requested, 0), energy);
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    public boolean consume(int amount) {
        if (amount < 0 || energy < amount) {
            return false;
        }
        energy -= amount;
        return true;
    }

    public void load(int storedEnergy) {
        energy = Math.max(0, Math.min(capacity, storedEnergy));
    }

    public int energy() {
        return energy;
    }
}
