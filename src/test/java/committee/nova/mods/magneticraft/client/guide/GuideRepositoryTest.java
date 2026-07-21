package committee.nova.mods.magneticraft.client.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPreset;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideRepositoryTest {
    @Test
    void generatedReactorStarterBlueprintsLoadThroughTheClientRepositoryContract() throws IOException {
        for (NuclearReactorPreset preset : NuclearReactorPreset.values()) {
            String id = preset.id();
            Path path = Path.of("src/generated/resources/assets/magneticraft/guide/multiblocks/"
                    + "pressurized_water_reactor_" + id + ".json");
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            GuideRepository.MultiblockGuide guide = GuideRepository.parseMultiblock(
                    ResourceLocation.fromNamespaceAndPath("magneticraft", path.getFileName().toString()), root);
            assertEquals(7, guide.layers().size(), id);
            assertEquals(preset.length(), guide.layers().get(0).size(), id);
            assertEquals(preset.width(), guide.layers().get(0).get(0).length(), id);
            assertTrue(guide.legend().containsKey('M'), id);
            assertTrue(guide.legend().containsKey('F'), id);
            assertTrue(guide.legend().values().stream()
                    .filter(entry -> !entry.ignored())
                    .allMatch(entry -> entry.blockId() != null), id);
            assertEquals(List.of(64_000, 64_000), guide.ports().fluidTankCapacitiesMb(), id);
        }
    }

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
    private static final ResourceLocation LANGUAGE_FILE = ResourceLocation.fromNamespaceAndPath(
            "magneticraft",
            "guide/computer_languages.json"
    );

    @Test
    void parsesMirroringAndPortSummary() {
        JsonObject root = baseGuide();
        root.addProperty("supports_mirroring", true);
        root.addProperty("description", "guide.magneticraft.multiblock.test.description");
        root.addProperty("recipe_type", "magneticraft:advanced_processing");
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
        assertEquals("guide.magneticraft.multiblock.test.description", guide.descriptionKey());
        assertEquals("magneticraft:advanced_processing", guide.recipeType());
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
        assertEquals("guide.magneticraft.multiblock.test.description", guide.descriptionKey());
        assertEquals("", guide.recipeType());
        assertEquals(0, guide.ports().inventorySlots());
        assertEquals(0, guide.ports().bulkItemCapacity());
        assertFalse(guide.ports().electricity());
        assertFalse(guide.ports().heat());
        assertTrue(guide.ports().fluidTankCapacitiesMb().isEmpty());
        assertNull(guide.legend().get('C').blockId());
    }

    @Test
    void parsesThreeDimensionalPreviewBlockStatesAndIgnoredCells() {
        JsonObject root = JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "id": "magneticraft:test",
                  "translation_key": "block.magneticraft.test",
                  "category": "processing",
                  "layers": [["CX"]],
                  "legend": [
                    {
                      "symbol": "C",
                      "rule": "column_x",
                      "block": "magneticraft:machine_support_column",
                      "properties": {"axis": "x"}
                    },
                    {"symbol": "X", "rule": "ignore", "ignored": true}
                  ]
                }
                """).getAsJsonObject();

        GuideRepository.MultiblockGuide guide = GuideRepository.parseMultiblock(FILE, root);

        GuideRepository.LegendEntry column = guide.legend().get('C');
        assertEquals("magneticraft:machine_support_column", column.blockId().toString());
        assertEquals(Map.of("axis", "x"), column.properties());
        assertFalse(column.ignored());
        assertTrue(guide.legend().get('X').ignored());
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
                  "recipe_type": "minecraft:smelting",
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
        assertEquals("minecraft:smelting", guide.recipeType());
        assertEquals("input_output", guide.automationProfile());
        assertEquals(List.of("input", "output"), guide.slotRoles());
        assertEquals(List.of("electric"), guide.physicalPorts());
    }

    @Test
    void parsesBoundedComputerLanguageContracts() {
        JsonObject root = JsonParser.parseString("""
                {
                  "schema_version": 1,
                  "limits": {
                    "source_bytes": 4096,
                    "output_characters": 2048,
                    "instructions_per_tick": 1000,
                    "device_calls_per_tick": 8,
                    "floppy_bytes": 32768,
                    "floppy_entries": 64,
                    "quarry_max_size": 32
                  },
                  "languages": [
                    {
                      "id": "shell",
                      "historical_version": "1.1",
                      "examples": ["help"],
                      "commands": ["help", "quarry"]
                    },
                    {
                      "id": "forth",
                      "historical_version": "1.1",
                      "examples": ["2 5 + ."],
                      "commands": ["MINE"]
                    }
                  ],
                  "security": {
                    "server_authoritative": true,
                    "menu_session_replay_protection": true,
                    "host_filesystem_access": false,
                    "outbound_network_access": false,
                    "force_load_chunks": false
                  }
                }
                """).getAsJsonObject();

        List<GuideRepository.ComputerLanguageGuide> languages =
                GuideRepository.parseLanguages(LANGUAGE_FILE, root);

        assertEquals(List.of("forth", "shell"), languages.stream()
                .map(GuideRepository.ComputerLanguageGuide::id)
                .toList());
        GuideRepository.ComputerLanguageGuide forth = languages.get(0);
        assertEquals(4_096, forth.limits().sourceBytes());
        assertEquals(8, forth.limits().deviceCallsPerTick());
        assertTrue(forth.security().serverAuthoritative());
        assertTrue(forth.security().menuSessionReplayProtection());
        assertFalse(forth.security().hostFilesystemAccess());
        assertFalse(forth.security().outboundNetworkAccess());
        assertFalse(forth.security().forceLoadChunks());
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
