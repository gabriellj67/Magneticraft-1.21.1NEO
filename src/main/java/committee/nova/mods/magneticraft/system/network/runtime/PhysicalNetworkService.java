package committee.nova.mods.magneticraft.system.network.runtime;

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

    static synchronized int managerCount() {
        return MANAGERS.size();
    }
}
