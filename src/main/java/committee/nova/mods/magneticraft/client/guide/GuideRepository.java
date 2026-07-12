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

    private volatile PreparedGuideData data = PreparedGuideData.EMPTY;

    private GuideRepository() {
    }

    public List<MultiblockGuide> multiblocks() {
        return data.multiblocks();
    }

    public List<OpcodeGuide> opcodes() {
        return data.opcodes();
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
        return new PreparedGuideData(multiblocks, opcodes);
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
            LegendEntry previous = legend.put(
                    symbol,
                    new LegendEntry(symbol, GsonHelper.getAsString(entry, "rule"))
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

    public record LegendEntry(char symbol, String rule) {
    }

    public record OpcodeGuide(String id, int code, int operandCount, String descriptionKey) {
    }

    protected record PreparedGuideData(List<MultiblockGuide> multiblocks, List<OpcodeGuide> opcodes) {
        private static final PreparedGuideData EMPTY = new PreparedGuideData(List.of(), List.of());

        protected PreparedGuideData {
            multiblocks = List.copyOf(multiblocks);
            opcodes = List.copyOf(opcodes);
        }
    }
}
