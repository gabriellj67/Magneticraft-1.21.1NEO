package committee.nova.mods.magneticraft.content.machine.framework;

import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.Direction;

/** Narrow visual/physical connection contract shared by machines and multiblock proxies. */
public interface NetworkConnectionHost {
    boolean supportsNetworkConnection(NetworkDomain domain, Direction side);
}
