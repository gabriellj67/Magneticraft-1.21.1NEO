package committee.nova.mods.magneticraft.content.nuclear.facility;

public enum NuclearFacilityPartRole {
    AIR,
    CONTROLLER,
    CASING,
    ITEM_INPUT,
    ITEM_OUTPUT,
    ELECTRICAL,
    PROCESS_CORE,
    CENTRIFUGE_STAGE;

    public boolean isPort() {
        return this == ITEM_INPUT || this == ITEM_OUTPUT || this == ELECTRICAL;
    }
}
