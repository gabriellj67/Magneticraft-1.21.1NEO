package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.ElectricToolItem;
import committee.nova.mods.magneticraft.content.item.LowBatteryItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
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
    private final List<GuideOpcodeEntry> opcodes;

    AdvancedGuideDataProvider(PackOutput output, Collection<GuideOpcodeEntry> opcodes) {
        multiblockGuides = output.createPathProvider(
                PackOutput.Target.RESOURCE_PACK,
                "guide/multiblocks"
        );
        guideFiles = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "guide");
        itemGuides = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "guide/items");
        this.opcodes = normalizeOpcodes(opcodes);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>(MultiblockDefinition.values().length + 2);
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
                portableItemGuide(),
                itemGuides.json(Magneticraft.id("portable_electric"))
        ));
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
        root.addProperty("supports_mirroring", true);
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

    static JsonObject portableItemGuide() {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        JsonArray items = new JsonArray();
        items.add(portableItem("battery_item_low", LowBatteryItem.CAPACITY, 0, 0, 0));
        items.add(portableItem("battery_item_medium", MediumBatteryItem.CAPACITY, 0, 0, 0));
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
        root.add("items", items);
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
            case BASE -> entry.addProperty("block", "magneticraft:multiblock_base");
            case GRATE -> entry.addProperty("block", "magneticraft:grate");
            case CORRUGATED_IRON -> entry.addProperty("block", "magneticraft:corrugated_iron");
            case COPPER_COIL -> entry.addProperty("block", "magneticraft:copper_coil");
            case BRICKS -> entry.addProperty("block", "minecraft:bricks");
            case SMALL_TANK -> entry.addProperty("block", "magneticraft:small_tank");
            case STRIPED -> entry.addProperty("block", "magneticraft:striped_multiblock_part");
            case ELECTRIC -> entry.addProperty("block", "magneticraft:electric_multiblock_part");
            case COLUMN_X -> addColumn(entry, "x");
            case COLUMN_Y -> addColumn(entry, "y");
            case COLUMN_Z -> addColumn(entry, "z");
        }
    }

    private static void addColumn(JsonObject entry, String axis) {
        entry.addProperty("block", "magneticraft:multiblock_column");
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

            JsonObject access = new JsonObject();
            access.addProperty("unsided", port.unsidedAccess().name().toLowerCase(Locale.ROOT));
            for (MultiblockPortProfile.RelativeSide side : MultiblockPortProfile.RelativeSide.values()) {
                var mode = port.sideAccess().get(side);
                if (mode != null) {
                    access.addProperty(side.serializedName(), mode.name().toLowerCase(Locale.ROOT));
                }
            }
            tank.add("access", access);
            tankPorts.add(tank);
        }
        ports.add("fluid_tanks", tankPorts);
        return ports;
    }

    private static String category(MultiblockDefinition definition) {
        return switch (definition) {
            case CONTAINER, SHELVING_UNIT -> "storage";
            case GRINDER, HYDRAULIC_PRESS, SIEVE, BIG_ELECTRIC_FURNACE -> "processing";
            case OIL_HEATER, PUMPJACK, REFINERY -> "oil";
            default -> "energy";
        };
    }
}
