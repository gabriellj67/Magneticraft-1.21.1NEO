package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Installed controller instrumentation level. */
public enum ReactorAutomationLevel {
    NONE,
    PROTECTION,
    REGULATION,
    LOAD_FOLLOWING;

    public boolean supports(ReactorAutomationLevel required) {
        return ordinal() >= required.ordinal();
    }
}
