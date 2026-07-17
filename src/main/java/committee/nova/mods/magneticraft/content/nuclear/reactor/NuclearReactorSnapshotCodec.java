package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Strict schema envelope for orientation, bounds, ports and the complete column map. */
final class NuclearReactorSnapshotCodec {
    private static final int SCHEMA_VERSION = 1;

    private NuclearReactorSnapshotCodec() {
    }

    static CompoundTag save(NuclearReactorSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putString("facing", snapshot.facing().getName());
        tag.putInt("width", snapshot.width());
        tag.putInt("length", snapshot.length());
        tag.putInt("height", snapshot.height());
        tag.putLong("minimum", snapshot.minimum().asLong());
        tag.putLong("maximum", snapshot.maximum().asLong());
        ListTag columns = new ListTag();
        snapshot.columns().forEach((coordinate, type) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", coordinate.x());
            entry.putInt("z", coordinate.z());
            entry.putString("type", type.name());
            columns.add(entry);
        });
        tag.put("columns", columns);
        ListTag ports = new ListTag();
        snapshot.ports().forEach((type, position) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("type", type.name());
            entry.putLong("position", position.asLong());
            ports.add(entry);
        });
        tag.put("ports", ports);
        return tag;
    }

    static Optional<NuclearReactorSnapshot> load(CompoundTag tag, BlockPos controller) {
        if (tag.getInt("schema_version") != SCHEMA_VERSION
                || !tag.contains("facing", Tag.TAG_STRING)
                || !tag.contains("columns", Tag.TAG_LIST)
                || !tag.contains("ports", Tag.TAG_LIST)
                || !tag.contains("minimum", Tag.TAG_LONG)
                || !tag.contains("maximum", Tag.TAG_LONG)) {
            return Optional.empty();
        }
        Direction facing = Direction.byName(tag.getString("facing"));
        int width = tag.getInt("width");
        int length = tag.getInt("length");
        int height = tag.getInt("height");
        if (facing == null || facing.getAxis().isVertical()
                || !NuclearReactorStructure.DESCRIPTOR.accepts(width, length, height)) {
            return Optional.empty();
        }
        try {
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = new LinkedHashMap<>();
            ListTag columnTags = tag.getList("columns", Tag.TAG_COMPOUND);
            for (Tag raw : columnTags) {
                CompoundTag entry = (CompoundTag) raw;
                ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(
                        entry.getInt("x"), entry.getInt("z"));
                NuclearReactorColumnType type = NuclearReactorColumnType.valueOf(entry.getString("type"));
                if (coordinate.x() >= width - 4 || coordinate.z() >= length - 4
                        || columns.put(coordinate, type) != null) {
                    return Optional.empty();
                }
            }
            if (columns.size() != (width - 4) * (length - 4)) {
                return Optional.empty();
            }
            Map<NuclearReactorPortType, BlockPos> ports = new EnumMap<>(NuclearReactorPortType.class);
            ListTag portTags = tag.getList("ports", Tag.TAG_COMPOUND);
            for (Tag raw : portTags) {
                CompoundTag entry = (CompoundTag) raw;
                NuclearReactorPortType type = NuclearReactorPortType.valueOf(entry.getString("type"));
                if (ports.put(type, BlockPos.of(entry.getLong("position"))) != null) {
                    return Optional.empty();
                }
            }
            if (ports.size() != NuclearReactorPortType.values().length) {
                return Optional.empty();
            }
            List<BlockPos> members = new ArrayList<>(width * length * height);
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        members.add(NuclearReactorStructure.worldPosition(
                                controller, facing, width, x, y, z));
                    }
                }
            }
            return Optional.of(new NuclearReactorSnapshot(
                    NuclearReactorStructure.DESCRIPTOR,
                    controller,
                    facing,
                    width,
                    length,
                    height,
                    BlockPos.of(tag.getLong("minimum")),
                    BlockPos.of(tag.getLong("maximum")),
                    columns,
                    ports,
                    members
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
