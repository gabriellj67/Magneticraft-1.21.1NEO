package committee.nova.mods.magneticraft.content.nuclear.reactor;

/** Server-authoritative operating phase before the detailed accident stages are added. */
public enum ReactorOperatingState {
    SHUTDOWN,
    STARTUP,
    RUNNING,
    DECAY_HEAT,
    SCRAMMED;

    public boolean producesFissionHeat() {
        return this == STARTUP || this == RUNNING;
    }
}
