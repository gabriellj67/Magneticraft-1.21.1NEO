package committee.nova.mods.magneticraft.content.computer.runtime;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualDiskTest {
    @Test
    void boundedFilesystemRoundTripsThroughOwnedSchema() {
        VirtualDisk disk = new VirtualDisk();
        disk.format();
        assertTrue(disk.setLabel("User Disk"));
        assertTrue(disk.mkdir("/", "programs"));
        assertTrue(disk.write("/", "programs/start.fth", "2 5 + ."));

        VirtualDisk restored = new VirtualDisk();
        assertTrue(restored.restore(disk.save()));

        assertEquals("User Disk", restored.label());
        assertEquals("2 5 + .", restored.read("/", "programs/start.fth").orElseThrow());
        assertEquals(disk.usedBytes(), restored.usedBytes());
        assertEquals(disk.entryCount(), restored.entryCount());
    }

    @Test
    void pathsCannotEscapeRootOrAddressTheHostFilesystem() {
        VirtualDisk disk = new VirtualDisk();
        disk.format();

        assertFalse(disk.write("/", "../outside", "no"));
        assertFalse(disk.write("/", "C:\\Windows\\secret", "no"));
        assertFalse(disk.write("/", "invalid:name", "no"));
        assertTrue(disk.write("/", "inside", "yes"));
        assertEquals(1, disk.entryCount());
    }

    @Test
    void capacityAndEntryLimitsAreHardBounds() {
        VirtualDisk disk = new VirtualDisk();
        disk.format();

        assertFalse(disk.write("/", "too_large", "x".repeat(VirtualDisk.MAX_FILE_BYTES + 1)));
        for (int index = 0; index < VirtualDisk.MAX_ENTRIES; index++) {
            assertTrue(disk.touch("/", "f" + index));
        }
        assertFalse(disk.touch("/", "overflow"));
        assertEquals(VirtualDisk.MAX_ENTRIES, disk.entryCount());
    }

    @Test
    void malformedAndFutureSnapshotsDoNotPartiallyMutateDisk() {
        VirtualDisk disk = new VirtualDisk();
        disk.format();
        assertTrue(disk.write("/", "kept", "value"));

        CompoundTag future = disk.save();
        future.putInt("schema_version", Integer.MAX_VALUE);
        assertFalse(disk.restore(future));
        assertEquals("value", disk.read("/", "kept").orElseThrow());

        CompoundTag malformed = disk.save();
        malformed.remove("files");
        assertFalse(disk.restore(malformed));
        assertEquals("value", disk.read("/", "kept").orElseThrow());
    }
}
