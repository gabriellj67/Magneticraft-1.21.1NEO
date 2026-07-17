package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearReactorSnapshotCodecTest {
    @Test
    void roundTripsCompleteImmutableStructureSnapshot() {
        int width = 9;
        int length = 11;
        int height = 13;
        Direction facing = Direction.WEST;
        BlockPos controller = new BlockPos(-22, 80, 41);
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns =
                NuclearReactorStructureTest.columns(width, length);
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells = new java.util.HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    var expected = NuclearReactorStructure.expectedPart(
                            width, length, height, x, y, z, columns, facing);
                    cells.put(NuclearReactorStructure.worldPosition(controller, facing, width, x, y, z),
                            new NuclearReactorStructure.ObservedPart(
                                    expected.kind(), expected.columnType(), expected.facing()));
                }
            }
        }
        var original = NuclearReactorStructure.validateExact(
                controller, facing, width, length, height, cells::get).snapshot().orElseThrow();
        var restored = NuclearReactorSnapshotCodec.load(
                NuclearReactorSnapshotCodec.save(original), controller).orElseThrow();

        assertEquals(original.facing(), restored.facing());
        assertEquals(original.columns(), restored.columns());
        assertEquals(original.ports(), restored.ports());
        assertEquals(original.minimum(), restored.minimum());
        assertEquals(original.maximum(), restored.maximum());
        assertEquals(width * length * height, restored.members().size());
    }

    @Test
    void rejectsFutureSchemaAndIncompleteColumnStateWithoutPartialRecovery() {
        int width = 7;
        int length = 7;
        int height = 7;
        Direction facing = Direction.NORTH;
        BlockPos controller = BlockPos.ZERO;
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns =
                NuclearReactorStructureTest.columns(width, length);
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells =
                NuclearReactorStructureTest.structure(width, length, height, facing, columns);
        var snapshot = NuclearReactorStructure.validateExact(
                new BlockPos(38, 72, -19), facing, width, length, height, cells::get)
                .snapshot().orElseThrow();
        CompoundTag future = NuclearReactorSnapshotCodec.save(snapshot);
        future.putInt("schema_version", 2);
        assertTrue(NuclearReactorSnapshotCodec.load(future, controller).isEmpty());

        CompoundTag incomplete = NuclearReactorSnapshotCodec.save(snapshot);
        ListTag columnTags = incomplete.getList("columns", Tag.TAG_COMPOUND);
        columnTags.remove(0);
        assertTrue(NuclearReactorSnapshotCodec.load(incomplete, controller).isEmpty());
    }
}
