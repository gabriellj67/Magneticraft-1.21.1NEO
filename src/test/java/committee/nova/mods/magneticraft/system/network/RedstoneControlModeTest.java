package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedstoneControlModeTest {
    @Test
    void everyModeHasExplicitSignalSemantics() {
        assertTrue(RedstoneControlMode.IGNORED.allows(false));
        assertTrue(RedstoneControlMode.IGNORED.allows(true));
        assertFalse(RedstoneControlMode.REQUIRES_SIGNAL.allows(false));
        assertTrue(RedstoneControlMode.REQUIRES_SIGNAL.allows(true));
        assertTrue(RedstoneControlMode.REQUIRES_NO_SIGNAL.allows(false));
        assertFalse(RedstoneControlMode.REQUIRES_NO_SIGNAL.allows(true));
    }
}
