package committee.nova.mods.magneticraft.content.machine.framework.menu;

import net.minecraft.world.inventory.SimpleContainerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Int32ContainerDataTest {
    @Test
    void signedAndUnsignedBoundariesRoundTripThroughTwoWords() {
        int[] values = {0, 32_767, 32_768, 65_535, 65_536, 1_000_000, Integer.MAX_VALUE, Integer.MIN_VALUE, -1};
        for (int value : values) {
            assertEquals(
                    value,
                    Int32ContainerData.combineWords(
                            Int32ContainerData.lowWord(value),
                            Int32ContainerData.highWord(value)
                    )
            );
        }
    }

    @Test
    void logicalReadTreatsPacketWordsAsUnsignedShorts() {
        SimpleContainerData packetData = new SimpleContainerData(4);
        packetData.set(0, (short) Int32ContainerData.lowWord(1_000_000));
        packetData.set(1, (short) Int32ContainerData.highWord(1_000_000));
        packetData.set(2, (short) 0xFFFF);
        packetData.set(3, (short) 0xFFFF);

        assertEquals(1_000_000, Int32ContainerData.read(packetData, 0));
        assertEquals(-1, Int32ContainerData.read(packetData, 1));
    }

    @Test
    void readOnlyProjectionExposesTwoPhysicalEntriesPerLogicalValue() {
        Int32ContainerData data = Int32ContainerData.readOnly(() -> 65_536, () -> 1_000_000);

        assertEquals(4, data.getCount());
        assertEquals(0, data.get(0));
        assertEquals(1, data.get(1));
        assertEquals(1_000_000, Int32ContainerData.read(data, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> data.get(4));
        assertThrows(IndexOutOfBoundsException.class, () -> Int32ContainerData.read(data, 2));
    }
}
