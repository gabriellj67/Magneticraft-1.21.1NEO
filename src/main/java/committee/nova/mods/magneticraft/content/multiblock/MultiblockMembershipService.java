package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Rebuildable loaded-level index from structure members to their controller.
 */
public final class MultiblockMembershipService {
    private static final Map<ServerLevel, Map<BlockPos, BlockPos>> MEMBERS = new WeakHashMap<>();

    private MultiblockMembershipService() {
    }

    public static boolean canRegister(ServerLevel level, BlockPos controller, List<BlockPos> members) {
        Map<BlockPos, BlockPos> index = MEMBERS.computeIfAbsent(level, ignored -> new HashMap<>());
        for (BlockPos member : members) {
            BlockPos existing = index.get(member);
            if (existing != null && !existing.equals(controller)) {
                return false;
            }
        }
        return true;
    }

    public static boolean register(ServerLevel level, BlockPos controller, List<BlockPos> members) {
        BlockPos stableController = controller.immutable();
        if (!canRegister(level, stableController, members)) {
            return false;
        }
        Map<BlockPos, BlockPos> index = MEMBERS.computeIfAbsent(level, ignored -> new HashMap<>());
        for (BlockPos member : members) {
            index.put(member.immutable(), stableController);
        }
        return true;
    }

    public static void unregister(ServerLevel level, BlockPos controller) {
        Map<BlockPos, BlockPos> index = MEMBERS.get(level);
        if (index == null) {
            return;
        }
        index.entrySet().removeIf(entry -> entry.getValue().equals(controller));
        if (index.isEmpty()) {
            MEMBERS.remove(level);
        }
    }

    @Nullable
    public static BlockPos controllerAt(ServerLevel level, BlockPos member) {
        Map<BlockPos, BlockPos> index = MEMBERS.get(level);
        return index == null ? null : index.get(member);
    }
}
