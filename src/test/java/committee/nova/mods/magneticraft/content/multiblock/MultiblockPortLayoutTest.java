package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockPortLayoutTest {
    private static final Map<MultiblockDefinition, Integer> RELEASED_PORT_COUNTS = Map.ofEntries(
            Map.entry(MultiblockDefinition.SOLAR_PANEL, 2),
            Map.entry(MultiblockDefinition.STEAM_ENGINE, 8),
            Map.entry(MultiblockDefinition.STEAM_TURBINE, 9),
            Map.entry(MultiblockDefinition.GRINDER, 12),
            Map.entry(MultiblockDefinition.SIEVE, 7),
            Map.entry(MultiblockDefinition.HYDRAULIC_PRESS, 4),
            Map.entry(MultiblockDefinition.PUMPJACK, 2),
            Map.entry(MultiblockDefinition.OIL_HEATER, 11),
            Map.entry(MultiblockDefinition.POLYMERIZER, 4),
            Map.entry(MultiblockDefinition.REFINERY, 15),
            Map.entry(MultiblockDefinition.SOLAR_TOWER, 1),
            Map.entry(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, 13),
            Map.entry(MultiblockDefinition.BIG_STEAM_BOILER, 141),
            Map.entry(MultiblockDefinition.BIG_ELECTRIC_FURNACE, 4)
    );

    @Test
    void exactTypedConnectionCatalogueMatchesTheReleasedMachines() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            assertEquals(
                    RELEASED_PORT_COUNTS.getOrDefault(definition, 0),
                    MultiblockPortLayout.ports(definition).size(),
                    definition.id()
            );
        }
    }

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
    void polymerizerExposesOnlyItsFourDeclaredControllerRelativePorts() {
        assertEquals(
                Set.of(
                        "ITEM@-1,1,-1:WEST",
                        "ITEM@1,1,-1:EAST",
                        "HEAT@0,3,-2:NORTH",
                        "FLUID@0,4,-1:UP"
                ),
                MultiblockPortLayout.ports(MultiblockDefinition.POLYMERIZER).stream()
                        .map(port -> port.kind() + "@" + port.offset().x() + "," + port.offset().y() + ","
                                + port.offset().z() + ":" + port.side().name())
                        .collect(java.util.stream.Collectors.toSet())
        );
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
