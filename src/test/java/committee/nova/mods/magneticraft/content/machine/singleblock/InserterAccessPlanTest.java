package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InserterAccessPlanTest {
    @Test
    void inventoryAccessOrderPreservesLegacyHighThenLowSideSemantics() {
        BlockPos origin = new BlockPos(8, 12, 16);
        BlockPos sameLevel = origin.east();

        List<SingleBlockMachineSupport.InserterAccess> accesses =
                SingleBlockMachineSupport.inserterInventoryAccesses(origin, Direction.EAST);

        assertEquals(3, accesses.size());
        assertEquals(sameLevel, accesses.get(0).position());
        assertEquals(Direction.UP, accesses.get(0).side());
        assertTrue(accesses.get(0).includeMinecarts());
        assertEquals(sameLevel.below(), accesses.get(1).position());
        assertEquals(Direction.UP, accesses.get(1).side());
        assertTrue(accesses.get(1).includeMinecarts());
        assertEquals(sameLevel, accesses.get(2).position());
        assertEquals(Direction.WEST, accesses.get(2).side());
        assertFalse(accesses.get(2).includeMinecarts(), "Same-level minecarts must not be queried twice");
    }

    @Test
    void entityPickupAndDropUseTheirDistinctLegacyHeightOrder() {
        BlockPos origin = new BlockPos(8, 12, 16);
        BlockPos sameLevel = origin.west();

        assertEquals(
                List.of(sameLevel.below(), sameLevel),
                SingleBlockMachineSupport.inserterGroundPickupPositions(origin, Direction.WEST)
        );
        assertEquals(
                List.of(sameLevel.below(), sameLevel),
                SingleBlockMachineSupport.inserterGroundDropPositions(origin, Direction.WEST)
        );
    }
}
