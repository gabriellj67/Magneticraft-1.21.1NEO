package committee.nova.mods.magneticraft.client.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client resource-pack view of generated guide JSON.
 */
public final class GuideRepository extends SimplePreparableReloadListener<GuideRepository.PreparedGuideData> {
    public static final GuideRepository INSTANCE = new GuideRepository();
    private static final int SUPPORTED_SCHEMA = 1;
    private static final ResourceLocation OPCODE_GUIDE = Magneticraft.id("guide/computer_opcodes.json");
    private static final ResourceLocation COMPUTER_LANGUAGE_GUIDE = Magneticraft.id("guide/computer_languages.json");
    private static final ResourceLocation PORTABLE_ITEM_GUIDE = Magneticraft.id("guide/items/portable_electric.json");

    private volatile PreparedGuideData data = PreparedGuideData.EMPTY;

    private GuideRepository() {
    }

    public List<MultiblockGuide> multiblocks() {
        return data.multiblocks();
    }

    public List<OpcodeGuide> opcodes() {
        return data.opcodes();
    }

    public List<ComputerLanguageGuide> languages() {
        return data.languages();
    }

    public List<ItemGuide> items() {
        return data.items();
    }

    public List<MachineGuide> machines() {
        return data.machines();
    }

    @Override
    protected PreparedGuideData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<MultiblockGuide> multiblocks = new ArrayList<>();
        resourceManager.listResources(
                "guide/multiblocks",
                location -> location.getNamespace().equals(Magneticraft.MOD_ID)
                        && location.getPath().endsWith(".json")
        ).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readMultiblock(entry.getKey(), entry.getValue(), multiblocks));

        List<OpcodeGuide> opcodes = resourceManager.getResource(OPCODE_GUIDE)
                .map(this::readOpcodes)
                .orElseGet(List::of);
        List<ComputerLanguageGuide> languages = resourceManager.getResource(COMPUTER_LANGUAGE_GUIDE)
                .map(this::readLanguages)
                .orElseGet(List::of);
        List<ItemGuide> items = resourceManager.getResource(PORTABLE_ITEM_GUIDE)
                .map(this::readItems)
                .orElseGet(List::of);
        List<MachineGuide> machines = new ArrayList<>();
        resourceManager.listResources(
                "guide/machines",
                location -> location.getNamespace().equals(Magneticraft.MOD_ID)
                        && location.getPath().endsWith(".json")
        ).entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> readMachine(entry.getKey(), entry.getValue(), machines));
        return new PreparedGuideData(multiblocks, opcodes, languages, items, machines);
    }

    @Override
    protected void apply(PreparedGuideData prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        data = prepared;
    }

    private void readMultiblock(ResourceLocation file, Resource resource, List<MultiblockGuide> destination) {
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            destination.add(parseMultiblock(file, root));
        } catch (IOException | RuntimeException exception) {
            Magneticraft.LOGGER.warn("Skipping invalid multiblock guide {}", file, exception);
        }
    }

    static MultiblockGuide parseMultiblock(ResourceLocation file, JsonObject root) {
        requireSchema(file, root);
        ResourceLocation id = parseId(file, GsonHelper.getAsString(root, "id"));
        String translationKey = GsonHelper.getAsString(root, "translation_key");
        String descriptionKey = root.has("description")
                ? GsonHelper.getAsString(root, "description")
                : "guide.magneticraft.multiblock." + id.getPath() + ".description";
        String recipeType = root.has("recipe_type") ? GsonHelper.getAsString(root, "recipe_type") : "";
        String category = GsonHelper.getAsString(root, "category");
        List<List<String>> layers = readLayers(file, GsonHelper.getAsJsonArray(root, "layers"));
        Map<Character, LegendEntry> legend = readLegend(file, GsonHelper.getAsJsonArray(root, "legend"));
        boolean supportsMirroring = root.has("supports_mirroring")
                && root.get("supports_mirroring").getAsBoolean();
        PortSummary ports = root.has("ports") && root.get("ports").isJsonObject()
                ? readPortSummary(root.getAsJsonObject("ports"))
                : PortSummary.EMPTY;
        return new MultiblockGuide(
                id,
                translationKey,
                descriptionKey,
                recipeType,
                category,
                layers,
                legend,
                supportsMirroring,
                ports
        );
    }

    private List<OpcodeGuide> readOpcodes(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = GsonHelper.parse(reader);
            requireSchema(OPCODE_GUIDE, root);
            List<OpcodeGuide> opcodes = new ArrayList<>();
            for (JsonElement element : GsonHelper.getAsJsonArray(root, "opcodes")) {
                JsonObject opcode = element.getAsJsonObject();
                opcodes.add(new OpcodeGuide(
                        GsonHelper.getAsString(opcode, "id"),
                        GsonHelper.getAsInt(opcode, "code"),
                        GsonHelper.getAsInt(opcode, "operand_count"),
                        GsonHelper.getAsString(opcode, "description")
                ));
            }
            opcodes.sort(Comparator.comparingInt(OpcodeGuide::code));
            return List.copyOf(opcodes);
        } catch (IOException | RuntimeException exception) {
            Magneticraft.LOGGER.warn("Skipping invalid opcode guide {}", OPCODE_GUIDE, exception);
            return List.of();
        }
    }

    private List<ItemGuide> readItems(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return parseItems(PORTABLE_ITEM_GUIDE, GsonHelper.parse(reader));
        } catch (IOException | RuntimeException exception) {
            Magneticraft.LOGGER.warn("Skipping invalid portable-item guide {}", PORTABLE_ITEM_GUIDE, exception);
            return List.of();
        }
    }

    private List<ComputerLanguageGuide> readLanguages(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return parseLanguages(COMPUTER_LANGUAGE_GUIDE, GsonHelper.parse(reader));
        } catch (IOException | RuntimeException exception) {
            Magneticraft.LOGGER.warn("Skipping invalid computer-language guide {}", COMPUTER_LANGUAGE_GUIDE, exception);
            return List.of();
        }
    }

    static List<ComputerLanguageGuide> parseLanguages(ResourceLocation file, JsonObject root) {
        requireSchema(file, root);
        JsonObject limitData = GsonHelper.getAsJsonObject(root, "limits");
        ComputerLimits limits = new ComputerLimits(
                nonNegativeInt(limitData, "source_bytes"),
                nonNegativeInt(limitData, "output_characters"),
                nonNegativeInt(limitData, "instructions_per_tick"),
                nonNegativeInt(limitData, "device_calls_per_tick"),
                nonNegativeInt(limitData, "floppy_bytes"),
                nonNegativeInt(limitData, "floppy_entries"),
                nonNegativeInt(limitData, "quarry_max_size")
        );
        JsonObject securityData = GsonHelper.getAsJsonObject(root, "security");
        ComputerSecurity security = new ComputerSecurity(
                GsonHelper.getAsBoolean(securityData, "server_authoritative"),
                GsonHelper.getAsBoolean(securityData, "menu_session_replay_protection"),
                GsonHelper.getAsBoolean(securityData, "host_filesystem_access"),
                GsonHelper.getAsBoolean(securityData, "outbound_network_access"),
                GsonHelper.getAsBoolean(securityData, "force_load_chunks")
        );
        List<ComputerLanguageGuide> languages = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "languages")) {
            JsonObject language = element.getAsJsonObject();
            String id = GsonHelper.getAsString(language, "id");
            if (id.isBlank()) {
                throw new IllegalArgumentException("Blank computer language in " + file);
            }
            languages.add(new ComputerLanguageGuide(
                    id,
                    GsonHelper.getAsString(language, "historical_version"),
                    readStrings(language, "examples"),
                    readStrings(language, "commands"),
                    limits,
                    security
            ));
        }
        languages.sort(Comparator.comparing(ComputerLanguageGuide::id));
        return List.copyOf(languages);
    }

    static List<ItemGuide> parseItems(ResourceLocation file, JsonObject root) {
        requireSchema(file, root);
        List<ItemGuide> items = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "items")) {
            JsonObject item = element.getAsJsonObject();
            items.add(new ItemGuide(
                    parseId(file, GsonHelper.getAsString(item, "id")),
                    GsonHelper.getAsString(item, "translation_key"),
                    GsonHelper.getAsString(item, "description"),
                    nonNegativeInt(item, "capacity_fe"),
                    nonNegativeInt(item, "break_cost_fe"),
                    nonNegativeInt(item, "attack_cost_fe"),
                    nonNegativeInt(item, "use_cost_fe")
            ));
        }
        return List.copyOf(items);
    }

    private void readMachine(ResourceLocation file, Resource resource, List<MachineGuide> destination) {
        try (Reader reader = resource.openAsReader()) {
            destination.add(parseMachine(file, GsonHelper.parse(reader)));
        } catch (IOException | RuntimeException exception) {
            Magneticraft.LOGGER.warn("Skipping invalid single-block machine guide {}", file, exception);
        }
    }

    static MachineGuide parseMachine(ResourceLocation file, JsonObject root) {
        requireSchema(file, root);
        int inventorySlots = nonNegativeInt(root, "inventory_slots");
        List<String> slotRoles = readStrings(root, "slot_roles");
        if (slotRoles.size() != inventorySlots) {
            throw new IllegalArgumentException("Machine guide slot count mismatch in " + file);
        }
        return new MachineGuide(
                parseId(file, GsonHelper.getAsString(root, "id")),
                GsonHelper.getAsString(root, "translation_key"),
                GsonHelper.getAsString(root, "description"),
                GsonHelper.getAsString(root, "category"),
                inventorySlots,
                nonNegativeInt(root, "ghost_slots"),
                GsonHelper.getAsBoolean(root, "has_menu"),
                GsonHelper.getAsString(root, "redstone_control"),
                GsonHelper.getAsString(root, "processing_kind"),
                root.has("recipe_type") ? GsonHelper.getAsString(root, "recipe_type") : "",
                GsonHelper.getAsString(root, "automation_profile"),
                slotRoles,
                readStrings(root, "physical_ports")
        );
    }

    private static List<String> readStrings(JsonObject root, String key) {
        JsonArray values = GsonHelper.getAsJsonArray(root, key);
        List<String> result = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            String text = value.getAsString();
            if (text.isBlank()) {
                throw new IllegalArgumentException("Blank " + key + " entry");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    private static List<List<String>> readLayers(ResourceLocation file, JsonArray layerElements) {
        if (layerElements.isEmpty()) {
            throw new IllegalArgumentException("Guide has no layers: " + file);
        }
        List<List<String>> layers = new ArrayList<>(layerElements.size());
        for (JsonElement layerElement : layerElements) {
            JsonArray rows = layerElement.getAsJsonArray();
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Guide has an empty layer: " + file);
            }
            List<String> layer = new ArrayList<>(rows.size());
            for (JsonElement row : rows) {
                layer.add(row.getAsString());
            }
            layers.add(List.copyOf(layer));
        }
        return List.copyOf(layers);
    }

    private static Map<Character, LegendEntry> readLegend(ResourceLocation file, JsonArray entries) {
        Map<Character, LegendEntry> legend = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String symbolText = GsonHelper.getAsString(entry, "symbol");
            if (symbolText.length() != 1) {
                throw new IllegalArgumentException("Invalid legend symbol in " + file);
            }
            char symbol = symbolText.charAt(0);
            ResourceLocation blockId = entry.has("block")
                    ? parseId(file, GsonHelper.getAsString(entry, "block"))
                    : null;
            Map<String, String> properties = new LinkedHashMap<>();
            if (entry.has("properties") && entry.get("properties").isJsonObject()) {
                for (Map.Entry<String, JsonElement> property : entry.getAsJsonObject("properties").entrySet()) {
                    String value = property.getValue().getAsString();
                    if (property.getKey().isBlank() || value.isBlank()) {
                        throw new IllegalArgumentException("Blank block-state property in " + file);
                    }
                    properties.put(property.getKey(), value);
                }
            }
            LegendEntry previous = legend.put(
                    symbol,
                    new LegendEntry(
                            symbol,
                            GsonHelper.getAsString(entry, "rule"),
                            blockId,
                            properties,
                            entry.has("ignored") && entry.get("ignored").getAsBoolean()
                    )
            );
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate legend symbol " + symbol + " in " + file);
            }
        }
        return Map.copyOf(legend);
    }

    private static PortSummary readPortSummary(JsonObject ports) {
        List<Integer> tankCapacities = new ArrayList<>();
        if (ports.has("fluid_tank_capacities_mb")
                && ports.get("fluid_tank_capacities_mb").isJsonArray()) {
            for (JsonElement element : ports.getAsJsonArray("fluid_tank_capacities_mb")) {
                tankCapacities.add(Math.max(0, element.getAsInt()));
            }
        }
        return new PortSummary(
                nonNegativeInt(ports, "inventory_slots"),
                nonNegativeInt(ports, "bulk_item_capacity"),
                ports.has("electricity") && ports.get("electricity").getAsBoolean(),
                ports.has("heat") && ports.get("heat").getAsBoolean(),
                tankCapacities
        );
    }

    private static int nonNegativeInt(JsonObject object, String key) {
        return object.has(key) ? Math.max(0, object.get(key).getAsInt()) : 0;
    }

    private static void requireSchema(ResourceLocation file, JsonObject root) {
        int schema = GsonHelper.getAsInt(root, "schema_version");
        if (schema != SUPPORTED_SCHEMA) {
            throw new IllegalArgumentException("Unsupported guide schema " + schema + " in " + file);
        }
    }

    private static ResourceLocation parseId(ResourceLocation file, String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid guide id " + id + " in " + file);
        }
        return parsed;
    }

    public record MultiblockGuide(
            ResourceLocation id,
            String translationKey,
            String descriptionKey,
            String recipeType,
            String category,
            List<List<String>> layers,
            Map<Character, LegendEntry> legend,
            boolean supportsMirroring,
            PortSummary ports
    ) {
        public MultiblockGuide {
            layers = List.copyOf(layers);
            legend = Map.copyOf(legend);
        }
    }

    public record PortSummary(
            int inventorySlots,
            int bulkItemCapacity,
            boolean electricity,
            boolean heat,
            List<Integer> fluidTankCapacitiesMb
    ) {
        private static final PortSummary EMPTY = new PortSummary(0, 0, false, false, List.of());

        public PortSummary {
            inventorySlots = Math.max(0, inventorySlots);
            bulkItemCapacity = Math.max(0, bulkItemCapacity);
            fluidTankCapacitiesMb = List.copyOf(fluidTankCapacitiesMb);
        }
    }

    public record LegendEntry(
            char symbol,
            String rule,
            ResourceLocation blockId,
            Map<String, String> properties,
            boolean ignored
    ) {
        public LegendEntry {
            properties = Map.copyOf(properties);
        }
    }

    public record OpcodeGuide(String id, int code, int operandCount, String descriptionKey) {
    }

    public record ComputerLanguageGuide(
            String id,
            String historicalVersion,
            List<String> examples,
            List<String> commands,
            ComputerLimits limits,
            ComputerSecurity security
    ) {
        public ComputerLanguageGuide {
            examples = List.copyOf(examples);
            commands = List.copyOf(commands);
        }
    }

    public record ComputerLimits(
            int sourceBytes,
            int outputCharacters,
            int instructionsPerTick,
            int deviceCallsPerTick,
            int floppyBytes,
            int floppyEntries,
            int quarryMaxSize
    ) {
    }

    public record ComputerSecurity(
            boolean serverAuthoritative,
            boolean menuSessionReplayProtection,
            boolean hostFilesystemAccess,
            boolean outboundNetworkAccess,
            boolean forceLoadChunks
    ) {
    }

    public record ItemGuide(
            ResourceLocation id,
            String translationKey,
            String descriptionKey,
            int capacityFe,
            int breakCostFe,
            int attackCostFe,
            int useCostFe
    ) {
        public ItemGuide {
            capacityFe = Math.max(0, capacityFe);
            breakCostFe = Math.max(0, breakCostFe);
            attackCostFe = Math.max(0, attackCostFe);
            useCostFe = Math.max(0, useCostFe);
        }
    }

    public record MachineGuide(
            ResourceLocation id,
            String translationKey,
            String descriptionKey,
            String category,
            int inventorySlots,
            int ghostSlots,
            boolean hasMenu,
            String redstoneControl,
            String processingKind,
            String recipeType,
            String automationProfile,
            List<String> slotRoles,
            List<String> physicalPorts
    ) {
        public MachineGuide {
            inventorySlots = Math.max(0, inventorySlots);
            ghostSlots = Math.max(0, ghostSlots);
            slotRoles = List.copyOf(slotRoles);
            physicalPorts = List.copyOf(physicalPorts);
        }
    }

    protected record PreparedGuideData(
            List<MultiblockGuide> multiblocks,
            List<OpcodeGuide> opcodes,
            List<ComputerLanguageGuide> languages,
            List<ItemGuide> items,
            List<MachineGuide> machines
    ) {
        private static final PreparedGuideData EMPTY = new PreparedGuideData(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        protected PreparedGuideData {
            multiblocks = List.copyOf(multiblocks);
            opcodes = List.copyOf(opcodes);
            languages = List.copyOf(languages);
            items = List.copyOf(items);
            machines = List.copyOf(machines);
        }
    }
}
