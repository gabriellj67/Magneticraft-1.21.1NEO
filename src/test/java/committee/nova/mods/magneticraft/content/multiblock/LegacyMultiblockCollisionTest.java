package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMultiblockCollisionTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void cataloguePreservesEveryReleasedCollisionBox() {
        Map<MultiblockDefinition, Integer> expected = expectedCounts();
        assertEquals(MultiblockDefinition.values().length, expected.size());
        expected.forEach((definition, count) -> {
            List<AABB> boxes = LegacyMultiblockCollision.authoredBoxes(definition);
            assertEquals(count, boxes.size(), definition::name);
            boxes.forEach(box -> {
                assertTrue(Double.isFinite(box.minX) && Double.isFinite(box.minY)
                        && Double.isFinite(box.minZ) && Double.isFinite(box.maxX)
                        && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ), definition::name);
                assertTrue(box.minX <= box.maxX && box.minY <= box.maxY && box.minZ <= box.maxZ,
                        definition::name);
            });
        });
    }

    @Test
    void currentNorthFacingRetainsReleasedAuthoredCoordinates() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            List<AABB> authored = LegacyMultiblockCollision.authoredBoxes(definition);
            List<AABB> oriented = LegacyMultiblockCollision.controllerRelativeBoxes(
                    definition, Direction.NORTH
            );
            assertEquals(authored.size(), oriented.size(), definition::name);
            for (int index = 0; index < authored.size(); index++) {
                assertBoxEquals(authored.get(index), oriented.get(index), definition);
            }
        }
    }

    @Test
    void everyDefinitionProducesBoundedPerMemberShapesInAllRotations() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                boolean foundCollision = false;
                for (MultiblockCell cell : definition.memberCells()) {
                    StructureOffset relative = MultiblockTransform.relative(
                            cell.offset(), definition.center(), facing, false
                    );
                    VoxelShape shape = LegacyMultiblockCollision.shapeAt(
                            new BlockPos(relative.x(), relative.y(), relative.z()),
                            BlockPos.ZERO,
                            definition,
                            facing
                    );
                    if (shape.isEmpty()) {
                        continue;
                    }
                    foundCollision = true;
                    AABB bounds = shape.bounds();
                    assertTrue(bounds.minX >= -EPSILON && bounds.minY >= -EPSILON
                                    && bounds.minZ >= -EPSILON,
                            definition + " " + facing + " escaped the member minimum");
                    assertTrue(bounds.maxX <= 1.0D + EPSILON && bounds.maxY <= 1.0D + EPSILON
                                    && bounds.maxZ <= 1.0D + EPSILON,
                            definition + " " + facing + " escaped the member maximum");
                }
                assertTrue(foundCollision, definition + " " + facing + " has no hosted collision");
            }
        }
    }

    private static void assertBoxEquals(AABB expected, AABB actual, MultiblockDefinition definition) {
        assertEquals(expected.minX, actual.minX, EPSILON, definition::name);
        assertEquals(expected.minY, actual.minY, EPSILON, definition::name);
        assertEquals(expected.minZ, actual.minZ, EPSILON, definition::name);
        assertEquals(expected.maxX, actual.maxX, EPSILON, definition::name);
        assertEquals(expected.maxY, actual.maxY, EPSILON, definition::name);
        assertEquals(expected.maxZ, actual.maxZ, EPSILON, definition::name);
    }

    private static Map<MultiblockDefinition, Integer> expectedCounts() {
        EnumMap<MultiblockDefinition, Integer> counts = new EnumMap<>(MultiblockDefinition.class);
        counts.put(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, 35);
        counts.put(MultiblockDefinition.BIG_ELECTRIC_FURNACE, 29);
        counts.put(MultiblockDefinition.BIG_STEAM_BOILER, 32);
        counts.put(MultiblockDefinition.CONTAINER, 44);
        counts.put(MultiblockDefinition.GRINDER, 56);
        counts.put(MultiblockDefinition.HYDRAULIC_PRESS, 42);
        counts.put(MultiblockDefinition.OIL_HEATER, 15);
        counts.put(MultiblockDefinition.POLYMERIZER, 1);
        counts.put(MultiblockDefinition.PUMPJACK, 85);
        counts.put(MultiblockDefinition.REFINERY, 34);
        counts.put(MultiblockDefinition.SHELVING_UNIT, 4);
        counts.put(MultiblockDefinition.SIEVE, 66);
        counts.put(MultiblockDefinition.SOLAR_MIRROR, 4);
        counts.put(MultiblockDefinition.SOLAR_PANEL, 33);
        counts.put(MultiblockDefinition.SOLAR_TOWER, 12);
        counts.put(MultiblockDefinition.STIRLING_GENERATOR, 1);
        counts.put(MultiblockDefinition.STEAM_ENGINE, 76);
        counts.put(MultiblockDefinition.STEAM_TURBINE, 51);
        return Map.copyOf(counts);
    }
}
