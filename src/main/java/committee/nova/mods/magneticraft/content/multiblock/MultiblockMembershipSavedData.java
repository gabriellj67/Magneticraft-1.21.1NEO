package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Durable per-dimension ownership for multiblock structure members. */
public final class MultiblockMembershipSavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    private static final String DATA_NAME = "magneticraft_multiblock_membership";
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String MEMBERSHIPS_TAG = "memberships";
    private static final String MEMBER_POSITION_TAG = "member_position";
    private static final String CONTROLLER_POSITION_TAG = "controller_position";
    private static final String DEFINITION_TAG = "definition";

    private final Map<BlockPos, Membership> memberships = new HashMap<>();

    private static final SavedData.Factory<MultiblockMembershipSavedData> FACTORY = new SavedData.Factory<>(
            MultiblockMembershipSavedData::new,
            MultiblockMembershipSavedData::load,
            null
    );

    public static MultiblockMembershipSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static MultiblockMembershipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MultiblockMembershipSavedData data = new MultiblockMembershipSavedData();
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(MEMBERSHIPS_TAG, Tag.TAG_LIST)) {
            return data;
        }
        ListTag memberships = tag.getList(MEMBERSHIPS_TAG, Tag.TAG_COMPOUND);
        for (Tag rawMembership : memberships) {
            if (!(rawMembership instanceof CompoundTag membership)
                    || !membership.contains(MEMBER_POSITION_TAG, Tag.TAG_LONG)
                    || !membership.contains(CONTROLLER_POSITION_TAG, Tag.TAG_LONG)
                    || !membership.contains(DEFINITION_TAG, Tag.TAG_STRING)) {
                continue;
            }
            String definition = membership.getString(DEFINITION_TAG);
            if (definition.isBlank()) {
                continue;
            }
            BlockPos member = BlockPos.of(membership.getLong(MEMBER_POSITION_TAG));
            BlockPos controller = BlockPos.of(membership.getLong(CONTROLLER_POSITION_TAG));
            data.memberships.putIfAbsent(member, new Membership(controller, definition));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ListTag encoded = new ListTag();
        memberships.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .forEach(entry -> {
                    CompoundTag membership = new CompoundTag();
                    membership.putLong(MEMBER_POSITION_TAG, entry.getKey().asLong());
                    membership.putLong(CONTROLLER_POSITION_TAG, entry.getValue().controller().asLong());
                    membership.putString(DEFINITION_TAG, entry.getValue().definition());
                    encoded.add(membership);
                });
        tag.put(MEMBERSHIPS_TAG, encoded);
        return tag;
    }

    public Optional<Membership> membershipAt(BlockPos member) {
        return Optional.ofNullable(memberships.get(member));
    }

    /** Atomically replaces one controller's projection when no other owner conflicts. */
    public boolean claim(BlockPos controller, String definition, List<BlockPos> members) {
        BlockPos stableController = controller.immutable();
        Set<BlockPos> stableMembers = new HashSet<>();
        for (BlockPos member : members) {
            BlockPos stableMember = member.immutable();
            Membership existing = memberships.get(stableMember);
            if (existing != null && !existing.controller().equals(stableController)) {
                return false;
            }
            stableMembers.add(stableMember);
        }

        boolean changed = memberships.entrySet().removeIf(entry ->
                entry.getValue().controller().equals(stableController) && !stableMembers.contains(entry.getKey()));
        Membership replacement = new Membership(stableController, definition);
        for (BlockPos member : stableMembers) {
            if (!replacement.equals(memberships.put(member, replacement))) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
        return true;
    }

    public boolean releaseMember(BlockPos member, BlockPos controller) {
        Membership existing = memberships.get(member);
        if (existing == null || !existing.controller().equals(controller)) {
            return false;
        }
        memberships.remove(member);
        setDirty();
        return true;
    }

    public int releaseController(BlockPos controller) {
        int before = memberships.size();
        memberships.entrySet().removeIf(entry -> entry.getValue().controller().equals(controller));
        int removed = before - memberships.size();
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public int size() {
        return memberships.size();
    }

    public record Membership(BlockPos controller, String definition) {
        public Membership {
            controller = controller.immutable();
            if (definition.isBlank()) {
                throw new IllegalArgumentException("Multiblock definition must not be blank");
            }
        }
    }
}
