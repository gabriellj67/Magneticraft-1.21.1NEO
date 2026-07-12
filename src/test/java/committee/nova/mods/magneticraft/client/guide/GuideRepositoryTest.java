package committee.nova.mods.magneticraft.client.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideRepositoryTest {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(
            "magneticraft",
            "guide/multiblocks/test.json"
    );

    @Test
    void parsesMirroringAndPortSummary() {
        JsonObject root = baseGuide();
        root.addProperty("supports_mirroring", true);
        root.add("ports", JsonParser.parseString("""
                {
                  "inventory_slots": 3,
                  "bulk_item_capacity": 8192,
                  "electricity": true,
                  "heat": false,
                  "fluid_tank_capacities_mb": [1000, 16000]
                }
                """).getAsJsonObject());

        GuideRepository.MultiblockGuide guide = GuideRepository.parseMultiblock(FILE, root);

        assertTrue(guide.supportsMirroring());
        assertEquals(3, guide.ports().inventorySlots());
        assertEquals(8192, guide.ports().bulkItemCapacity());
        assertTrue(guide.ports().electricity());
        assertFalse(guide.ports().heat());
        assertEquals(List.of(1000, 16000), guide.ports().fluidTankCapacitiesMb());
    }

    @Test
    void defaultsOptionalReleaseMetadataForOlderPacks() {
        GuideRepository.MultiblockGuide guide = GuideRepository.parseMultiblock(FILE, baseGuide());

        assertFalse(guide.supportsMirroring());
        assertEquals(0, guide.ports().inventorySlots());
        assertEquals(0, guide.ports().bulkItemCapacity());
        assertFalse(guide.ports().electricity());
        assertFalse(guide.ports().heat());
        assertTrue(guide.ports().fluidTankCapacitiesMb().isEmpty());
    }

    private static JsonObject baseGuide() {
        return JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "id": "magneticraft:test",
                  "translation_key": "block.magneticraft.test",
                  "category": "processing",
                  "layers": [["C"]],
                  "legend": [{"symbol": "C", "rule": "controller"}]
                }
                """).getAsJsonObject();
    }
}
