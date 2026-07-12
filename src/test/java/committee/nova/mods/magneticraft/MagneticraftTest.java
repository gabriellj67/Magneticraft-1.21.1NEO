package committee.nova.mods.magneticraft;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagneticraftTest {
    @Test
    void createsNamespacedResourceLocations() {
        ResourceLocation id = Magneticraft.id("engineering_baseline");

        assertEquals(Magneticraft.MOD_ID, id.getNamespace());
        assertEquals("engineering_baseline", id.getPath());
    }
}
