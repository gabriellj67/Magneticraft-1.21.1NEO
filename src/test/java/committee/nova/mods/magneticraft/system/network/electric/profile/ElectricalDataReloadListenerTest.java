package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElectricalDataReloadListenerTest {
    @Test
    void fullDataPathMapsToStableLogicalId() {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("example", "custom/ultra_voltage"),
                ElectricalDataReloadListener.logicalId(
                        ElectricalDataReloadListener.VOLTAGE_DIRECTORY,
                        ResourceLocation.fromNamespaceAndPath(
                                "example",
                                "magneticraft/voltage_tiers/custom/ultra_voltage.json"
                        )
                )
        );
    }

    @Test
    void resourcesOutsideTheFixedDirectoryAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ElectricalDataReloadListener.logicalId(
                        ElectricalDataReloadListener.VOLTAGE_DIRECTORY,
                        ResourceLocation.fromNamespaceAndPath("example", "voltage_tiers/value.json")
                )
        );
    }
}
