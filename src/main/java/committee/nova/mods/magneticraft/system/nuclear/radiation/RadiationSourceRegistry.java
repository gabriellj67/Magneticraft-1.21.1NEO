package committee.nova.mods.magneticraft.system.nuclear.radiation;

import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Loaded-source index. It stores positions only and never persists invisible chunk contamination. */
public final class RadiationSourceRegistry {
    private static final Map<ServerLevel, Map<BlockPos, Boolean>> SOURCES = new WeakHashMap<>();

    private RadiationSourceRegistry() {
    }

    public static synchronized void register(ServerLevel level, BlockPos position) {
        SOURCES.computeIfAbsent(level, ignored -> new HashMap<>()).put(position.immutable(), Boolean.TRUE);
    }

    public static synchronized void unregister(ServerLevel level, BlockPos position) {
        Map<BlockPos, Boolean> positions = SOURCES.get(level);
        if (positions != null) {
            positions.remove(position);
        }
    }

    public static synchronized List<RadiationSource> nearby(ServerLevel level, BlockPos center, double range) {
        Map<BlockPos, Boolean> positions = SOURCES.get(level);
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        double rangeSquared = range * range;
        ArrayList<RadiationSource> result = new ArrayList<>();
        for (BlockPos position : List.copyOf(positions.keySet())) {
            if (position.distSqr(center) > rangeSquared || !level.hasChunkAt(position)) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(position);
            if (entity instanceof RadiationSource source) {
                result.add(source);
            } else {
                positions.remove(position);
            }
        }
        return List.copyOf(result);
    }
}
