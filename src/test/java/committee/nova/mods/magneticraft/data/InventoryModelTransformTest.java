package committee.nova.mods.magneticraft.data;

import net.minecraft.world.item.ItemDisplayContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryModelTransformTest {
    @Test
    void unitBlockKeepsVanillaGuiScale() {
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(0, 0, 0, 1, 1, 1);
        InventoryModelTransform.DisplayTransform gui = transform.forContext(ItemDisplayContext.GUI);
        assertEquals(0.625F, gui.scale(), 0.000001F);
        assertEquals(0.0F, gui.translationX(), 0.000001F);
        assertEquals(0.0F, gui.translationY(), 0.000001F);
        assertEquals(0.0F, gui.translationZ(), 0.000001F);
    }

    @Test
    void oversizedSceneIsScaledAndCenteredInEveryDisplayContext() {
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(-4, -2, 0, 6, 8, 10);
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            if (context == ItemDisplayContext.NONE) {
                continue;
            }
            InventoryModelTransform.DisplayTransform display = transform.forContext(context);
            assertTrue(display.scale() > 0.0F && display.scale() <= 0.1F, context::name);
            assertTrue(Math.abs(display.translationX()) <= 80.0F, context::name);
            assertTrue(Math.abs(display.translationY()) <= 80.0F, context::name);
            assertTrue(Math.abs(display.translationZ()) <= 80.0F, context::name);
        }
    }

    @Test
    void displayContextsKeepModelLoaderBlockDefaultsForAUnitBlock() {
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(0, 0, 0, 1, 1, 1);

        assertDisplay(transform, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                75.0F, 225.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F);
        assertDisplay(transform, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                75.0F, 45.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F);
        assertDisplay(transform, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                0.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
        assertDisplay(transform, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
        assertDisplay(transform, ItemDisplayContext.HEAD,
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        assertDisplay(transform, ItemDisplayContext.GUI,
                30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
        assertDisplay(transform, ItemDisplayContext.GROUND,
                0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.25F);
        assertDisplay(transform, ItemDisplayContext.FIXED,
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
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
        InventoryModelTransform transform = InventoryModelTransform.fromBounds(0, 0, 0, 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> transform.forContext(ItemDisplayContext.NONE));
        assertThrows(
                IllegalArgumentException.class,
                () -> InventoryModelTransform.fromBounds(0, 0, 0, Double.MAX_VALUE, 1, 1)
        );
    }

    private static void assertDisplay(
            InventoryModelTransform transform,
            ItemDisplayContext context,
            float rotationX,
            float rotationY,
            float rotationZ,
            float translationX,
            float translationY,
            float translationZ,
            float scale
    ) {
        InventoryModelTransform.DisplayTransform display = transform.forContext(context);
        assertEquals(rotationX, display.rotationX(), 0.000001F, context + " rotation X");
        assertEquals(rotationY, display.rotationY(), 0.000001F, context + " rotation Y");
        assertEquals(rotationZ, display.rotationZ(), 0.000001F, context + " rotation Z");
        assertEquals(translationX, display.translationX(), 0.000001F, context + " translation X");
        assertEquals(translationY, display.translationY(), 0.000001F, context + " translation Y");
        assertEquals(translationZ, display.translationZ(), 0.000001F, context + " translation Z");
        assertEquals(scale, display.scale(), 0.000001F, context + " scale");
    }
}
