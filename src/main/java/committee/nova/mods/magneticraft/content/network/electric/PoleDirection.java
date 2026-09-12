package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.util.StringRepresentable;

/** Eight visible orientations retained from the released pole model. */
public enum PoleDirection implements StringRepresentable {
    NORTH("north"),
    NORTH_EAST("north_east"),
    EAST("east"),
    SOUTH_EAST("south_east"),
    SOUTH("south"),
    SOUTH_WEST("south_west"),
    WEST("west"),
    NORTH_WEST("north_west");

    private static final PoleDirection[] YAW_ORDER = {
            SOUTH,
            SOUTH_WEST,
            WEST,
            NORTH_WEST,
            NORTH,
            NORTH_EAST,
            EAST,
            SOUTH_EAST
    };

    private final String serializedName;

    PoleDirection(String serializedName) {
        this.serializedName = serializedName;
    }

    public static PoleDirection fromYaw(float yawDegrees) {
        int sector = Math.floorMod((int) Math.floor(yawDegrees / 45.0F + 0.5F), YAW_ORDER.length);
        return YAW_ORDER[sector];
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
