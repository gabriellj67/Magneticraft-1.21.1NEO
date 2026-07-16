package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeothermalPumpStateTest {
    @Test
    void ownerDrillSourcesAndRemainingEnergyRoundTrip() {
        GeothermalPumpState original = new GeothermalPumpState();
        UUID owner = UUID.randomUUID();
        BlockPos controller = new BlockPos(4, 60, -8);
        original.setOwner(owner);
        assertEquals(controller.below(), original.drillPosition(controller));
        original.advanceDrill();
        original.sources().add(new BlockPos(4, 11, -8));
        original.sources().add(new BlockPos(5, 11, -8));
        original.setRemainingEnergyJoules(123_456.75D);

        CompoundTag tag = new CompoundTag();
        original.save(tag);
        GeothermalPumpState restored = new GeothermalPumpState();
        restored.load(tag);

        assertEquals(owner, restored.owner());
        assertEquals(controller.below(2), restored.drillPosition(controller));
        assertEquals(original.sources(), restored.sources());
        assertEquals(123_456.75D, restored.remainingEnergyJoules());
        assertFalse(restored.searchComplete());
    }

    @Test
    void invalidRemainingEnergyIsClamped() {
        GeothermalPumpState state = new GeothermalPumpState();
        state.setRemainingEnergyJoules(-1.0D);
        assertEquals(0.0D, state.remainingEnergyJoules());
        state.setRemainingEnergyJoules(Double.NaN);
        assertEquals(0.0D, state.remainingEnergyJoules());
    }
}
