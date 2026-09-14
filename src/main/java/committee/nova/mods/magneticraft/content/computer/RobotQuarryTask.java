package committee.nova.mods.magneticraft.content.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** Bounded, persistable serpentine cursor used by the historical shell quarry command. */
final class RobotQuarryTask {
    static final int MAX_SIZE = MiningRobotBlockEntity.MAX_QUARRY_SIZE;

    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String SIZE_TAG = "size";
    private static final String INDEX_TAG = "index";
    private static final String MINED_TAG = "mined";
    private static final String COOLDOWN_TAG = "cooldown";

    private final int size;
    private int index;
    private boolean mined;
    private int cooldown;

    RobotQuarryTask(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Quarry size must be between 1 and " + MAX_SIZE);
        }
        this.size = size;
    }

    int size() {
        return size;
    }

    int index() {
        return index;
    }

    boolean mined() {
        return mined;
    }

    int cooldown() {
        return cooldown;
    }

    boolean tickCooldown() {
        if (cooldown <= 0) {
            return false;
        }
        cooldown--;
        return true;
    }

    void markMined(boolean waitForMiningCooldown) {
        mined = true;
        cooldown = waitForMiningCooldown ? 10 : 0;
    }

    boolean complete() {
        return mined && index == size * size - 1;
    }

    /** Relative direction: 0 front, 1 right, 3 left. */
    int nextMovement() {
        if (complete()) {
            return -1;
        }
        int row = index / size;
        int column = index % size;
        if (column < size - 1) {
            return (row & 1) == 0 ? 1 : 3;
        }
        return 0;
    }

    void advanceAfterMove() {
        if (!mined || complete()) {
            throw new IllegalStateException("Quarry cursor cannot advance");
        }
        index++;
        mined = false;
        cooldown = 0;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putInt(SIZE_TAG, size);
        tag.putInt(INDEX_TAG, index);
        tag.putBoolean(MINED_TAG, mined);
        tag.putInt(COOLDOWN_TAG, cooldown);
        return tag;
    }

    static Optional<RobotQuarryTask> restore(CompoundTag tag) {
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(SIZE_TAG, Tag.TAG_INT)
                || !tag.contains(INDEX_TAG, Tag.TAG_INT)
                || !tag.contains(MINED_TAG, Tag.TAG_BYTE)
                || !tag.contains(COOLDOWN_TAG, Tag.TAG_INT)) {
            return Optional.empty();
        }
        int size = tag.getInt(SIZE_TAG);
        int index = tag.getInt(INDEX_TAG);
        int cooldown = tag.getInt(COOLDOWN_TAG);
        if (size < 1 || size > MAX_SIZE || index < 0 || index >= size * size || cooldown < 0 || cooldown > 10) {
            return Optional.empty();
        }
        RobotQuarryTask task = new RobotQuarryTask(size);
        task.index = index;
        task.mined = tag.getBoolean(MINED_TAG);
        task.cooldown = cooldown;
        return Optional.of(task);
    }
}
