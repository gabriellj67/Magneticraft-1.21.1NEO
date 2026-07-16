package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleBlockMachineStateTest {
    @Test
    void durableStateRoundTripsWithStableInserterFlags() {
        SingleBlockMachineState original = new SingleBlockMachineState();
        original.progress = 37;
        original.totalProgress = 80;
        original.cooldown = 5;
        original.fluidOutputCursor = 4;
        original.tankExportEnabled = true;
        original.activeRecipe = "magneticraft:test";
        original.thermopileFlux = 12.5D;
        original.fuelEnergyJoules = 345.5D;
        assertTrue(original.toggleInserterFlag(0));
        assertTrue(original.toggleInserterFlag(4));

        CompoundTag tag = new CompoundTag();
        original.save(tag);
        SingleBlockMachineState restored = new SingleBlockMachineState();
        restored.load(tag);

        assertEquals(37, restored.progress);
        assertEquals(80, restored.totalProgress);
        assertEquals(5, restored.cooldown);
        assertEquals(4, restored.fluidOutputCursor);
        assertTrue(restored.tankExportEnabled);
        assertEquals("magneticraft:test", restored.activeRecipe);
        assertEquals(12.5D, restored.thermopileFlux);
        assertEquals(345.5D, restored.fuelEnergyJoules);
        assertEquals(original.inserterFlags(), restored.inserterFlags());
    }

    @Test
    void malformedNumericStateIsClampedWithoutChangingInvalidFlags() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("progress", -1);
        tag.putInt("total_progress", -2);
        tag.putInt("fluid_output_cursor", -1);
        tag.putDouble("thermopile_flux", Double.NaN);
        tag.putDouble("fuel_energy_joules", Double.POSITIVE_INFINITY);

        SingleBlockMachineState state = new SingleBlockMachineState();
        state.load(tag);

        assertEquals(0, state.progress);
        assertEquals(0, state.totalProgress);
        assertEquals(5, state.fluidOutputCursor);
        assertEquals(0.0D, state.thermopileFlux);
        assertEquals(0.0D, state.fuelEnergyJoules);
        assertEquals((1 << 2) | (1 << 3), state.inserterFlags());
        int flags = state.inserterFlags();
        assertFalse(state.toggleInserterFlag(99));
        assertEquals(flags, state.inserterFlags());
    }

    @Test
    void emptyPayloadRestoresDefaultsAfterPriorState() {
        SingleBlockMachineState state = new SingleBlockMachineState();
        state.progress = 12;
        state.working = true;
        state.tankExportEnabled = true;
        state.activeRecipe = "magneticraft:test";
        assertTrue(state.toggleInserterFlag(0));
        assertTrue(state.toggleInserterFlag(2));
        assertTrue(state.toggleInserterFlag(3));
        assertTrue(state.toggleInserterFlag(5));

        state.load(new CompoundTag());

        assertEquals(0, state.progress);
        assertFalse(state.working);
        assertFalse(state.tankExportEnabled);
        assertEquals("", state.activeRecipe);
        assertEquals((1 << 2) | (1 << 3), state.inserterFlags());
    }
}
