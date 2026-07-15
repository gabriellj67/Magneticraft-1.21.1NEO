package committee.nova.mods.magneticraft.system.network.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

/**
 * Loaded-world adapter around one authoritative, block-entity-owned node.
 */
public interface PhysicalNetworkNode {
    NetworkDomain domain();

    BlockPos position();

    default PhysicalNodeKey nodeKey() {
        return PhysicalNodeKey.main(position());
    }

    Set<Direction> connectionSides();

    /** Resolves a position-only proxy to the authoritative state-owning node. */
    default PhysicalNetworkNode transferNode() {
        return this;
    }

    default boolean canConnect(Direction side, PhysicalNetworkNode other) {
        return domain() == other.domain()
                && connectionSides().contains(side)
                && other.connectionSides().contains(side.getOpposite());
    }

    default void beforeNetworkTick(PhysicalNetworkManager manager) {
    }

    default void exchangeWith(PhysicalNetworkNode other) {
    }

    default void afterNetworkTick(PhysicalNetworkManager manager) {
    }
}
