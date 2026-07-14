package committee.nova.mods.magneticraft.content.computer;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RobotOrientationTest {
    @Test
    void horizontalFacingAndPitchProduceExactlyTwelveStableStates() {
        Set<Integer> flags = new HashSet<>();
        for (int pitch = -1; pitch <= 1; pitch++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                flags.add(MiningRobotBlockEntity.orientationFlag(direction, pitch));
            }
        }
        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), flags);
    }

    @Test
    void invalidPitchAndVerticalFacingAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MiningRobotBlockEntity.orientationFlag(Direction.UP, 0));
        assertThrows(IllegalArgumentException.class,
                () -> MiningRobotBlockEntity.orientationFlag(Direction.NORTH, 2));
    }
}
