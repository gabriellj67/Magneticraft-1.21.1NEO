package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.ElectricToolItem;
import committee.nova.mods.magneticraft.content.item.LowBatteryItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockPortLayout;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockRule;
import committee.nova.mods.magneticraft.content.multiblock.ShelvingStorageModule;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates client guide snapshots from the same immutable definitions used by
 * server-side multiblock validation.
 */
final class AdvancedGuideDataProvider implements DataProvider {
    static final int SCHEMA_VERSION = 1;

    private final PackOutput.PathProvider multiblockGuides;
    private final PackOutput.PathProvider guideFiles;
    private final PackOutput.PathProvider itemGuides;
    private final PackOutput.PathProvider machineGuides;
    private final List<GuideOpcodeEntry> opcodes;

    AdvancedGuideDataProvider(PackOutput output, Collection<GuideOpcodeEntry> opcodes) {
        multiblockGuides = output.createPathProvider(
                PackOutput.Target.RESOURCE_PACK,
                "guide/multiblocks"
        );
        guideFiles = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "guide");
        itemGuides = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "guide/items");
        machineGuides = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "guide/machines");
        this.opcodes = normalizeOpcodes(opcodes);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<MachineGuideContract> machines = singleBlockMachineGuides();
        List<CompletableFuture<?>> writes = new ArrayList<>(
                MultiblockDefinition.values().length + machines.size() + 3
        );
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            Path path = multiblockGuides.json(Magneticraft.id(definition.id()));
            writes.add(DataProvider.saveStable(output, multiblockGuide(definition), path));
        }
        writes.add(DataProvider.saveStable(
                output,
                opcodeGuide(opcodes),
                guideFiles.json(Magneticraft.id("computer_opcodes"))
        ));
        writes.add(DataProvider.saveStable(
                output,
                computerLanguageGuide(),
                guideFiles.json(Magneticraft.id("computer_languages"))
        ));
        writes.add(DataProvider.saveStable(
                output,
                portableItemGuide(),
                itemGuides.json(Magneticraft.id("portable_electric"))
        ));
        for (MachineGuideContract machine : machines) {
            writes.add(DataProvider.saveStable(
                    output,
                    machineGuide(machine),
                    machineGuides.json(Magneticraft.id(machine.id()))
            ));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Magneticraft advanced-system guide data";
    }

    static JsonObject multiblockGuide(MultiblockDefinition definition) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        root.addProperty("id", Magneticraft.MOD_ID + ":" + definition.id());
        root.addProperty("category", category(definition));
        root.addProperty("controller", Magneticraft.MOD_ID + ":" + definition.id());
        root.addProperty("translation_key", "block.magneticraft." + definition.id());
        root.addProperty("description", "guide.magneticraft.multiblock." + definition.id() + ".description");
        String recipeType = multiblockRecipeType(definition);
        if (!recipeType.isEmpty()) {
            root.addProperty("recipe_type", recipeType);
        }
        root.addProperty("supports_mirroring", false);
        root.add("size", offset(definition.size().x(), definition.size().y(), definition.size().z()));
        root.add("anchor", offset(definition.center().x(), definition.center().y(), definition.center().z()));

        JsonArray layers = new JsonArray();
        definition.layers().forEach(layer -> {
            JsonArray rows = new JsonArray();
            layer.forEach(rows::add);
            layers.add(rows);
        });
        root.add("layers", layers);
        root.add("legend", legend(definition));
        root.add("ports", portSummary(definition));
        return root;
    }

    static JsonObject opcodeGuide(Collection<GuideOpcodeEntry> entries) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        JsonArray opcodes = new JsonArray();
        normalizeOpcodes(entries).forEach(entry -> {
            JsonObject opcode = new JsonObject();
            opcode.addProperty("id", entry.id());
            opcode.addProperty("code", entry.code());
            opcode.addProperty("operand_count", entry.operandCount());
            opcode.addProperty("description", entry.descriptionTranslationKey());
            opcodes.add(opcode);
        });
        root.add("opcodes", opcodes);
        return root;
    }

    static JsonObject computerLanguageGuide() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        JsonObject limits = new JsonObject();
        limits.addProperty("source_bytes", ScriptRuntime.MAX_SOURCE_BYTES);
        limits.addProperty("output_characters", ScriptRuntime.MAX_OUTPUT_CHARACTERS);
        limits.addProperty("instructions_per_tick", ScriptRuntime.MAX_INSTRUCTIONS_PER_TICK);
        limits.addProperty("device_calls_per_tick", ScriptRuntime.MAX_DEVICE_CALLS_PER_TICK);
        limits.addProperty("floppy_bytes", VirtualDisk.CAPACITY_BYTES);
        limits.addProperty("floppy_entries", VirtualDisk.MAX_ENTRIES);
        limits.addProperty("quarry_max_size", MiningRobotBlockEntity.MAX_QUARRY_SIZE);
        root.add("limits", limits);

        JsonArray languages = new JsonArray();
        languages.add(language(
                ScriptLanguage.FORTH,
                "1.1",
                List.of("2 5 + .", "WORDS"),
                List.of("MINE", "FRONT", "FORWARD", "BACK", "LEFT", "RIGHT", "UP", "DOWN", "SCAN")
        ));
        languages.add(language(
                ScriptLanguage.LISP,
                "2.0",
                List.of("(print 5)", "(define x 5)", "(env)"),
                List.of("mine", "front", "back", "left", "right", "up", "down", "scan")
        ));
        languages.add(language(
                ScriptLanguage.SHELL,
                "1.1",
                List.of("help", "quarry 10"),
                List.of("help", "ls", "cd", "mkdir", "rm", "format", "free", "fs", "cat", "touch",
                        "write", "update_disk", "quarry")
        ));
        root.add("languages", languages);

        JsonObject security = new JsonObject();
        security.addProperty("server_authoritative", true);
        security.addProperty("menu_session_replay_protection", true);
        security.addProperty("host_filesystem_access", false);
        security.addProperty("outbound_network_access", false);
        security.addProperty("force_load_chunks", false);
        root.add("security", security);
        return root;
    }

    private static JsonObject language(
            ScriptLanguage language,
            String historicalVersion,
            List<String> examples,
            List<String> commands
    ) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", language.serializedName());
        entry.addProperty("historical_version", historicalVersion);
        JsonArray encodedExamples = new JsonArray();
        examples.forEach(encodedExamples::add);
        entry.add("examples", encodedExamples);
        JsonArray encodedCommands = new JsonArray();
        commands.forEach(encodedCommands::add);
        entry.add("commands", encodedCommands);
        return entry;
    }

    static JsonObject portableItemGuide() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        JsonArray items = new JsonArray();
        items.add(portableItem("low_voltage_battery", LowBatteryItem.CAPACITY, 0, 0, 0));
        items.add(portableItem("medium_voltage_battery", MediumBatteryItem.CAPACITY, 0, 0, 0));
        items.add(portableItem(
                "electric_drill",
                ElectricToolItem.CAPACITY,
                ElectricToolItem.BLOCK_BREAK_COST,
                ElectricToolItem.ATTACK_COST,
                0
        ));
        items.add(portableItem(
                "electric_chainsaw",
                ElectricToolItem.CAPACITY,
                ElectricToolItem.BLOCK_BREAK_COST,
                ElectricToolItem.ATTACK_COST,
                0
        ));
        items.add(portableItem(
                "electric_piston",
                ElectricToolItem.CAPACITY,
                0,
                ElectricToolItem.ATTACK_COST,
                ElectricPistonItem.PUSH_COST
        ));
        items.add(portableItem("voltmeter", 0, 0, 0, 0));
        items.add(portableItem("thermometer", 0, 0, 0, 0));
        items.add(electricEquipmentItem("copper_wire_coil", false));
        items.add(electricEquipmentItem("electric_connector", true));
        items.add(electricEquipmentItem("electric_pole", true));
        items.add(electricEquipmentItem("electric_pole_transformer", true));
        items.add(electricEquipmentItem("tesla_tower", true));
        items.add(electricEquipmentItem("wireless_energy_receiver", true));
        items.add(electricEquipmentItem("wind_turbine", true));
        items.add(electricEquipmentItem("wrench", false));
        items.add(electricEquipmentItem("electrical_fuse", false));
        items.add(electricEquipmentItem("electrical_repair_tool", false));
        items.add(electricEquipmentItem("electric_cable", true));
        items.add(electricEquipmentItem("box_transformer", true));
        items.add(electricEquipmentItem("fuse_box", true));
        items.add(electricEquipmentItem("circuit_breaker", true));
        items.add(electricEquipmentItem("heat_pipe", true));
        items.add(electricEquipmentItem("insulated_heat_pipe", true));
        items.add(electricEquipmentItem("heat_sink", true));
        items.add(electricEquipmentItem("iron_fluid_pipe", true));
        items.add(electricEquipmentItem("pneumatic_tube", true));
        items.add(electricEquipmentItem("pneumatic_restriction_tube", true));
        items.add(electricEquipmentItem("conveyor_belt", true));
        items.add(electricEquipmentItem("inserter_speed_upgrade", false));
        items.add(electricEquipmentItem("inserter_stack_upgrade", false));
        items.add(electricEquipmentItem("computer", true));
        items.add(electricEquipmentItem("mining_robot", true));
        items.add(electricEquipmentItem("floppy_disk", false));
        items.add(electricEquipmentItem("oil_deposit", true));
        root.add("items", items);
        return root;
    }

    static List<MachineGuideContract> singleBlockMachineGuides() {
        List<MachineGuideContract> machines = new ArrayList<>(SingleBlockMachineDefinition.values().length + 3);
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            machines.add(new MachineGuideContract(
                    definition.id(),
                    "block.magneticraft." + definition.id(),
                    "guide.magneticraft.machine." + definition.id() + ".description",
                    definition.guideCategory(),
                    definition.inventorySlots(),
                    definition.ghostSlots(),
                    definition.hasMenu(),
                    definition.redstoneControl().name().toLowerCase(Locale.ROOT),
                    definition.processingKind().name().toLowerCase(Locale.ROOT),
                    definition.automationProfile().name().toLowerCase(Locale.ROOT),
                    definition.slotRoles().stream().map(role -> role.name().toLowerCase(Locale.ROOT)).toList(),
                    definition.physicalPorts().stream()
                            .map(port -> port.name().toLowerCase(Locale.ROOT))
                            .sorted()
                            .toList()
            ));
        }
        machines.add(new MachineGuideContract(
                "crushing_table",
                "block.magneticraft.crushing_table",
                "guide.magneticraft.machine.crushing_table.description",
                "processing",
                1,
                0,
                false,
                "ignored",
                "crushing_table",
                "none",
                List.of("input"),
                List.of()
        ));
        machines.add(new MachineGuideContract(
                "battery_box",
                "block.magneticraft.battery_box",
                "guide.magneticraft.machine.battery_box.description",
                "energy",
                2,
                0,
                true,
                "ignored",
                "none",
                "electric_top_bottom_back",
                List.of("charge", "discharge"),
                List.of("electricity", "item")
        ));
        machines.add(new MachineGuideContract(
                "electric_furnace",
                "block.magneticraft.electric_furnace",
                "guide.magneticraft.machine.electric_furnace.description",
                "processing",
                2,
                0,
                true,
                "ignored",
                "smelting",
                "item_input_output",
                List.of("input", "output"),
                List.of("electricity", "item")
        ));
        machines.sort(Comparator.comparing(MachineGuideContract::id));
        return List.copyOf(machines);
    }

    static JsonObject machineGuide(MachineGuideContract machine) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        root.addProperty("id", Magneticraft.MOD_ID + ":" + machine.id());
        root.addProperty("translation_key", machine.translationKey());
        root.addProperty("description", machine.descriptionKey());
        root.addProperty("category", machine.category());
        root.addProperty("inventory_slots", machine.inventorySlots());
        root.addProperty("ghost_slots", machine.ghostSlots());
        root.addProperty("has_menu", machine.hasMenu());
        root.addProperty("redstone_control", machine.redstoneControl());
        root.addProperty("processing_kind", machine.processingKind());
        String recipeType = machineRecipeType(machine.processingKind());
        if (!recipeType.isEmpty()) {
            root.addProperty("recipe_type", recipeType);
        }
        root.addProperty("automation_profile", machine.automationProfile());
        JsonArray slots = new JsonArray();
        machine.slotRoles().forEach(slots::add);
        root.add("slot_roles", slots);
        JsonArray ports = new JsonArray();
        machine.physicalPorts().forEach(ports::add);
        root.add("physical_ports", ports);
        return root;
    }

    private static JsonObject portableItem(
            String id,
            int capacityFe,
            int breakCostFe,
            int attackCostFe,
            int useCostFe
    ) {
        JsonObject item = new JsonObject();
        item.addProperty("id", Magneticraft.MOD_ID + ":" + id);
        item.addProperty("translation_key", "item.magneticraft." + id);
        item.addProperty("description", "guide.magneticraft.item." + id + ".description");
        item.addProperty("capacity_fe", capacityFe);
        item.addProperty("break_cost_fe", breakCostFe);
        item.addProperty("attack_cost_fe", attackCostFe);
        item.addProperty("use_cost_fe", useCostFe);
        return item;
    }

    private static JsonObject electricEquipmentItem(String id, boolean blockItem) {
        JsonObject item = portableItem(id, 0, 0, 0, 0);
        if (blockItem) {
            item.addProperty("translation_key", "block.magneticraft." + id);
        }
        return item;
    }

    private static String machineRecipeType(String processingKind) {
        return switch (processingKind) {
            case "crushing_table" -> "magneticraft:crushing_table";
            case "sluice_box" -> "magneticraft:sluice_box";
            case "gasification" -> "magneticraft:gasification_unit";
            case "thermopile" -> "magneticraft:thermopile";
            case "smelting" -> "minecraft:smelting";
            case "crafting" -> "minecraft:crafting";
            default -> "";
        };
    }

    private static String multiblockRecipeType(MultiblockDefinition definition) {
        return switch (definition) {
            case GRINDER, HYDRAULIC_PRESS, SIEVE, OIL_HEATER, REFINERY ->
                    "magneticraft:" + definition.id();
            case POLYMERIZER -> "magneticraft:polymerizing";
            case BIG_COMBUSTION_CHAMBER -> "magneticraft:industrial_combustion_chamber";
            case BIG_ELECTRIC_FURNACE -> "minecraft:smelting";
            default -> "";
        };
    }

    static List<GuideOpcodeEntry> computerOpcodes() {
        return Arrays.stream(ComputerOpcode.values())
                .map(opcode -> new GuideOpcodeEntry(
                        opcode.serializedName(),
                        opcode.networkId(),
                        opcode.operandCount(),
                        opcode.descriptionTranslationKey()
                ))
                .toList();
    }

    static List<GuideOpcodeEntry> normalizeOpcodes(Collection<GuideOpcodeEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("Opcode guide entries must not be null");
        }
        Set<String> ids = new HashSet<>();
        Set<Integer> codes = new HashSet<>();
        List<GuideOpcodeEntry> copy = new ArrayList<>(entries.size());
        for (GuideOpcodeEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("Opcode guide entry must not be null");
            }
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate opcode ID: " + entry.id());
            }
            if (!codes.add(entry.code())) {
                throw new IllegalArgumentException("Duplicate opcode code: " + entry.code());
            }
            copy.add(entry);
        }
        copy.sort(Comparator.comparingInt(GuideOpcodeEntry::code).thenComparing(GuideOpcodeEntry::id));
        return List.copyOf(copy);
    }

    private static JsonObject offset(int x, int y, int z) {
        JsonObject offset = new JsonObject();
        offset.addProperty("x", x);
        offset.addProperty("y", y);
        offset.addProperty("z", z);
        return offset;
    }

    private static JsonArray legend(MultiblockDefinition definition) {
        Set<MultiblockRule> used = new HashSet<>();
        definition.cells().forEach(cell -> used.add(cell.rule()));

        JsonArray legend = new JsonArray();
        for (MultiblockRule rule : MultiblockRule.values()) {
            if (!used.contains(rule)) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("symbol", Character.toString(rule.symbol()));
            entry.addProperty("rule", rule.name().toLowerCase(Locale.ROOT));
            addBlockState(entry, rule, definition);
            legend.add(entry);
        }
        return legend;
    }

    private static void addBlockState(
            JsonObject entry,
            MultiblockRule rule,
            MultiblockDefinition definition
    ) {
        switch (rule) {
            case IGNORE -> entry.addProperty("ignored", true);
            case AIR -> entry.addProperty("block", "minecraft:air");
            case CONTROLLER -> entry.addProperty("block", Magneticraft.MOD_ID + ":" + definition.id());
            case BASE -> entry.addProperty("block", "magneticraft:machine_casing");
            case GRATE -> entry.addProperty("block", "magneticraft:iron_grate");
            case CORRUGATED_IRON -> entry.addProperty("block", "magneticraft:corrugated_iron");
            case COPPER_COIL -> entry.addProperty("block", "magneticraft:copper_coil");
            case BRICKS -> entry.addProperty("block", "minecraft:bricks");
            case SMALL_TANK -> entry.addProperty("block", "magneticraft:small_tank");
            case STRIPED -> entry.addProperty("block", "magneticraft:striped_machine_casing");
            case ELECTRIC -> entry.addProperty("block", "magneticraft:electrical_machine_casing");
            case COLUMN_X -> addColumn(entry, "x");
            case COLUMN_Y -> addColumn(entry, "y");
            case COLUMN_Z -> addColumn(entry, "z");
        }
    }

    private static void addColumn(JsonObject entry, String axis) {
        entry.addProperty("block", "magneticraft:machine_support_column");
        JsonObject properties = new JsonObject();
        properties.addProperty("axis", axis);
        entry.add("properties", properties);
    }

    private static JsonObject portSummary(MultiblockDefinition definition) {
        JsonObject ports = new JsonObject();
        ports.addProperty("inventory_slots", definition.inventorySlots());
        ports.addProperty("bulk_item_capacity", definition.bulkItemCapacity());
        ports.addProperty("electricity", definition.usesElectricity());
        ports.addProperty("heat", definition.usesHeat());
        if (definition == MultiblockDefinition.SHELVING_UNIT) {
            ports.addProperty("chest_upgrade_slots", ShelvingStorageModule.MAX_CHESTS);
            ports.addProperty("inventory_slots_per_chest", ShelvingStorageModule.SLOTS_PER_CHEST);
        }

        JsonArray tankCapacities = new JsonArray();
        for (int index = 0; index < definition.tankCount(); index++) {
            tankCapacities.add(definition.tankCapacity(index));
        }
        ports.add("fluid_tank_capacities_mb", tankCapacities);

        JsonArray tankPorts = new JsonArray();
        for (MultiblockPortProfile.TankPort port : MultiblockPortProfile.tanks(definition)) {
            JsonObject tank = new JsonObject();
            tank.addProperty("index", port.index());
            tank.addProperty("role", port.role());
            tank.addProperty("capacity_mb", port.capacity());
            JsonArray accepted = new JsonArray();
            port.acceptedFluids().forEach(accepted::add);
            tank.add("accepted_fluids", accepted);
            tankPorts.add(tank);
        }
        ports.add("fluid_tanks", tankPorts);

        JsonArray connections = new JsonArray();
        for (MultiblockPortLayout.Port port : MultiblockPortLayout.ports(definition)) {
            JsonObject connection = new JsonObject();
            connection.addProperty("kind", port.kind().name().toLowerCase(Locale.ROOT));
            JsonObject offset = new JsonObject();
            offset.addProperty("x", port.offset().x());
            offset.addProperty("y", port.offset().y());
            offset.addProperty("z", port.offset().z());
            connection.add("offset", offset);
            connection.addProperty("side", port.side().getName());
            if (port.target() >= 0) {
                connection.addProperty("target", port.target());
            }
            if (port.kind() == MultiblockPortLayout.Kind.FLUID) {
                connection.addProperty("access", port.fluidAccess().name().toLowerCase(Locale.ROOT));
                connection.addProperty("role", MultiblockPortProfile.tank(definition, port.target()).role());
            } else if (port.kind() == MultiblockPortLayout.Kind.ITEM) {
                connection.add("insert_slots", intArray(port.itemAccess().insertSlots()));
                connection.add("extract_slots", intArray(port.itemAccess().extractSlots()));
            }
            connections.add(connection);
        }
        ports.add("connections", connections);
        return ports;
    }

    private static JsonArray intArray(int[] values) {
        JsonArray result = new JsonArray();
        for (int value : values) {
            result.add(value);
        }
        return result;
    }

    private static String category(MultiblockDefinition definition) {
        return switch (definition) {
            case CONTAINER, SHELVING_UNIT -> "storage";
            case GRINDER, HYDRAULIC_PRESS, SIEVE, POLYMERIZER, BIG_ELECTRIC_FURNACE -> "processing";
            case OIL_HEATER, PUMPJACK, REFINERY -> "oil";
            default -> "energy";
        };
    }

    record MachineGuideContract(
            String id,
            String translationKey,
            String descriptionKey,
            String category,
            int inventorySlots,
            int ghostSlots,
            boolean hasMenu,
            String redstoneControl,
            String processingKind,
            String automationProfile,
            List<String> slotRoles,
            List<String> physicalPorts
    ) {
        MachineGuideContract {
            if (id.isBlank() || translationKey.isBlank() || descriptionKey.isBlank() || category.isBlank()) {
                throw new IllegalArgumentException("Machine guide identifiers must not be blank");
            }
            if (inventorySlots < 0 || ghostSlots < 0 || slotRoles.size() != inventorySlots) {
                throw new IllegalArgumentException("Invalid slot contract for " + id);
            }
            slotRoles = List.copyOf(slotRoles);
            physicalPorts = List.copyOf(physicalPorts);
        }
    }
}
