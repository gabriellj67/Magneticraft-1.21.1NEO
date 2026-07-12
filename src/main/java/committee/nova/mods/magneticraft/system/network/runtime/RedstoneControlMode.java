package committee.nova.mods.magneticraft.system.network.runtime;

/**
 * Persisted automation gate shared by active network components.
 */
public enum RedstoneControlMode {
    IGNORED,
    REQUIRES_SIGNAL,
    REQUIRES_NO_SIGNAL;

    public boolean allows(boolean hasSignal) {
        return switch (this) {
            case IGNORED -> true;
            case REQUIRES_SIGNAL -> hasSignal;
            case REQUIRES_NO_SIGNAL -> !hasSignal;
        };
    }

    public RedstoneControlMode next() {
        RedstoneControlMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
