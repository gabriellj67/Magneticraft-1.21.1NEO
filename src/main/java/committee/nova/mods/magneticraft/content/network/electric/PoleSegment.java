package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.util.StringRepresentable;

/** The public pole occupies five vertical blocks; only BASE owns a block entity. */
public enum PoleSegment implements StringRepresentable {
    BASE("base", 0),
    DOWN_1("down_1", 1),
    DOWN_2("down_2", 2),
    DOWN_3("down_3", 3),
    DOWN_4("down_4", 4);

    private final String serializedName;
    private final int blocksBelowBase;

    PoleSegment(String serializedName, int blocksBelowBase) {
        this.serializedName = serializedName;
        this.blocksBelowBase = blocksBelowBase;
    }

    public int blocksBelowBase() {
        return blocksBelowBase;
    }

    public static PoleSegment atHeightFromBottom(int height) {
        return switch (height) {
            case 0 -> DOWN_4;
            case 1 -> DOWN_3;
            case 2 -> DOWN_2;
            case 3 -> DOWN_1;
            case 4 -> BASE;
            default -> throw new IllegalArgumentException("Pole height must be between 0 and 4");
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
