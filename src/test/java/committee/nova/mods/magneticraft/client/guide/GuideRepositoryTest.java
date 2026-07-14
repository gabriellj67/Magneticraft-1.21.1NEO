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
    private static final ResourceLocation ITEM_FILE = ResourceLocation.fromNamespaceAndPath(
            "magneticraft",
            "guide/items/portable_electric.json"
    );
    private static final ResourceLocation MACHINE_FILE = ResourceLocation.fromNamespaceAndPath(
            "magneticraft",
            "guide/machines/electric_furnace.json"
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

    @Test
    void parsesPortableItemContractsAndClampsMalformedNegativeCosts() {
        JsonObject root = JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "items": [
                    {
                      "id": "magneticraft:electric_drill",
                      "translation_key": "item.magneticraft.electric_drill",
                      "description": "guide.magneticraft.item.electric_drill.description",
                      "capacity_fe": 512000,
                      "break_cost_fe": 1000,
                      "attack_cost_fe": 2000,
                      "use_cost_fe": -1
                    }
                  ]
                }
                """).getAsJsonObject();

        List<GuideRepository.ItemGuide> items = GuideRepository.parseItems(ITEM_FILE, root);

        assertEquals(1, items.size());
        GuideRepository.ItemGuide drill = items.get(0);
        assertEquals("magneticraft:electric_drill", drill.id().toString());
        assertEquals(512_000, drill.capacityFe());
        assertEquals(1_000, drill.breakCostFe());
        assertEquals(2_000, drill.attackCostFe());
        assertEquals(0, drill.useCostFe());
    }

    @Test
    void parsesSingleBlockMachineContracts() {
        JsonObject root = JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "id": "magneticraft:electric_furnace",
                  "translation_key": "block.magneticraft.electric_furnace",
                  "description": "guide.magneticraft.machine.electric_furnace.description",
                  "category": "processing",
                  "inventory_slots": 2,
                  "ghost_slots": 0,
                  "has_menu": true,
                  "redstone_control": "ignored",
                  "processing_kind": "recipe",
                  "automation_profile": "input_output",
                  "slot_roles": ["input", "output"],
                  "physical_ports": ["electric"]
                }
                """).getAsJsonObject();

        GuideRepository.MachineGuide guide = GuideRepository.parseMachine(MACHINE_FILE, root);

        assertEquals("magneticraft:electric_furnace", guide.id().toString());
        assertEquals("block.magneticraft.electric_furnace", guide.translationKey());
        assertEquals("guide.magneticraft.machine.electric_furnace.description", guide.descriptionKey());
        assertEquals("processing", guide.category());
        assertEquals(2, guide.inventorySlots());
        assertEquals(0, guide.ghostSlots());
        assertTrue(guide.hasMenu());
        assertEquals("ignored", guide.redstoneControl());
        assertEquals("recipe", guide.processingKind());
        assertEquals("input_output", guide.automationProfile());
        assertEquals(List.of("input", "output"), guide.slotRoles());
        assertEquals(List.of("electric"), guide.physicalPorts());
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
