package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;

import java.util.Arrays;
import java.util.Optional;

/**
 * A physical wire terminal. Ports with different conductor layouts cannot be
 * joined, even when both are hosted by the same kind of block.
 */
public enum LongDistancePort {
    CONNECTOR("connector", 1),
    POLE("pole", 3);

    private final String serializedName;
    private final int wireCount;

    LongDistancePort(String serializedName, int wireCount) {
        this.serializedName = serializedName;
        this.wireCount = wireCount;
    }

    public String serializedName() {
        return serializedName;
    }

    public int maxDistance(VoltageTier tier) {
        return this == CONNECTOR ? tier.connectorRange() : tier.poleRange();
    }

    public int wireCount() {
        return wireCount;
    }

    public static Optional<LongDistancePort> byName(String name) {
        return Arrays.stream(values()).filter(port -> port.serializedName.equals(name)).findFirst();
    }
}
