package committee.nova.mods.magneticraft.content.network.electric;

/** Stable identity for the three two-terminal electrical control devices. */
public enum ElectricalControlKind {
    SWITCH("electric_switch"),
    DIODE("diode"),
    RESISTOR("resistor");

    private final String id;

    ElectricalControlKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
