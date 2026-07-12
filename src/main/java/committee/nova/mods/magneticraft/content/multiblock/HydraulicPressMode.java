package committee.nova.mods.magneticraft.content.multiblock;

import java.util.Locale;

/** Legacy hydraulic-press force selector in stable serialized order. */
public enum HydraulicPressMode {
    LIGHT,
    MEDIUM,
    HEAVY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public HydraulicPressMode next() {
        HydraulicPressMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static HydraulicPressMode parse(String value) {
        for (HydraulicPressMode mode : values()) {
            if (mode.serializedName().equals(value.toLowerCase(Locale.ROOT))) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown hydraulic press mode: " + value);
    }
}
