package committee.nova.mods.magneticraft.client;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvancedMultiblockRendererTest {
    @Test
    void adaptsCurrentControllerFacingToLegacyModelFacing() {
        assertEquals(Direction.SOUTH, AdvancedMultiblockRenderer.legacyModelFacing(Direction.NORTH));
        assertEquals(Direction.WEST, AdvancedMultiblockRenderer.legacyModelFacing(Direction.EAST));
        assertEquals(Direction.NORTH, AdvancedMultiblockRenderer.legacyModelFacing(Direction.SOUTH));
        assertEquals(Direction.EAST, AdvancedMultiblockRenderer.legacyModelFacing(Direction.WEST));
    }
}
