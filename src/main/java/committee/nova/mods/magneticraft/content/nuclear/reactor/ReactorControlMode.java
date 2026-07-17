package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Closed-loop modes unlocked by instrumentation upgrades; none modify physical coefficients. */
public enum ReactorControlMode {
    MANUAL(ReactorAutomationLevel.NONE),
    POWER(ReactorAutomationLevel.REGULATION),
    TEMPERATURE(ReactorAutomationLevel.REGULATION),
    LOAD_FOLLOWING(ReactorAutomationLevel.LOAD_FOLLOWING);

    private final ReactorAutomationLevel requiredLevel;

    ReactorControlMode(ReactorAutomationLevel requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public ReactorAutomationLevel requiredLevel() {
        return requiredLevel;
    }
}
