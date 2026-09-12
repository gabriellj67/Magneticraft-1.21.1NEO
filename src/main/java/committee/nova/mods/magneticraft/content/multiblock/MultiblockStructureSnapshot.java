package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Captures formed member blocks so the invisible gap projection is lossless and restart-safe. */
final class MultiblockStructureSnapshot {
    private static final String POSITION_TAG = "position";
    private static final String STATE_TAG = "state";
    private static final String BLOCK_ENTITY_TAG = "block_entity";

    private final Map<BlockPos, Entry> entries = new LinkedHashMap<>();

    boolean captureAndHide(ServerLevel level, BlockPos controller, List<BlockPos> members) {
        if (!entries.isEmpty()) {
            return false;
        }
        for (BlockPos member : members) {
            if (member.equals(controller)) {
                continue;
            }
            if (!level.hasChunk(member.getX() >> 4, member.getZ() >> 4)) {
                entries.clear();
                return false;
            }
            BlockState state = level.getBlockState(member);
            if (state.is(ModAdvancedBlocks.MULTIBLOCK_GAP.get())) {
                entries.clear();
                return false;
            }
            BlockEntity blockEntity = level.getBlockEntity(member);
            entries.put(
                    member.immutable(),
                    new Entry(state, blockEntity == null
                            ? null
                            : blockEntity.saveWithFullMetadata(level.registryAccess()))
            );
        }

        BlockState gap = ModAdvancedBlocks.MULTIBLOCK_GAP.get().defaultBlockState();
        for (BlockPos member : entries.keySet()) {
            if (!level.setBlock(member, gap, Block.UPDATE_ALL)) {
                restore(level, null);
                return false;
            }
        }
        return true;
    }

    void restore(ServerLevel level, @Nullable BlockPos brokenMember) {
        for (Map.Entry<BlockPos, Entry> saved : entries.entrySet()) {
            BlockPos position = saved.getKey();
            if (position.equals(brokenMember)
                    || !level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                continue;
            }
            Entry entry = saved.getValue();
            level.setBlock(position, entry.state(), Block.UPDATE_ALL);
            if (entry.blockEntity() != null) {
                BlockEntity restored = BlockEntity.loadStatic(
                        position, entry.state(), entry.blockEntity().copy(), level.registryAccess()
                );
                if (restored != null) {
                    level.setBlockEntity(restored);
                    restored.setChanged();
                }
            }
        }
        entries.clear();
    }

    boolean contains(BlockPos position) {
        return entries.containsKey(position);
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    ListTag save() {
        ListTag encoded = new ListTag();
        for (Map.Entry<BlockPos, Entry> saved : entries.entrySet()) {
            CompoundTag element = new CompoundTag();
            element.putLong(POSITION_TAG, saved.getKey().asLong());
            element.put(STATE_TAG, NbtUtils.writeBlockState(saved.getValue().state()));
            if (saved.getValue().blockEntity() != null) {
                element.put(BLOCK_ENTITY_TAG, saved.getValue().blockEntity().copy());
            }
            encoded.add(element);
        }
        return encoded;
    }

    void load(ListTag encoded) {
        entries.clear();
        for (Tag raw : encoded) {
            if (!(raw instanceof CompoundTag element)
                    || !element.contains(POSITION_TAG, Tag.TAG_LONG)
                    || !element.contains(STATE_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            BlockPos position = BlockPos.of(element.getLong(POSITION_TAG));
            BlockState state = NbtUtils.readBlockState(
                    BuiltInRegistries.BLOCK.asLookup(),
                    element.getCompound(STATE_TAG)
            );
            CompoundTag blockEntity = element.contains(BLOCK_ENTITY_TAG, Tag.TAG_COMPOUND)
                    ? element.getCompound(BLOCK_ENTITY_TAG).copy()
                    : null;
            entries.putIfAbsent(position, new Entry(state, blockEntity));
        }
    }

    private record Entry(BlockState state, @Nullable CompoundTag blockEntity) {
    }
}
