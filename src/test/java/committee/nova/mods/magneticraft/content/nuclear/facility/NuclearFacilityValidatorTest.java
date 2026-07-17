package committee.nova.mods.magneticraft.content.nuclear.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearFacilityValidatorTest {
    private static final BlockPos CONTROLLER = new BlockPos(31, 70, -12);

    @Test
    void fixedFacilitiesValidateInEveryHorizontalRotation() {
        for (NuclearFacilityType type : new NuclearFacilityType[]{
                NuclearFacilityType.URANIUM_PROCESSOR,
                NuclearFacilityType.FUEL_FABRICATOR
        }) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                int depth = type.minimumDepth();
                Map<BlockPos, NuclearFacilityValidator.ObservedCell> cells = structure(type, facing, depth);
                NuclearFacilityValidator.Result result = NuclearFacilityValidator.validate(
                        type, CONTROLLER, facing, cells::get);
                assertTrue(result.snapshot().isPresent(), () -> type + " / " + facing + ": " + result.reason());
                NuclearFacilitySnapshot snapshot = result.snapshot().orElseThrow();
                assertEquals(depth, snapshot.depth());
                assertEquals(type.width() * type.height() * depth, new HashSet<>(snapshot.members()).size());
                assertEquals(3, snapshot.ports().size());
            }
        }
    }

    @Test
    void cascadeAcceptsBothDepthBoundsAndCountsStages() {
        NuclearFacilityType type = NuclearFacilityType.CENTRIFUGE_CASCADE;
        for (int depth : new int[]{type.minimumDepth(), type.maximumDepth()}) {
            Map<BlockPos, NuclearFacilityValidator.ObservedCell> cells = structure(type, Direction.WEST, depth);
            NuclearFacilityValidator.Result result = NuclearFacilityValidator.validate(
                    type, CONTROLLER, Direction.WEST, cells::get);
            assertTrue(result.snapshot().isPresent(), result.reason());
            long stages = result.snapshot().orElseThrow().members().stream()
                    .filter(position -> cells.get(position).role() == NuclearFacilityPartRole.CENTRIFUGE_STAGE)
                    .count();
            assertEquals((depth - 2L) * 2L, stages);
        }
    }

    @Test
    void rejectsWrongPortFaceWithoutAcceptingAnotherDepth() {
        NuclearFacilityType type = NuclearFacilityType.CENTRIFUGE_CASCADE;
        Map<BlockPos, NuclearFacilityValidator.ObservedCell> cells = structure(type, Direction.SOUTH, 7);
        BlockPos electrical = NuclearFacilityValidator.worldPosition(type, CONTROLLER, Direction.SOUTH,
                type.width() / 2, 1, 6);
        cells.put(electrical, new NuclearFacilityValidator.ObservedCell(
                NuclearFacilityPartRole.ELECTRICAL, Direction.SOUTH));

        NuclearFacilityValidator.Result result = NuclearFacilityValidator.validate(
                type, CONTROLLER, Direction.SOUTH, cells::get);
        assertFalse(result.snapshot().isPresent());
    }

    @Test
    void rejectsCascadeBeyondMaximumDepth() {
        NuclearFacilityType type = NuclearFacilityType.CENTRIFUGE_CASCADE;
        Map<BlockPos, NuclearFacilityValidator.ObservedCell> cells = structure(
                type, Direction.NORTH, type.maximumDepth() + 1);
        NuclearFacilityValidator.Result result = NuclearFacilityValidator.validate(
                type, CONTROLLER, Direction.NORTH, cells::get);
        assertFalse(result.snapshot().isPresent());
    }

    private static Map<BlockPos, NuclearFacilityValidator.ObservedCell> structure(
            NuclearFacilityType type, Direction facing, int depth
    ) {
        Map<BlockPos, NuclearFacilityValidator.ObservedCell> cells = new HashMap<>();
        for (int y = 0; y < type.height(); y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < type.width(); x++) {
                    NuclearFacilityPartRole role = NuclearFacilityValidator.expectedRole(type, x, y, z, depth);
                    Direction portFacing = role.isPort()
                            ? role == NuclearFacilityPartRole.ELECTRICAL ? facing.getOpposite() : facing
                            : null;
                    cells.put(
                            NuclearFacilityValidator.worldPosition(type, CONTROLLER, facing, x, y, z),
                            new NuclearFacilityValidator.ObservedCell(role, portFacing)
                    );
                }
            }
        }
        return cells;
    }
}
