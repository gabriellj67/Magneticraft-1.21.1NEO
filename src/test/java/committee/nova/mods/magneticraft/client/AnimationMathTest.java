package committee.nova.mods.magneticraft.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationMathTest {
    @Test
    void interpolatesFromTheSnapshotAndClampsToTheSyncInterval() {
        assertEquals(0.25F, AnimationMath.boundedSnapshotAge(100L, 100L, 0.25F, 4.0F, true));
        assertEquals(2.5F, AnimationMath.boundedSnapshotAge(102L, 100L, 0.5F, 4.0F, true));
        assertEquals(4.0F, AnimationMath.boundedSnapshotAge(110L, 100L, 0.5F, 4.0F, true));
    }

    @Test
    void freezesWhenMovementIsDisabledOrNoSnapshotExists() {
        assertEquals(0.0F, AnimationMath.boundedSnapshotAge(102L, 100L, 0.5F, 4.0F, false));
        assertEquals(0.0F, AnimationMath.boundedSnapshotAge(102L, Long.MIN_VALUE, 0.5F, 4.0F, true));
    }

    @Test
    void clampsNegativeAgeAndPartialTicks() {
        assertEquals(0.0F, AnimationMath.boundedSnapshotAge(99L, 100L, -1.0F, 4.0F, true));
        assertEquals(1.0F, AnimationMath.boundedSnapshotAge(100L, 100L, 2.0F, 4.0F, true));
    }
}
