package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirBubbleOwnershipSavedDataTest {
    @Test
    void roundTripIsVersionedDeterministicAndRejectsUnknownSchemas() {
        AirBubbleOwnershipSavedData data = new AirBubbleOwnershipSavedData();
        BlockPos firstBubble = new BlockPos(1, 64, 2);
        BlockPos firstAirlock = new BlockPos(0, 64, 0);
        BlockPos secondBubble = new BlockPos(33, -12, -4);
        BlockPos secondAirlock = new BlockPos(32, -12, 0);
        data.bind(secondBubble, secondAirlock);
        data.bind(firstBubble, firstAirlock);

        CompoundTag firstSave = data.save(new CompoundTag());
        assertEquals(AirBubbleOwnershipSavedData.SCHEMA_VERSION, firstSave.getInt("schema_version"));

        AirBubbleOwnershipSavedData restored = AirBubbleOwnershipSavedData.load(firstSave);
        assertEquals(firstAirlock, restored.ownerOf(firstBubble).orElseThrow());
        assertEquals(secondAirlock, restored.ownerOf(secondBubble).orElseThrow());
        assertEquals(firstSave, restored.save(new CompoundTag()));

        CompoundTag versionless = firstSave.copy();
        versionless.remove("schema_version");
        assertEquals(0, AirBubbleOwnershipSavedData.load(versionless).size());
        CompoundTag future = firstSave.copy();
        future.putInt("schema_version", AirBubbleOwnershipSavedData.SCHEMA_VERSION + 1);
        assertEquals(0, AirBubbleOwnershipSavedData.load(future).size());
    }

    @Test
    void unbindingAnAirlockReleasesOnlyItsBubbles() {
        AirBubbleOwnershipSavedData data = new AirBubbleOwnershipSavedData();
        BlockPos airlock = new BlockPos(16, 70, 16);
        BlockPos otherAirlock = new BlockPos(48, 70, 16);
        BlockPos firstBubble = new BlockPos(17, 70, 16);
        BlockPos secondBubble = new BlockPos(18, 70, 16);
        BlockPos otherBubble = new BlockPos(47, 70, 16);
        data.bind(firstBubble, airlock);
        data.bind(secondBubble, airlock);
        data.bind(otherBubble, otherAirlock);

        assertEquals(List.of(firstBubble, secondBubble), data.unbindAirlock(airlock));
        assertFalse(data.ownerOf(firstBubble).isPresent());
        assertFalse(data.ownerOf(secondBubble).isPresent());
        assertEquals(otherAirlock, data.ownerOf(otherBubble).orElseThrow());
        assertEquals(1, data.size());
        assertTrue(data.unbindBubble(otherBubble));
        assertFalse(data.unbindBubble(otherBubble));
        assertEquals(0, data.size());
    }
}
