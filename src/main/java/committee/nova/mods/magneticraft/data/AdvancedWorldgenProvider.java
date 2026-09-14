package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.OreBlockDefinition;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates the released overworld deposits without cross-chunk writes. */
final class AdvancedWorldgenProvider implements DataProvider {
    private static final String OVERWORLD_BIOME_TAG = "#minecraft:is_overworld";
    private static final String STONE_REPLACEABLES = "minecraft:stone_ore_replaceables";
    private static final String DEEPSLATE_REPLACEABLES = "minecraft:deepslate_ore_replaceables";

    private static final List<DepositDefinition> DEPOSITS = depositsWithOres();

    private final PackOutput.PathProvider configuredFeatures;
    private final PackOutput.PathProvider placedFeatures;
    private final PackOutput.PathProvider biomeModifiers;

    AdvancedWorldgenProvider(PackOutput output) {
        configuredFeatures = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "worldgen/configured_feature"
        );
        placedFeatures = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "worldgen/placed_feature"
        );
        biomeModifiers = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "neoforge/biome_modifier"
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>(DEPOSITS.size() * 3);
        for (DepositDefinition deposit : DEPOSITS) {
            ResourceLocation id = Magneticraft.id(deposit.id());
            writes.add(save(output, configuredFeature(deposit), configuredFeatures.json(id)));
            writes.add(save(output, placedFeature(deposit), placedFeatures.json(id)));
            writes.add(save(output, biomeModifier(deposit), biomeModifiers.json(id)));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Magneticraft advanced world generation";
    }

    static List<DepositDefinition> deposits() {
        return DEPOSITS;
    }

    static JsonObject configuredFeature(DepositDefinition deposit) {
        JsonObject root = new JsonObject();
        if (deposit.frequencyType() == FrequencyType.SECTOR) {
            root.addProperty("type", "magneticraft:oil_field");
            root.add("config", new JsonObject());
            return root;
        }
        root.addProperty("type", "minecraft:ore");

        JsonObject config = new JsonObject();
        config.addProperty("size", deposit.veinSize());
        config.addProperty("discard_chance_on_air_exposure", 0.0F);

        JsonArray targets = new JsonArray();
        targets.add(oreTarget(STONE_REPLACEABLES, deposit.block()));
        targets.add(oreTarget(DEEPSLATE_REPLACEABLES, deposit.block()));
        config.add("targets", targets);
        root.add("config", config);
        return root;
    }

    static JsonObject placedFeature(DepositDefinition deposit) {
        JsonObject root = new JsonObject();
        root.addProperty("feature", Magneticraft.MOD_ID + ":" + deposit.id());

        JsonArray placement = new JsonArray();
        if (deposit.frequencyType() == FrequencyType.SECTOR) {
            placement.add(typeOnly("minecraft:biome"));
            root.add("placement", placement);
            return root;
        }

        JsonObject frequency = new JsonObject();
        if (deposit.frequencyType() == FrequencyType.RARITY) {
            frequency.addProperty("type", "minecraft:rarity_filter");
            frequency.addProperty("chance", deposit.frequency());
        } else {
            frequency.addProperty("type", "minecraft:count");
        }
        if (deposit.frequencyType() == FrequencyType.CLAMPED_NORMAL) {
            JsonObject count = new JsonObject();
            count.addProperty("type", "minecraft:clamped_normal");
            count.addProperty("mean", deposit.frequency());
            count.addProperty("deviation", deposit.deviation());
            count.addProperty("min_inclusive", deposit.minCount());
            count.addProperty("max_inclusive", deposit.maxCount());
            frequency.add("count", count);
        } else {
            frequency.addProperty("count", deposit.frequency());
        }
        placement.add(frequency);
        placement.add(typeOnly("minecraft:in_square"));

        JsonObject heightRange = new JsonObject();
        heightRange.addProperty("type", "minecraft:height_range");
        JsonObject height = new JsonObject();
        height.addProperty("type", "minecraft:uniform");
        height.add("min_inclusive", absoluteHeight(deposit.minY()));
        height.add("max_inclusive", absoluteHeight(deposit.maxY()));
        heightRange.add("height", height);
        placement.add(heightRange);
        placement.add(typeOnly("minecraft:biome"));
        root.add("placement", placement);
        return root;
    }

    static JsonObject biomeModifier(DepositDefinition deposit) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "neoforge:add_features");
        root.addProperty("biomes", OVERWORLD_BIOME_TAG);
        root.addProperty("features", Magneticraft.MOD_ID + ":" + deposit.id());
        root.addProperty("step", "underground_ores");
        return root;
    }

    private static JsonObject oreTarget(String replaceableTag, String block) {
        JsonObject target = new JsonObject();
        JsonObject predicate = new JsonObject();
        predicate.addProperty("predicate_type", "minecraft:tag_match");
        predicate.addProperty("tag", replaceableTag);
        target.add("target", predicate);

        JsonObject state = new JsonObject();
        state.addProperty("Name", block);
        target.add("state", state);
        return target;
    }

    private static JsonObject absoluteHeight(int value) {
        JsonObject height = new JsonObject();
        height.addProperty("absolute", value);
        return height;
    }

    private static JsonObject typeOnly(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        return object;
    }

    private static CompletableFuture<?> save(CachedOutput output, JsonObject json, Path path) {
        return DataProvider.saveStable(output, json, path);
    }

    private static List<DepositDefinition> depositsWithOres() {
        List<DepositDefinition> deposits = new ArrayList<>();
        Arrays.stream(OreBlockDefinition.values()).map(DepositDefinition::ore).forEach(deposits::add);
        deposits.add(DepositDefinition.gaussian(
                "limestone", "magneticraft:limestone", 32, 3, 0.9D, 0, 5, 16, 63));
        deposits.add(DepositDefinition.sector("oil_deposit", "magneticraft:oil_deposit"));
        return List.copyOf(deposits);
    }

    enum FrequencyType {
        COUNT,
        RARITY,
        CLAMPED_NORMAL,
        SECTOR
    }

    record DepositDefinition(
            String id,
            String block,
            int veinSize,
            FrequencyType frequencyType,
            int frequency,
            double deviation,
            int minCount,
            int maxCount,
            int minY,
            int maxY
    ) {
        DepositDefinition {
            if (id.isBlank() || block.isBlank()) {
                throw new IllegalArgumentException("Deposit IDs must not be blank");
            }
            if (frequencyType != FrequencyType.SECTOR
                    && (veinSize <= 0 || frequency <= 0 || minY > maxY)) {
                throw new IllegalArgumentException("Invalid deposit bounds for " + id);
            }
            if (frequencyType == FrequencyType.CLAMPED_NORMAL
                    && (!(deviation > 0.0D) || minCount < 0 || minCount > maxCount)) {
                throw new IllegalArgumentException("Invalid Gaussian deposit bounds for " + id);
            }
        }

        static DepositDefinition counted(
                String id,
                String block,
                int veinSize,
                int count,
                int minY,
                int maxY
        ) {
            return new DepositDefinition(
                    id, block, veinSize, FrequencyType.COUNT, count, 0.0D, count, count, minY, maxY);
        }

        static DepositDefinition rare(
                String id,
                String block,
                int veinSize,
                int chance,
                int minY,
                int maxY
        ) {
            return new DepositDefinition(
                    id, block, veinSize, FrequencyType.RARITY, chance, 0.0D, 0, 0, minY, maxY);
        }

        static DepositDefinition ore(OreBlockDefinition ore) {
            String id = ore.block().id();
            String block = Magneticraft.MOD_ID + ":" + id;
            if (ore.rarity() > 0) {
                return rare(id, block, ore.veinSize(), ore.rarity(), ore.minY(), ore.maxY());
            }
            return counted(id, block, ore.veinSize(), ore.count(), ore.minY(), ore.maxY());
        }

        static DepositDefinition gaussian(
                String id,
                String block,
                int veinSize,
                int mean,
                double deviation,
                int minCount,
                int maxCount,
                int minY,
                int maxY
        ) {
            return new DepositDefinition(
                    id, block, veinSize, FrequencyType.CLAMPED_NORMAL,
                    mean, deviation, minCount, maxCount, minY, maxY);
        }

        static DepositDefinition sector(String id, String block) {
            return new DepositDefinition(id, block, 0, FrequencyType.SECTOR, 0, 0.0D, 0, 0, 0, 0);
        }
    }
}
