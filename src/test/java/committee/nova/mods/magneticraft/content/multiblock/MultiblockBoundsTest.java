package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiblockBoundsTest {
    @Test
    void renderBoundsMatchDefinitionDimensionsForEveryFacing() {
        BlockPos controller = new BlockPos(20, 70, -30);
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                AABB bounds = MultiblockBounds.worldBounds(controller, facing, false, definition);
                assertEquals(definition.size().y(), bounds.getYsize(), 0.0D, definition::id);
                if (facing.getAxis() == Direction.Axis.Z) {
                    assertEquals(definition.size().x(), bounds.getXsize(), 0.0D, definition::id);
                    assertEquals(definition.size().z(), bounds.getZsize(), 0.0D, definition::id);
                } else {
                    assertEquals(definition.size().z(), bounds.getXsize(), 0.0D, definition::id);
                    assertEquals(definition.size().x(), bounds.getZsize(), 0.0D, definition::id);
                }
            }
        }
    }
}
