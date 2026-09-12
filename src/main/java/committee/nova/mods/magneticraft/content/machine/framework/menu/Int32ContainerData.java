package committee.nova.mods.magneticraft.content.machine.framework.menu;

import net.minecraft.world.inventory.ContainerData;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Encodes logical 32-bit menu values as pairs of unsigned 16-bit words.
 * Vanilla's container-data packet writes each physical entry as a short.
 */
public final class Int32ContainerData implements ContainerData {
    private final IntSupplier[] values;

    private Int32ContainerData(IntSupplier[] values) {
        this.values = Arrays.copyOf(values, values.length);
        for (IntSupplier value : this.values) {
            Objects.requireNonNull(value, "value");
        }
    }

    public static Int32ContainerData readOnly(IntSupplier... values) {
        Objects.requireNonNull(values, "values");
        return new Int32ContainerData(values);
    }

    @Override
    public int get(int index) {
        checkPhysicalIndex(index, getCount());
        int value = values[index / 2].getAsInt();
        return (index & 1) == 0 ? lowWord(value) : highWord(value);
    }

    @Override
    public void set(int index, int value) {
        checkPhysicalIndex(index, getCount());
    }

    @Override
    public int getCount() {
        return values.length * 2;
    }

    public static int read(ContainerData data, int logicalIndex) {
        Objects.requireNonNull(data, "data");
        if (logicalIndex < 0) {
            throw new IndexOutOfBoundsException("Logical container-data index " + logicalIndex);
        }
        int physicalIndex = Math.multiplyExact(logicalIndex, 2);
        if (physicalIndex + 1 >= data.getCount()) {
            throw new IndexOutOfBoundsException("Logical container-data index " + logicalIndex);
        }
        return combineWords(data.get(physicalIndex), data.get(physicalIndex + 1));
    }

    public static int lowWord(int value) {
        return value & 0xFFFF;
    }

    public static int highWord(int value) {
        return value >>> 16 & 0xFFFF;
    }

    public static int combineWords(int lowWord, int highWord) {
        return lowWord & 0xFFFF | (highWord & 0xFFFF) << 16;
    }

    private static void checkPhysicalIndex(int index, int count) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Physical container-data index " + index);
        }
    }
}
