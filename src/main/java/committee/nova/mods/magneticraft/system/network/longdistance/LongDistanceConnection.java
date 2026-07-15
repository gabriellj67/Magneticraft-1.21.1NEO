package committee.nova.mods.magneticraft.system.network.longdistance;

import java.util.Comparator;
import java.util.Objects;

/** Canonically ordered durable edge, independent from loaded block entities. */
public record LongDistanceConnection(LongDistanceEndpoint first, LongDistanceEndpoint second) {
    private static final Comparator<LongDistanceEndpoint> ENDPOINT_ORDER = Comparator
            .comparingLong((LongDistanceEndpoint endpoint) -> endpoint.position().asLong())
            .thenComparing(endpoint -> endpoint.terminalId().toString())
            .thenComparingInt(endpoint -> endpoint.port().ordinal())
            .thenComparing(endpoint -> endpoint.tierId().toString());

    public LongDistanceConnection {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        if (ENDPOINT_ORDER.compare(first, second) >= 0) {
            throw new IllegalArgumentException("Long-distance endpoints must be distinct and canonically ordered");
        }
    }

    public static LongDistanceConnection of(LongDistanceEndpoint first, LongDistanceEndpoint second) {
        return ENDPOINT_ORDER.compare(first, second) < 0
                ? new LongDistanceConnection(first, second)
                : new LongDistanceConnection(second, first);
    }

    public boolean containsPosition(net.minecraft.core.BlockPos position) {
        return first.position().equals(position) || second.position().equals(position);
    }

    public double distance() {
        return Math.sqrt(first.position().distSqr(second.position()));
    }
}
