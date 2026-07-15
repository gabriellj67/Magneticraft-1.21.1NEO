package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.Objects;

/** Stable identity of one physical-network terminal at a world position. */
public record PhysicalNodeKey(BlockPos position, ResourceLocation terminalId)
        implements Comparable<PhysicalNodeKey> {
    public static final ResourceLocation MAIN_TERMINAL = Magneticraft.id("main");
    public static final Comparator<PhysicalNodeKey> ORDER = Comparator
            .comparingInt((PhysicalNodeKey key) -> key.position().getX())
            .thenComparingInt(key -> key.position().getY())
            .thenComparingInt(key -> key.position().getZ())
            .thenComparing(key -> key.terminalId().toString());

    public PhysicalNodeKey {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(terminalId, "terminalId");
    }

    public static PhysicalNodeKey main(BlockPos position) {
        return new PhysicalNodeKey(position, MAIN_TERMINAL);
    }

    @Override
    public int compareTo(PhysicalNodeKey other) {
        return ORDER.compare(this, other);
    }
}
