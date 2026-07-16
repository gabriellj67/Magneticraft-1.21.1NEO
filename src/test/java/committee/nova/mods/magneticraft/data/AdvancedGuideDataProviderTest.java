package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.ElectricToolItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockPortLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedGuideDataProviderTest {
    @Test
    void computerLanguageGuidePublishesHistoricalSurfacesAndSecurityBounds() {
        JsonObject guide = AdvancedGuideDataProvider.computerLanguageGuide();
        JsonObject limits = guide.getAsJsonObject("limits");
        assertEquals(ScriptRuntime.MAX_SOURCE_BYTES, limits.get("source_bytes").getAsInt());
        assertEquals(ScriptRuntime.MAX_INSTRUCTIONS_PER_TICK, limits.get("instructions_per_tick").getAsInt());
        assertEquals(VirtualDisk.CAPACITY_BYTES, limits.get("floppy_bytes").getAsInt());
        assertEquals(MiningRobotBlockEntity.MAX_QUARRY_SIZE, limits.get("quarry_max_size").getAsInt());

        JsonArray languages = guide.getAsJsonArray("languages");
        assertEquals(List.of("forth", "lisp", "shell"), languages.asList().stream()
                .map(entry -> entry.getAsJsonObject().get("id").getAsString())
                .toList());
        assertTrue(languages.get(0).getAsJsonObject().getAsJsonArray("examples").toString().contains("2 5 + ."));
        assertTrue(languages.get(2).getAsJsonObject().getAsJsonArray("commands").toString().contains("quarry"));

        JsonObject security = guide.getAsJsonObject("security");
        assertTrue(security.get("server_authoritative").getAsBoolean());
        assertTrue(security.get("menu_session_replay_protection").getAsBoolean());
        assertFalse(security.get("host_filesystem_access").getAsBoolean());
        assertFalse(security.get("outbound_network_access").getAsBoolean());
        assertFalse(security.get("force_load_chunks").getAsBoolean());
    }

    @Test
    void singleBlockGuideCatalogueIsBilingualContractDataFromRuntimeDefinitions() {
        List<AdvancedGuideDataProvider.MachineGuideContract> machines =
                AdvancedGuideDataProvider.singleBlockMachineGuides();
        assertEquals(SingleBlockMachineDefinition.values().length + 3, machines.size());
        assertEquals(machines.size(), machines.stream().map(AdvancedGuideDataProvider.MachineGuideContract::id)
                .distinct().count());

        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            AdvancedGuideDataProvider.MachineGuideContract contract = machines.stream()
                    .filter(machine -> machine.id().equals(definition.id()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(definition.inventorySlots(), contract.inventorySlots(), definition.id());
            assertEquals(definition.ghostSlots(), contract.ghostSlots(), definition.id());
            assertEquals(definition.slotRoles().size(), contract.slotRoles().size(), definition.id());
            assertEquals("ignored", contract.redstoneControl(), definition.id());
            assertEquals(
                    definition.automationProfile().name().toLowerCase(java.util.Locale.ROOT),
                    contract.automationProfile(),
                    definition.id()
            );
            assertEquals(
                    "guide.magneticraft.machine." + definition.id() + ".description",
                    contract.descriptionKey()
            );
            JsonObject json = AdvancedGuideDataProvider.machineGuide(contract);
            assertEquals("magneticraft:" + definition.id(), json.get("id").getAsString());
            assertEquals(definition.physicalPorts().size(), json.getAsJsonArray("physical_ports").size());
            if (Set.of("sluice_box", "gasification", "thermopile", "smelting", "crafting")
                    .contains(contract.processingKind())) {
                assertTrue(json.has("recipe_type"), definition.id());
            }
        }

        AdvancedGuideDataProvider.MachineGuideContract water = machines.stream()
                .filter(machine -> machine.id().equals("water_generator"))
                .findFirst()
                .orElseThrow();
        assertFalse(water.hasMenu());
        assertEquals("fluid_output_all_sides", water.automationProfile());
    }

    @Test
    void everyMultiblockGuideIsAnExactSnapshotOfTheRuntimeCatalogue() {
        Set<String> ids = new HashSet<>();
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            JsonObject guide = AdvancedGuideDataProvider.multiblockGuide(definition);
            assertEquals(AdvancedGuideDataProvider.SCHEMA_VERSION,
                    guide.get("schema_version").getAsInt());
            assertEquals("magneticraft:" + definition.id(), guide.get("id").getAsString());
            assertEquals("magneticraft:" + definition.id(), guide.get("controller").getAsString());
            assertEquals(
                    "guide.magneticraft.multiblock." + definition.id() + ".description",
                    guide.get("description").getAsString()
            );
            boolean hasRecipe = Set.of(
                    MultiblockDefinition.GRINDER,
                    MultiblockDefinition.HYDRAULIC_PRESS,
                    MultiblockDefinition.SIEVE,
                    MultiblockDefinition.OIL_HEATER,
                    MultiblockDefinition.REFINERY,
                    MultiblockDefinition.POLYMERIZER,
                    MultiblockDefinition.BIG_COMBUSTION_CHAMBER,
                    MultiblockDefinition.BIG_ELECTRIC_FURNACE
            ).contains(definition);
            assertEquals(hasRecipe, guide.has("recipe_type"), definition.id());
            if (hasRecipe) {
                assertEquals(
                        definition == MultiblockDefinition.BIG_ELECTRIC_FURNACE
                                ? "minecraft:smelting"
                                : definition == MultiblockDefinition.POLYMERIZER
                                        ? "magneticraft:polymerizing"
                                        : "magneticraft:" + definition.id(),
                        guide.get("recipe_type").getAsString(),
                        definition.id()
                );
            }
            assertFalse(guide.get("supports_mirroring").getAsBoolean());
            assertTrue(ids.add(guide.get("id").getAsString()));

            assertOffset(guide.getAsJsonObject("size"), definition.size().x(), definition.size().y(), definition.size().z());
            assertOffset(
                    guide.getAsJsonObject("anchor"),
                    definition.center().x(),
                    definition.center().y(),
                    definition.center().z()
            );

            JsonArray layers = guide.getAsJsonArray("layers");
            assertEquals(definition.layers().size(), layers.size());
            Set<String> usedSymbols = new HashSet<>();
            for (int y = 0; y < layers.size(); y++) {
                JsonArray rows = layers.get(y).getAsJsonArray();
                assertEquals(definition.layers().get(y).size(), rows.size());
                for (int z = 0; z < rows.size(); z++) {
                    String row = rows.get(z).getAsString();
                    assertEquals(definition.layers().get(y).get(z), row);
                    row.chars().mapToObj(value -> Character.toString((char) value)).forEach(usedSymbols::add);
                }
            }

            Set<String> legendSymbols = new HashSet<>();
            guide.getAsJsonArray("legend").forEach(entry -> legendSymbols.add(
                    entry.getAsJsonObject().get("symbol").getAsString()
            ));
            assertEquals(usedSymbols, legendSymbols);

            JsonObject ports = guide.getAsJsonObject("ports");
            assertEquals(definition.inventorySlots(), ports.get("inventory_slots").getAsInt());
            assertEquals(definition.usesElectricity(), ports.get("electricity").getAsBoolean());
            assertEquals(definition.usesHeat(), ports.get("heat").getAsBoolean());
            assertEquals(definition.tankCount(), ports.getAsJsonArray("fluid_tank_capacities_mb").size());
            assertEquals(definition.tankCount(), ports.getAsJsonArray("fluid_tanks").size());
            assertEquals(MultiblockPortLayout.ports(definition).size(),
                    ports.getAsJsonArray("connections").size());
        }
        assertEquals(18, ids.size());
    }

    @Test
    void refineryGuideExposesExactRuntimeConnectionCoordinatesAndTypedAccess() {
        JsonObject ports = AdvancedGuideDataProvider.multiblockGuide(MultiblockDefinition.REFINERY)
                .getAsJsonObject("ports");
        JsonArray tanks = ports.getAsJsonArray("fluid_tanks");

        assertEquals(5, tanks.size());
        JsonObject feed = tanks.get(0).getAsJsonObject();
        assertEquals("feed_input", feed.get("role").getAsString());
        assertEquals(16_000, feed.get("capacity_mb").getAsInt());
        assertFalse(feed.has("access"));
        assertTrue(feed.getAsJsonArray("accepted_fluids").toString().contains("magneticraft:heated_crude_oil"));

        JsonObject processSteam = tanks.get(1).getAsJsonObject();
        assertEquals("process_steam_input", processSteam.get("role").getAsString());

        List<JsonObject> connections = ports.getAsJsonArray("connections").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .toList();
        assertEquals(15, connections.size());
        JsonObject feedConnection = connections.stream()
                .filter(connection -> connection.get("target").getAsInt() == 0)
                .findFirst()
                .orElseThrow();
        assertEquals("fluid", feedConnection.get("kind").getAsString());
        assertEquals("north", feedConnection.get("side").getAsString());
        assertEquals("both", feedConnection.get("access").getAsString());
        assertEquals("feed_input", feedConnection.get("role").getAsString());
        assertOffset(feedConnection.getAsJsonObject("offset"), 0, 1, -2);

        assertEquals(2, connections.stream()
                .filter(connection -> connection.get("target").getAsInt() == 1)
                .filter(connection -> "both".equals(connection.get("access").getAsString()))
                .count());
        for (int target = 2; target <= 4; target++) {
            int expectedTarget = target;
            assertEquals(4, connections.stream()
                    .filter(connection -> connection.get("target").getAsInt() == expectedTarget)
                    .filter(connection -> "output".equals(connection.get("access").getAsString()))
                    .count());
        }
    }

    @Test
    void opcodeGuideUsesExplicitValidatedInjectionAndStableCodeOrdering() {
        List<GuideOpcodeEntry> injected = List.of(
                new GuideOpcodeEntry("move", 2, 1, "guide.magneticraft.opcode.move"),
                new GuideOpcodeEntry("halt", 0, 0, "guide.magneticraft.opcode.halt")
        );

        JsonArray opcodes = AdvancedGuideDataProvider.opcodeGuide(injected).getAsJsonArray("opcodes");
        assertEquals(2, opcodes.size());
        assertEquals("halt", opcodes.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("move", opcodes.get(1).getAsJsonObject().get("id").getAsString());
        assertFalse(opcodes.get(0).getAsJsonObject().has("implementation_class"));

        assertThrows(IllegalArgumentException.class, () -> AdvancedGuideDataProvider.normalizeOpcodes(List.of(
                new GuideOpcodeEntry("halt", 0, 0, "guide.magneticraft.opcode.halt"),
                new GuideOpcodeEntry("stop", 0, 0, "guide.magneticraft.opcode.stop")
        )));
        assertThrows(IllegalArgumentException.class, () -> AdvancedGuideDataProvider.normalizeOpcodes(List.of(
                new GuideOpcodeEntry("halt", 0, 0, "guide.magneticraft.opcode.halt"),
                new GuideOpcodeEntry("halt", 1, 0, "guide.magneticraft.opcode.halt_alias")
        )));
    }

    @Test
    void generatedOpcodeEntriesCoverTheBoundedVmInstructionSet() {
        List<GuideOpcodeEntry> entries = AdvancedGuideDataProvider.computerOpcodes();
        assertEquals(ComputerOpcode.values().length, entries.size());
        for (ComputerOpcode opcode : ComputerOpcode.values()) {
            GuideOpcodeEntry entry = entries.stream()
                    .filter(candidate -> candidate.code() == opcode.networkId())
                    .findFirst()
                    .orElseThrow();
            assertEquals(opcode.name().toLowerCase(java.util.Locale.ROOT), entry.id());
            assertEquals(opcode.operandCount(), entry.operandCount());
            assertEquals(opcode.descriptionTranslationKey(), entry.descriptionTranslationKey());
        }
    }

    @Test
    void portableItemGuideCapturesTheRestoredEnergyContracts() {
        JsonArray items = AdvancedGuideDataProvider.portableItemGuide().getAsJsonArray("items");

        assertEquals(35, items.size());
        assertEquals(35, items.asList().stream()
                .map(element -> element.getAsJsonObject().get("id").getAsString())
                .distinct()
                .count());
        JsonObject mediumBattery = item(items, "magneticraft:medium_voltage_battery");
        assertEquals(MediumBatteryItem.CAPACITY, mediumBattery.get("capacity_fe").getAsInt());
        JsonObject drill = item(items, "magneticraft:electric_drill");
        assertEquals(ElectricToolItem.CAPACITY, drill.get("capacity_fe").getAsInt());
        assertEquals(ElectricToolItem.BLOCK_BREAK_COST, drill.get("break_cost_fe").getAsInt());
        assertEquals(ElectricToolItem.ATTACK_COST, drill.get("attack_cost_fe").getAsInt());
        JsonObject piston = item(items, "magneticraft:electric_piston");
        assertEquals(ElectricPistonItem.PUSH_COST, piston.get("use_cost_fe").getAsInt());
        JsonObject voltmeter = item(items, "magneticraft:voltmeter");
        assertEquals(0, voltmeter.get("capacity_fe").getAsInt());
        for (String id : List.of(
                "copper_wire_coil",
                "electric_connector",
                "electric_pole",
                "electric_pole_transformer",
                "electrical_fuse",
                "electrical_repair_tool",
                "box_transformer",
                "fuse_box",
                "circuit_breaker",
                "tesla_tower",
                "wireless_energy_receiver",
                "wind_turbine",
                "wrench",
                "electric_cable",
                "heat_pipe",
                "insulated_heat_pipe",
                "heat_sink",
                "iron_fluid_pipe",
                "pneumatic_tube",
                "pneumatic_restriction_tube",
                "conveyor_belt",
                "inserter_speed_upgrade",
                "inserter_stack_upgrade",
                "computer",
                "mining_robot",
                "floppy_disk",
                "oil_deposit"
        )) {
            JsonObject equipment = item(items, "magneticraft:" + id);
            assertEquals(0, equipment.get("capacity_fe").getAsInt());
            assertEquals("guide.magneticraft.item." + id + ".description",
                    equipment.get("description").getAsString());
        }
    }

    @Test
    void generatedGuideAndFaultKeysHaveCompleteEnglishAndChineseTranslations() throws IOException {
        Path languageRoot = Path.of("src/generated/resources/assets/magneticraft/lang");
        JsonObject english = JsonParser.parseString(Files.readString(
                languageRoot.resolve("en_us.json"), StandardCharsets.UTF_8
        )).getAsJsonObject();
        JsonObject chinese = JsonParser.parseString(Files.readString(
                languageRoot.resolve("zh_cn.json"), StandardCharsets.UTF_8
        )).getAsJsonObject();
        assertEquals(english.keySet(), chinese.keySet(), "en_us and zh_cn translation keys drifted");

        Set<String> requiredKeys = new HashSet<>();
        Path guideRoot = Path.of("src/generated/resources/assets/magneticraft/guide");
        try (var paths = Files.walk(guideRoot)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".json"))
                    .toList()) {
                collectTranslationKeys(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)), requiredKeys);
            }
        }
        for (String language : List.of("forth", "lisp", "shell")) {
            requiredKeys.add("guide.magneticraft.computer.language." + language + ".name");
            requiredKeys.add("guide.magneticraft.computer.language." + language + ".description");
        }
        for (VmFault fault : VmFault.values()) {
            if (fault != VmFault.NONE) {
                requiredKeys.add("gui.magneticraft.programmable.fault."
                        + fault.name().toLowerCase(Locale.ROOT));
            }
        }
        for (String key : requiredKeys) {
            assertTrue(english.has(key), "Missing en_us guide/fault translation: " + key);
            assertTrue(chinese.has(key), "Missing zh_cn guide/fault translation: " + key);
            assertFalse(english.get(key).getAsString().isBlank(), "Blank en_us translation: " + key);
            assertFalse(chinese.get(key).getAsString().isBlank(), "Blank zh_cn translation: " + key);
        }
    }

    private static void collectTranslationKeys(JsonElement element, Set<String> destination) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectTranslationKeys(child, destination));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (var entry : element.getAsJsonObject().entrySet()) {
            if ((entry.getKey().equals("translation_key") || entry.getKey().equals("description"))
                    && entry.getValue().isJsonPrimitive()
                    && entry.getValue().getAsString().contains(".magneticraft.")) {
                destination.add(entry.getValue().getAsString());
            } else {
                collectTranslationKeys(entry.getValue(), destination);
            }
        }
    }

    private static JsonObject item(JsonArray items, String id) {
        return items.asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(element -> element.get("id").getAsString().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void assertOffset(JsonObject offset, int x, int y, int z) {
        assertEquals(x, offset.get("x").getAsInt());
        assertEquals(y, offset.get("y").getAsInt());
        assertEquals(z, offset.get("z").getAsInt());
    }
}
