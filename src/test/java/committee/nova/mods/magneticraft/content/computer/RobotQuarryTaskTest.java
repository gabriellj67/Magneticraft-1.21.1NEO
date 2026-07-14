package committee.nova.mods.magneticraft.content.computer;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotQuarryTaskTest {
    @Test
    void cursorWalksACompleteSerpentineSquareExactlyOnce() {
        RobotQuarryTask task = new RobotQuarryTask(3);
        List<Integer> movements = new ArrayList<>();

        while (task.index() < 8) {
            task.markMined(false);
            movements.add(task.nextMovement());
            task.advanceAfterMove();
        }
        task.markMined(false);

        assertEquals(List.of(1, 1, 0, 3, 3, 0, 1, 1), movements);
        assertTrue(task.complete());
        assertEquals(8, task.index());
    }

    @Test
    void taskSnapshotRejectsCorruptBounds() {
        RobotQuarryTask task = new RobotQuarryTask(10);
        task.markMined(true);
        assertTrue(task.tickCooldown());

        RobotQuarryTask restored = RobotQuarryTask.restore(task.save()).orElseThrow();
        assertEquals(10, restored.size());
        assertTrue(restored.mined());
        assertEquals(9, restored.cooldown());

        CompoundTag corrupt = task.save();
        corrupt.putInt("index", 100);
        assertFalse(RobotQuarryTask.restore(corrupt).isPresent());
        corrupt = task.save();
        corrupt.putInt("schema_version", Integer.MAX_VALUE);
        assertFalse(RobotQuarryTask.restore(corrupt).isPresent());
    }

    @Test
    void sizeLimitPreventsUnboundedWork() {
        assertThrows(IllegalArgumentException.class, () -> new RobotQuarryTask(0));
        assertThrows(IllegalArgumentException.class, () -> new RobotQuarryTask(RobotQuarryTask.MAX_SIZE + 1));
    }
}
