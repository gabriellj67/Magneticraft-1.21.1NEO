package committee.nova.mods.magneticraft.system.network.longdistance;

import java.util.Arrays;
import java.util.Optional;

/**
 * A physical wire terminal. Ports with different conductor layouts cannot be
 * joined, even when both are hosted by the same kind of block.
 */
public enum LongDistancePort {
    CONNECTOR("connector", 8.0D, 1),
    POLE("pole", 16.0D, 3);

    private final String serializedName;
    private final double maxDistance;
    private final int wireCount;

    LongDistancePort(String serializedName, double maxDistance, int wireCount) {
        this.serializedName = serializedName;
        this.maxDistance = maxDistance;
        this.wireCount = wireCount;
    }

    public String serializedName() {
        return serializedName;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public int wireCount() {
        return wireCount;
    }

    public static Optional<LongDistancePort> byName(String name) {
        return Arrays.stream(values()).filter(port -> port.serializedName.equals(name)).findFirst();
    }
}
