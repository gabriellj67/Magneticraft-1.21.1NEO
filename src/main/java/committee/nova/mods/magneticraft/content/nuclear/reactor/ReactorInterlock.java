package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Explicit interlocks, including the two non-fatal conditions eligible for engineering override. */
public enum ReactorInterlock {
    STRUCTURE(false),
    FUEL(false),
    STATION_POWER(false),
    COOLANT_FLOW(true),
    INSTRUMENTATION(true),
    CLADDING(false),
    OVER_TEMPERATURE(false),
    RUNTIME_DATA(false);

    private final boolean overrideAllowed;

    ReactorInterlock(boolean overrideAllowed) {
        this.overrideAllowed = overrideAllowed;
    }

    public boolean overrideAllowed() {
        return overrideAllowed;
    }
}
