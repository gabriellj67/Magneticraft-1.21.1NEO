package committee.nova.mods.magneticraft.content.nuclear.facility;

import java.util.Locale;

/** Supported front-end nuclear processing structures. */
public enum NuclearFacilityType {
    URANIUM_PROCESSOR("uranium_processor", 3, 3, 3, 3),
    CENTRIFUGE_CASCADE("centrifuge_cascade", 3, 4, 4, 16),
    FUEL_FABRICATOR("fuel_fabricator", 3, 3, 3, 3);

    private final String id;
    private final int width;
    private final int height;
    private final int minimumDepth;
    private final int maximumDepth;

    NuclearFacilityType(String id, int width, int height, int minimumDepth, int maximumDepth) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.minimumDepth = minimumDepth;
        this.maximumDepth = maximumDepth;
    }

    public String id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int minimumDepth() {
        return minimumDepth;
    }

    public int maximumDepth() {
        return maximumDepth;
    }

    public boolean acceptsDepth(int depth) {
        return depth >= minimumDepth && depth <= maximumDepth;
    }

    public static NuclearFacilityType byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (NuclearFacilityType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown nuclear facility type: " + id);
    }
}
