package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.util.StringRepresentable;

public enum TeslaTowerPart implements StringRepresentable {
    BOTTOM("bottom", 0),
    MIDDLE("middle", 1),
    TOP("top", 2);

    private final String serializedName;
    private final int height;

    TeslaTowerPart(String serializedName, int height) {
        this.serializedName = serializedName;
        this.height = height;
    }

    public int height() {
        return height;
    }

    public static TeslaTowerPart atHeight(int height) {
        return switch (height) {
            case 0 -> BOTTOM;
            case 1 -> MIDDLE;
            case 2 -> TOP;
            default -> throw new IllegalArgumentException("Tesla tower height must be between 0 and 2");
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
