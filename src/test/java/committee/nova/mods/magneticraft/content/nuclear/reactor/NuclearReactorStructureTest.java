package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearReactorStructureTest {
    private static final BlockPos CONTROLLER = new BlockPos(38, 72, -19);

    @Test
    void validatesBothSizeBoundsInEveryHorizontalRotation() {
        int[][] sizes = {{7, 7, 7}, {7, 13, 15}, {13, 7, 15}, {13, 13, 7}};
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int[] size : sizes) {
                Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = columns(size[0], size[1]);
                Map<BlockPos, NuclearReactorStructure.ObservedPart> cells =
                        structure(size[0], size[1], size[2], facing, columns);
                NuclearReactorStructure.Result result = NuclearReactorStructure.validateExact(
                        CONTROLLER, facing, size[0], size[1], size[2], cells::get);
                assertTrue(result.snapshot().isPresent(), () -> facing + ": " + result.reason());
                var snapshot = result.snapshot().orElseThrow();
                assertEquals(size[0] * size[1] * size[2], new HashSet<>(snapshot.members()).size());
                assertEquals((size[0] - 4) * (size[1] - 4), snapshot.columns().size());
                assertEquals(4, snapshot.ports().size());
                assertEquals(facing, snapshot.facing());
            }
        }
    }

    @Test
    void rejectsMissingRequiredColumnBeforeAcceptingShell() {
        int width = 7;
        int length = 7;
        int height = 7;
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = new HashMap<>();
        for (int z = 0; z < length - 4; z++) {
            for (int x = 0; x < width - 4; x++) {
                columns.put(new ReactorColumnCoordinate(x, z), NuclearReactorColumnType.FUEL_STANDARD);
            }
        }
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells =
                structure(width, length, height, Direction.NORTH, columns);
        NuclearReactorStructure.Result result = NuclearReactorStructure.validateExact(
                CONTROLLER, Direction.NORTH, width, length, height, cells::get);
        assertFalse(result.snapshot().isPresent());
        assertEquals("required_columns", result.reason());
    }

    @Test
    void validatesPairedPortFacesAndBothControlActuators() {
        int width = 7;
        int length = 7;
        int height = 9;
        Direction facing = Direction.EAST;
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = columns(width, length);
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells = structure(width, length, height, facing, columns);

        BlockPos innerCoolant = NuclearReactorStructure.worldPosition(CONTROLLER, facing, width, 1, 1, 1);
        cells.put(innerCoolant, new NuclearReactorStructure.ObservedPart(
                NuclearReactorStructure.PartKind.COOLANT_PORT, null, facing.getOpposite()));
        NuclearReactorStructure.Result wrongPort = NuclearReactorStructure.validateExact(
                CONTROLLER, facing, width, length, height, cells::get);
        assertEquals("expected_coolant_port", wrongPort.reason());

        cells = structure(width, length, height, facing, columns);
        ReactorColumnCoordinate control = new ReactorColumnCoordinate(1, 0);
        BlockPos topActuator = NuclearReactorStructure.worldPosition(
                CONTROLLER, facing, width, control.x() + 2, height - 1, control.z() + 2);
        cells.put(topActuator, new NuclearReactorStructure.ObservedPart(
                NuclearReactorStructure.PartKind.CONTAINMENT_CASING, null, null));
        NuclearReactorStructure.Result wrongActuator = NuclearReactorStructure.validateExact(
                CONTROLLER, facing, width, length, height, cells::get);
        assertEquals("expected_control_rod_actuator", wrongActuator.reason());
    }

    @Test
    void refusesUnloadedCellsWithoutForcingChunks() {
        int width = 7;
        int length = 7;
        int height = 7;
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = columns(width, length);
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells =
                structure(width, length, height, Direction.SOUTH, columns);
        BlockPos unavailable = NuclearReactorStructure.worldPosition(
                CONTROLLER, Direction.SOUTH, width, 2, 2, 2);
        NuclearReactorStructure.PartLookup lookup = new NuclearReactorStructure.PartLookup() {
            @Override
            public NuclearReactorStructure.ObservedPart partAt(BlockPos position) {
                return cells.get(position);
            }

            @Override
            public boolean isLoaded(BlockPos position) {
                return !position.equals(unavailable);
            }
        };
        NuclearReactorStructure.Result result = NuclearReactorStructure.validateExact(
                CONTROLLER, Direction.SOUTH, width, length, height, lookup);
        assertEquals("unloaded", result.reason());
        assertEquals(unavailable, result.position());
    }

    static Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns(int width, int length) {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> result = new HashMap<>();
        NuclearReactorColumnType[] required = {
                NuclearReactorColumnType.FUEL_STANDARD,
                NuclearReactorColumnType.CONTROL_ROD_A,
                NuclearReactorColumnType.COOLANT_CHANNEL,
                NuclearReactorColumnType.INSTRUMENTATION
        };
        int index = 0;
        for (int z = 0; z < length - 4; z++) {
            for (int x = 0; x < width - 4; x++) {
                NuclearReactorColumnType type = index < required.length
                        ? required[index]
                        : index % 3 == 0 ? NuclearReactorColumnType.REFLECTOR
                        : NuclearReactorColumnType.FUEL_STANDARD;
                result.put(new ReactorColumnCoordinate(x, z), type);
                index++;
            }
        }
        return result;
    }

    static Map<BlockPos, NuclearReactorStructure.ObservedPart> structure(
            int width,
            int length,
            int height,
            Direction facing,
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns
    ) {
        Map<BlockPos, NuclearReactorStructure.ObservedPart> cells = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    NuclearReactorStructure.ExpectedPart expected = NuclearReactorStructure.expectedPart(
                            width, length, height, x, y, z, columns, facing);
                    cells.put(NuclearReactorStructure.worldPosition(CONTROLLER, facing, width, x, y, z),
                            new NuclearReactorStructure.ObservedPart(
                                    expected.kind(), expected.columnType(), expected.facing()));
                }
            }
        }
        return cells;
    }
}
