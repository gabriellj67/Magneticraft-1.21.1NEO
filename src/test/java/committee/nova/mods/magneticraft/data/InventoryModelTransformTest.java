package committee.nova.mods.magneticraft.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryModelTransformTest {
    @Test
    void unitBlockKeepsVanillaGuiScale() {
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(0, 0, 0, 1, 1, 1);
        assertEquals(0.625F, transform.scale(), 0.000001F);
        assertEquals(0.0F, transform.translationX(), 0.000001F);
        assertEquals(0.0F, transform.translationY(), 0.000001F);
        assertEquals(0.0F, transform.translationZ(), 0.000001F);
    }

    @Test
    void oversizedSceneIsScaledAndCentered() {
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(-4, -2, 0, 6, 8, 10);
        assertTrue(transform.scale() > 0.0F && transform.scale() < 0.1F);
        assertTrue(Math.abs(transform.translationX()) <= 80.0F);
        assertTrue(Math.abs(transform.translationY()) <= 80.0F);
        assertTrue(Math.abs(transform.translationZ()) <= 80.0F);
    }

    @Test
    void degenerateOrNonFiniteBoundsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InventoryModelTransform.fromBounds(0, 0, 0, 0, 0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> InventoryModelTransform.fromBounds(0, 0, 0, Double.POSITIVE_INFINITY, 1, 1)
        );
    }
}
