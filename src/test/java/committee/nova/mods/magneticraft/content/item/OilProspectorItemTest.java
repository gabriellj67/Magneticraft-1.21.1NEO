package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OilProspectorItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void reportsAllEightStableHorizontalDirectionsWithoutCoordinates() {
        assertEquals("here", OilProspectorItem.directionId(0, 0));
        assertEquals("east", OilProspectorItem.directionId(1, 0));
        assertEquals("southeast", OilProspectorItem.directionId(1, 1));
        assertEquals("south", OilProspectorItem.directionId(0, 1));
        assertEquals("southwest", OilProspectorItem.directionId(-1, 1));
        assertEquals("west", OilProspectorItem.directionId(-1, 0));
        assertEquals("northwest", OilProspectorItem.directionId(-1, -1));
        assertEquals("north", OilProspectorItem.directionId(0, -1));
        assertEquals("northeast", OilProspectorItem.directionId(1, -1));
    }

    @Test
    void roundsCenterDistanceToTheNearestEightBlocks() {
        assertEquals(0, OilProspectorItem.roundedDistance(3, 0));
        assertEquals(8, OilProspectorItem.roundedDistance(4, 0));
        assertEquals(16, OilProspectorItem.roundedDistance(12, 0));
        assertEquals(128, OilProspectorItem.roundedDistance(90, 90));
    }
}
