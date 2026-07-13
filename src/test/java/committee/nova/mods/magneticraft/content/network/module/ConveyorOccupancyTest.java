package committee.nova.mods.magneticraft.content.network.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConveyorOccupancyTest {
    @Test
    void modelsFourPixelParcelsOnA16By16Bitmap() {
        ConveyorOccupancy occupancy = new ConveyorOccupancy();
        occupancy.mark(ConveyorRoute.LEFT_FORWARD, 2);

        assertFalse(occupancy.isFree(ConveyorRoute.LEFT_FORWARD, 2));
        assertFalse(occupancy.isFree(ConveyorRoute.LEFT_FORWARD, 5));
        assertTrue(occupancy.isFree(ConveyorRoute.LEFT_FORWARD, 6));
        assertTrue(occupancy.isFree(ConveyorRoute.RIGHT_FORWARD, 2));
    }

    @Test
    void releasesCellsWhenAParcelMoves() {
        ConveyorOccupancy occupancy = new ConveyorOccupancy();
        occupancy.mark(ConveyorRoute.LEFT_FORWARD, 2);
        occupancy.unmark(ConveyorRoute.LEFT_FORWARD, 2);

        assertTrue(occupancy.isFree(ConveyorRoute.LEFT_FORWARD, 2));
    }

    @Test
    void keepsSeparateTurnEntriesFreeButBlocksMergedPaths() {
        ConveyorOccupancy occupancy = new ConveyorOccupancy();
        occupancy.mark(ConveyorRoute.LEFT_CORNER, 0);
        assertTrue(occupancy.isFree(ConveyorRoute.RIGHT_SHORT, 0));

        occupancy = new ConveyorOccupancy();
        occupancy.mark(ConveyorRoute.LEFT_SHORT, 8);
        assertFalse(occupancy.isFree(ConveyorRoute.LEFT_FORWARD, 11));
    }
}
