package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearReactorPresetTest {
    @Test
    void starterPresetsCoverThreeSizesAndEveryRequiredCoreRole() {
        assertEquals(java.util.List.of(7, 9, 11), Arrays.stream(NuclearReactorPreset.values())
                .map(NuclearReactorPreset::width).toList());
        for (NuclearReactorPreset preset : NuclearReactorPreset.values()) {
            assertEquals(preset.width(), preset.length(), preset.id());
            assertEquals(7, preset.height(), preset.id());
            assertEquals((preset.width() - 4) * (preset.length() - 4), preset.columns().size(), preset.id());
            assertTrue(preset.columns().values().stream().anyMatch(NuclearReactorColumnType::isFuel), preset.id());
            assertTrue(preset.columns().values().stream().anyMatch(NuclearReactorColumnType::isControlRod), preset.id());
            assertTrue(preset.columns().containsValue(NuclearReactorColumnType.COOLANT_CHANNEL), preset.id());
            assertTrue(preset.columns().containsValue(NuclearReactorColumnType.INSTRUMENTATION), preset.id());
        }
    }
}
