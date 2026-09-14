package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Ordered, deterministic PWR accident progression. Stages at or above cladding damage are irreversible. */
public enum ReactorAccidentStage {
    NORMAL,
    COOLING_SHORTAGE,
    LOCAL_BOILING,
    CLADDING_DAMAGE,
    FUEL_MELT,
    PRESSURE_RISE,
    VESSEL_BREACH,
    CONTAINMENT_BREACH;

    public boolean severe() {
        return ordinal() >= FUEL_MELT.ordinal();
    }

    public boolean releasesContamination() {
        return ordinal() >= CLADDING_DAMAGE.ordinal();
    }
}
