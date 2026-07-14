package committee.nova.mods.magneticraft.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjInventoryTransformTest {
    @Test
    void centeredUnitCubeKeepsVanillaGuiScale() throws IOException {
        ObjInventoryTransform transform = ObjInventoryTransform.parse(new StringReader("""
                v 0 0 0
                v 1 1 1
                """));

        assertEquals(0.625F, transform.scale(), 0.000001F);
        assertEquals(0.0F, transform.translationX(), 0.000001F);
        assertEquals(0.0F, transform.translationY(), 0.000001F);
        assertEquals(0.0F, transform.translationZ(), 0.000001F);
    }

    @Test
    void oversizedOffCenterModelIsScaledAndRecentered() throws IOException {
        ObjInventoryTransform transform = ObjInventoryTransform.parse(new StringReader("""
                # Five blocks tall and centered below the normal block origin
                v 0 -4 0
                v 1 1 1
                """));

        assertEquals(0.125F, transform.scale(), 0.000001F);
        assertTrue(Math.abs(transform.translationX())
                + Math.abs(transform.translationY())
                + Math.abs(transform.translationZ()) > 0.0F);
    }

    @Test
    void objWithoutVerticesIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ObjInventoryTransform.parse(new StringReader("o empty\n"))
        );
    }

    @Test
    void transformBeyondMinecraftTranslationLimitIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ObjInventoryTransform.parse(new StringReader("""
                        v 100 0 0
                        v 100.1 0.1 0.1
                        """))
        );
    }
}
