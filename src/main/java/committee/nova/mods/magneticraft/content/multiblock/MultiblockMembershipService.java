package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Resolves durable member ownership without forcing controller chunks to load.
 */
public final class MultiblockMembershipService {
    private MultiblockMembershipService() {
    }

    public static boolean register(
            ServerLevel level,
            BlockPos controller,
            MultiblockDefinition definition,
            List<BlockPos> members
    ) {
        MultiblockMembershipSavedData data = MultiblockMembershipSavedData.get(level);
        for (BlockPos member : members) {
            MultiblockMembershipSavedData.Membership existing = data.membershipAt(member).orElse(null);
            if (existing == null || existing.controller().equals(controller)) {
                continue;
            }
            if (ownerRemainsAuthoritative(level, member, existing)) {
                return false;
            }
            data.releaseMember(member, existing.controller());
        }
        return data.claim(controller, definition.id(), members);
    }

    public static void unregister(ServerLevel level, BlockPos controller) {
        MultiblockMembershipSavedData.get(level).releaseController(controller);
    }

    @Nullable
    public static BlockPos controllerAt(ServerLevel level, BlockPos member) {
        return MultiblockMembershipSavedData.get(level).membershipAt(member)
                .map(MultiblockMembershipSavedData.Membership::controller)
                .orElse(null);
    }

    private static boolean ownerRemainsAuthoritative(
            ServerLevel level,
            BlockPos member,
            MultiblockMembershipSavedData.Membership ownership
    ) {
        BlockPos controller = ownership.controller();
        var chunk = level.getChunkSource().getChunkNow(controller.getX() >> 4, controller.getZ() >> 4);
        if (chunk == null) {
            return true;
        }
        if (!(chunk.getBlockEntity(controller) instanceof AdvancedMultiblockBlockEntity blockEntity)
                || !blockEntity.formed()
                || !blockEntity.definition().id().equals(ownership.definition())) {
            MultiblockMembershipSavedData.get(level).releaseController(controller);
            return false;
        }
        return blockEntity.members().contains(member);
    }
}
