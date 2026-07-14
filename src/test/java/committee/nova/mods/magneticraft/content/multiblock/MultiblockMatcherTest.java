package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiblockMatcherTest {
    @Test
    void unloadedMemberStopsBeforeReadingItsBlockState() {
        BlockPos controller = new BlockPos(15, 64, 15);
        MultiblockDefinition definition = MultiblockDefinition.SOLAR_PANEL;
        MultiblockCell firstCell = definition.requiredCells().get(0);
        BlockPos expectedPosition = MultiblockTransform.worldPosition(
                controller,
                firstCell.offset(),
                definition.center(),
                Direction.NORTH,
                false
        );
        AtomicInteger chunkChecks = new AtomicInteger();
        AtomicInteger blockReads = new AtomicInteger();

        MultiblockValidationResult result = MultiblockMatcher.validate(
                controller,
                Direction.NORTH,
                false,
                definition,
                position -> {
                    chunkChecks.incrementAndGet();
                    return false;
                },
                (cell, position) -> {
                    blockReads.incrementAndGet();
                    return false;
                }
        );

        assertEquals(MultiblockValidationResult.Status.UNLOADED, result.status());
        assertEquals(expectedPosition, result.position());
        assertEquals(firstCell.rule(), result.expected());
        assertEquals(1, chunkChecks.get());
        assertEquals(0, blockReads.get());
    }
}
