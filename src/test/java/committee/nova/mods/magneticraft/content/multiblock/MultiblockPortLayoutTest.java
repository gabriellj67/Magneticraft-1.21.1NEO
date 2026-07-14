package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockPortLayoutTest {
    @Test
    void rotatesLegacyPortCoordinatesAndSidesWithControllerFacing() {
        MultiblockPortLayout.Port port = MultiblockPortLayout.ports(MultiblockDefinition.OIL_HEATER)
                .stream()
                .filter(candidate -> candidate.kind() == MultiblockPortLayout.Kind.FLUID)
                .findFirst()
                .orElseThrow();

        assertEquals(new BlockPos(10, 65, 12), port.worldPosition(
                new BlockPos(10, 64, 10), Direction.NORTH));
        assertEquals(Direction.SOUTH, port.worldSide(Direction.NORTH));
        assertEquals(new BlockPos(8, 65, 10), port.worldPosition(
                new BlockPos(10, 64, 10), Direction.EAST));
        assertEquals(Direction.WEST, port.worldSide(Direction.EAST));
    }

    @Test
    void everyNonRemotePortTargetsTheControllerOrADeclaredStructureMember() {
        BlockPos controller = BlockPos.ZERO;
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                Set<BlockPos> members = new HashSet<>();
                for (MultiblockCell cell : definition.memberCells()) {
                    members.add(MultiblockTransform.worldPosition(
                            controller, cell.offset(), definition.center(), facing, false
                    ));
                }
                for (MultiblockPortLayout.Port port : MultiblockPortLayout.ports(definition)) {
                    BlockPos position = port.worldPosition(controller, facing);
                    boolean releasedRemoteSolarPort = definition == MultiblockDefinition.SOLAR_PANEL
                            && port.offset().equals(new StructureOffset(0, 0, -5));
                    assertTrue(releasedRemoteSolarPort || members.contains(position), () ->
                            definition + " port " + port + " maps outside structure at " + position
                                    + " while facing " + facing);
                }
            }
        }
    }
}
