package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Four player-facing safety states derived from the detailed accident state machine. */
public enum ReactorOperatorStatus {
    NORMAL,
    COOLING_WARNING,
    CORE_DAMAGE,
    MELTDOWN_OR_RELEASE;

    public static ReactorOperatorStatus from(ReactorAccidentStage stage) {
        if (stage == null) {
            return NORMAL;
        }
        return switch (stage) {
            case NORMAL -> NORMAL;
            case COOLING_SHORTAGE, LOCAL_BOILING -> COOLING_WARNING;
            case CLADDING_DAMAGE -> CORE_DAMAGE;
            case FUEL_MELT, PRESSURE_RISE, VESSEL_BREACH, CONTAINMENT_BREACH -> MELTDOWN_OR_RELEASE;
        };
    }

    public String translationKey() {
        return "gui.magneticraft.reactor.operator_status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
