package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Rebuildable runtime cache keyed by actual ServerLevel identity.
 */
public final class PhysicalNetworkService {
    private static final Map<ServerLevel, PhysicalNetworkManager> MANAGERS = new WeakHashMap<>();

    private PhysicalNetworkService() {
    }

    public static synchronized PhysicalNetworkManager manager(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, PhysicalNetworkManager::new);
    }

    public static synchronized void discard(ServerLevel level) {
        MANAGERS.remove(level);
    }

    public static synchronized void discardAll() {
        MANAGERS.clear();
    }

    /** Rebinds only loaded nodes; unloaded chunks remain untouched and are never force-loaded. */
    public static synchronized void onElectricalProfilesReloaded(
            ElectricalDataSnapshot snapshot,
            int graceTicks
    ) {
        MANAGERS.values().forEach(manager -> manager.onElectricalProfilesReloaded(snapshot, graceTicks));
    }

    static synchronized int managerCount() {
        return MANAGERS.size();
    }
}
