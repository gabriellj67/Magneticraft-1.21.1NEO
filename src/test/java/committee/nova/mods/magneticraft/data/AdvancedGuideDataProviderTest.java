package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.ElectricToolItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedGuideDataProviderTest {
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
            assertEquals(AdvancedGuideDataProvider.SCHEMA_VERSION, guide.get("schema_version").getAsInt());
            assertEquals("magneticraft:" + definition.id(), guide.get("id").getAsString());
            assertEquals("magneticraft:" + definition.id(), guide.get("controller").getAsString());
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
        }
        assertEquals(16, ids.size());
    }

    @Test
    void refineryGuideExposesRuntimeTankRolesFluidsAndRelativeSides() {
        JsonArray tanks = AdvancedGuideDataProvider.multiblockGuide(MultiblockDefinition.REFINERY)
                .getAsJsonObject("ports")
                .getAsJsonArray("fluid_tanks");

        assertEquals(5, tanks.size());
        JsonObject feed = tanks.get(0).getAsJsonObject();
        assertEquals("feed_input", feed.get("role").getAsString());
        assertEquals(16_000, feed.get("capacity_mb").getAsInt());
        assertEquals("input", feed.getAsJsonObject("access").get("front").getAsString());
        assertTrue(feed.getAsJsonArray("accepted_fluids").toString().contains("magneticraft:heated_crude_oil"));

        JsonObject processSteam = tanks.get(1).getAsJsonObject();
        assertEquals("process_steam_input", processSteam.get("role").getAsString());
        assertEquals("input", processSteam.getAsJsonObject("access").get("back").getAsString());

        assertEquals("output", tanks.get(2).getAsJsonObject()
                .getAsJsonObject("access").get("left").getAsString());
        assertEquals("output", tanks.get(3).getAsJsonObject()
                .getAsJsonObject("access").get("right").getAsString());
        assertEquals("output", tanks.get(4).getAsJsonObject()
                .getAsJsonObject("access").get("up").getAsString());
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

        assertEquals(14, items.size());
        assertEquals(14, items.asList().stream()
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
                "tesla_tower",
                "wireless_energy_receiver",
                "wind_turbine"
        )) {
            JsonObject equipment = item(items, "magneticraft:" + id);
            assertEquals(0, equipment.get("capacity_fe").getAsInt());
            assertEquals("guide.magneticraft.item." + id + ".description",
                    equipment.get("description").getAsString());
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
