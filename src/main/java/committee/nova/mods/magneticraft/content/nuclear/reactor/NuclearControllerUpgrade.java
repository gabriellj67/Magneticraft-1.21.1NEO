package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Progressive instrumentation upgrades; they enable controls but never alter reactor physics. */
public enum NuclearControllerUpgrade {
    PROTECTION("reactor_protection_upgrade", ReactorAutomationLevel.PROTECTION),
    REGULATION("reactor_regulation_upgrade", ReactorAutomationLevel.REGULATION),
    LOAD_FOLLOWING("reactor_load_following_upgrade", ReactorAutomationLevel.LOAD_FOLLOWING);

    private final String id;
    private final ReactorAutomationLevel level;

    NuclearControllerUpgrade(String id, ReactorAutomationLevel level) {
        this.id = id;
        this.level = level;
    }

    public String id() {
        return id;
    }

    public ReactorAutomationLevel level() {
        return level;
    }
}
