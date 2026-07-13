package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/** One durable terminal identity inside a single dimension. */
public record LongDistanceEndpoint(BlockPos position, LongDistancePort port) {
    public LongDistanceEndpoint {
        position = Objects.requireNonNull(position).immutable();
        port = Objects.requireNonNull(port);
    }
}
