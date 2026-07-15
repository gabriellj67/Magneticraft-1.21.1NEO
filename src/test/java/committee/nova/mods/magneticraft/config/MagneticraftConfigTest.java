package committee.nova.mods.magneticraft.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagneticraftConfigTest {
    @Test
    void electricalDamageAndReloadGraceUseThePublishedDefaults() {
        assertTrue(MagneticraftConfig.ENABLE_ELECTRICAL_DAMAGE.getDefault());
        assertEquals(200, MagneticraftConfig.ELECTRICAL_RELOAD_GRACE_TICKS.getDefault());
        assertEquals(
                java.util.List.of("electricity", "enable_electrical_damage"),
                MagneticraftConfig.ENABLE_ELECTRICAL_DAMAGE.getPath()
        );
        assertEquals(
                java.util.List.of("electricity", "electrical_reload_grace_ticks"),
                MagneticraftConfig.ELECTRICAL_RELOAD_GRACE_TICKS.getPath()
        );
    }
}
