package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Complete durable terminal identity inside a single dimension. */
public record LongDistanceEndpoint(
        BlockPos position,
        ResourceLocation terminalId,
        LongDistancePort port,
        ResourceLocation tierId
) {
    public LongDistanceEndpoint {
        position = Objects.requireNonNull(position).immutable();
        terminalId = Objects.requireNonNull(terminalId);
        port = Objects.requireNonNull(port);
        tierId = Objects.requireNonNull(tierId);
    }
}
