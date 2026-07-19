package committee.nova.mods.magneticraft.content.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NuclearMultiblockBoundsTest {
    private static final BlockPos CONTROLLER = new BlockPos(20, 70, -30);

    @Test
    void renderBoundsFollowTheScaledSceneForEveryHorizontalFacing() {
        assertEquals(
                new AABB(16.5D, 68.5D, -30.5D, 24.5D, 76.5D, -20.5D),
                NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.NORTH, 7, 7, 9)
        );
        assertEquals(
                new AABB(16.5D, 68.5D, -38.5D, 24.5D, 76.5D, -28.5D),
                NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.SOUTH, 7, 7, 9)
        );
        assertEquals(
                new AABB(19.5D, 68.5D, -33.5D, 29.5D, 76.5D, -25.5D),
                NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.WEST, 7, 7, 9)
        );
        assertEquals(
                new AABB(11.5D, 68.5D, -33.5D, 21.5D, 76.5D, -25.5D),
                NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.EAST, 7, 7, 9)
        );
    }

    @Test
    void invalidDimensionsFallBackToTheControllerBlock() {
        assertEquals(
                new AABB(CONTROLLER),
                NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.NORTH, 0, 7, 9)
        );
    }

    @Test
    void maximumSupportedScenesRetainTheirModelOverhang() {
        AABB reactor = NuclearMultiblockBounds.renderBounds(
                CONTROLLER, Direction.NORTH, 13, 15, 13
        );
        assertEquals(14.0D, reactor.getXsize(), 0.0D);
        assertEquals(16.0D, reactor.getYsize(), 0.0D);
        assertEquals(14.0D, reactor.getZsize(), 0.0D);

        AABB pool = NuclearMultiblockBounds.renderBounds(
                CONTROLLER, Direction.EAST, 11, 8, 11
        );
        assertEquals(12.0D, pool.getXsize(), 0.0D);
        assertEquals(9.0D, pool.getYsize(), 0.0D);
        assertEquals(12.0D, pool.getZsize(), 0.0D);
    }

    @Test
    void verticalFacingIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NuclearMultiblockBounds.renderBounds(CONTROLLER, Direction.UP, 7, 7, 9)
        );
    }
}
