package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PumpjackCursorTest {
    private static final int MIN_BUILD_HEIGHT = -64;
    private static final int MAX_BUILD_HEIGHT_EXCLUSIVE = 320;

    @Test
    void searchingOilCoversTheInclusiveRadiusThreeVolumeFromTopToBottom() {
        BlockPos origin = new BlockPos(10, 70, -4);
        PumpjackCursor cursor = PumpjackCursor.searchingOil(origin, MIN_BUILD_HEIGHT);

        assertEquals(new BlockPos(7, MIN_BUILD_HEIGHT, -7), cursor.min());
        assertEquals(new BlockPos(13, 70, -1), cursor.max());
        assertTrue(cursor.descendingY());
        assertEquals(7 * 7 * (70 - MIN_BUILD_HEIGHT + 1), cursor.totalPositions());
        assertEquals(new BlockPos(7, 70, -7), cursor.positionAt(0));
        assertEquals(new BlockPos(13, MIN_BUILD_HEIGHT, -1),
                cursor.positionAt(cursor.totalPositions() - 1));
        assertCompleteAndUnique(cursor);
    }

    @Test
    void depositScanCoversTheInclusiveLegacyFieldBounds() {
        BlockPos depositOrigin = new BlockPos(5, 20, -2);
        PumpjackCursor cursor = PumpjackCursor.depositScan(
                depositOrigin,
                MIN_BUILD_HEIGHT,
                MAX_BUILD_HEIGHT_EXCLUSIVE
        );

        assertEquals(new BlockPos(-27, 13, -34), cursor.min());
        assertEquals(new BlockPos(37, 24, 30), cursor.max());
        assertFalse(cursor.descendingY());
        assertEquals(PumpjackCursor.DEPOSIT_SCAN_MAX_POSITIONS, cursor.totalPositions());
        assertEquals(cursor.min(), cursor.positionAt(0));
        assertEquals(cursor.max(), cursor.positionAt(cursor.totalPositions() - 1));
        assertCompleteAndUnique(cursor);
    }

    @Test
    void sourceScanUsesTheDrillForHorizontalBoundsAndDepositForVerticalBounds() {
        BlockPos searchOrigin = new BlockPos(100, 80, 200);
        BlockPos depositOrigin = new BlockPos(110, 20, 210);
        PumpjackCursor cursor = PumpjackCursor.sourceScan(
                searchOrigin,
                depositOrigin,
                MIN_BUILD_HEIGHT,
                MAX_BUILD_HEIGHT_EXCLUSIVE
        );

        assertEquals(new BlockPos(68, 10, 168), cursor.min());
        assertEquals(new BlockPos(132, 40, 232), cursor.max());
        assertEquals(PumpjackCursor.SOURCE_SCAN_MAX_POSITIONS, cursor.totalPositions());
        assertCompleteAndUnique(cursor);
    }

    @Test
    void diggingCoversEveryVerticalPositionExactlyOnceInDescendingOrder() {
        PumpjackCursor cursor = PumpjackCursor.digging(new BlockPos(2, 64, 3), 60);

        assertEquals(5, cursor.totalPositions());
        assertEquals(new BlockPos(2, 64, 3), cursor.positionAt(0));
        assertEquals(new BlockPos(2, 63, 3), cursor.positionAt(1));
        assertEquals(new BlockPos(2, 60, 3), cursor.positionAt(4));
        assertCompleteAndUnique(cursor);
    }

    @Test
    void boundedScansClampTheirVerticalOffsetsToTheWorldHeight() {
        PumpjackCursor deposit = PumpjackCursor.depositScan(
                new BlockPos(0, MIN_BUILD_HEIGHT, 0),
                MIN_BUILD_HEIGHT,
                MAX_BUILD_HEIGHT_EXCLUSIVE
        );
        PumpjackCursor source = PumpjackCursor.sourceScan(
                new BlockPos(0, 64, 0),
                new BlockPos(0, MAX_BUILD_HEIGHT_EXCLUSIVE - 1, 0),
                MIN_BUILD_HEIGHT,
                MAX_BUILD_HEIGHT_EXCLUSIVE
        );

        assertEquals(MIN_BUILD_HEIGHT, deposit.min().getY());
        assertEquals(MIN_BUILD_HEIGHT + 4, deposit.max().getY());
        assertEquals(MAX_BUILD_HEIGHT_EXCLUSIVE - 11, source.min().getY());
        assertEquals(MAX_BUILD_HEIGHT_EXCLUSIVE - 1, source.max().getY());
    }

    @Test
    void invalidBoundsAndIndicesFailBeforeProducingPositions() {
        PumpjackCursor cursor = PumpjackCursor.digging(new BlockPos(0, 10, 0), 8);

        assertThrows(IndexOutOfBoundsException.class, () -> cursor.positionAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.positionAt(cursor.totalPositions()));
        assertThrows(IllegalArgumentException.class,
                () -> PumpjackCursor.digging(new BlockPos(0, 10, 0), 11));
        assertThrows(IllegalArgumentException.class,
                () -> PumpjackCursor.searchingOil(new BlockPos(0, -65, 0), MIN_BUILD_HEIGHT));
        assertThrows(IllegalArgumentException.class,
                () -> PumpjackCursor.depositScan(BlockPos.ZERO, 0, 0));
    }

    private static void assertCompleteAndUnique(PumpjackCursor cursor) {
        Set<BlockPos> positions = new HashSet<>();
        for (int index = 0; index < cursor.totalPositions(); index++) {
            assertTrue(positions.add(cursor.positionAt(index)), "Duplicate cursor position at " + index);
        }
        assertEquals(cursor.totalPositions(), positions.size());
        assertTrue(positions.contains(cursor.min()));
        assertTrue(positions.contains(cursor.max()));
    }
}
