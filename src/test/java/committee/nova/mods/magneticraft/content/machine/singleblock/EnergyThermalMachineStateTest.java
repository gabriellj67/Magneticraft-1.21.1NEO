package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyThermalMachineStateTest {
    @Test
    void workingTimestampRoundTripsAndKeepsAStableTwentyTickWindow() {
        SingleBlockMachineState state = new SingleBlockMachineState();
        state.recordWorking(100L);

        CompoundTag saved = new CompoundTag();
        state.save(saved);
        SingleBlockMachineState restored = new SingleBlockMachineState();
        restored.load(saved);

        assertEquals(100L, restored.lastWorkingTick);
        assertTrue(restored.wasWorkingRecently(119L, 20));
        assertFalse(restored.wasWorkingRecently(120L, 20));
        assertFalse(restored.wasWorkingRecently(99L, 20));
    }

    @Test
    void neverWorkedStateDoesNotAcquireGraceAfterLoad() {
        SingleBlockMachineState restored = new SingleBlockMachineState();
        restored.load(new CompoundTag());

        assertEquals(-1L, restored.lastWorkingTick);
        assertFalse(restored.wasWorkingRecently(0L, 20));
    }
}
