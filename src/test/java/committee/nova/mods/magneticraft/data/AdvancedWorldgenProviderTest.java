package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedWorldgenProviderTest {
    private static final Set<String> EXPECTED_IDS = Set.of(
            "galena_ore",
            "cobalt_ore",
            "tungsten_ore",
            "pyrite_ore",
            "zinc_ore",
            "bauxite_ore",
            "silver_ore",
            "nickel_ore",
            "tin_ore",
            "limestone",
            "oil_deposit"
    );

    private static final Map<String, CountedOreContract> COUNTED_ORES = Map.ofEntries(
            Map.entry("galena_ore", new CountedOreContract("magneticraft:galena_ore", 8, 10, 2, 80)),
            Map.entry("cobalt_ore", new CountedOreContract("magneticraft:cobalt_ore", 6, 4, -32, 33)),
            Map.entry("tungsten_ore", new CountedOreContract("magneticraft:tungsten_ore", 8, 8, 20, 60)),
            Map.entry("pyrite_ore", new CountedOreContract("magneticraft:pyrite_ore", 9, 9, 30, 100)),
            Map.entry("zinc_ore", new CountedOreContract("magneticraft:zinc_ore", 7, 6, 0, 65)),
            Map.entry("bauxite_ore", new CountedOreContract("magneticraft:bauxite_ore", 7, 5, 32, 113)),
            Map.entry("nickel_ore", new CountedOreContract("magneticraft:nickel_ore", 5, 4, -24, 41)),
            Map.entry("tin_ore", new CountedOreContract("magneticraft:tin_ore", 6, 4, -16, 49))
    );

    @Test
    void catalogueContainsEveryReleasedDepositExactlyOnce() {
        Set<String> ids = new HashSet<>();
        for (AdvancedWorldgenProvider.DepositDefinition deposit : AdvancedWorldgenProvider.deposits()) {
            assertTrue(ids.add(deposit.id()), "Duplicate deposit ID " + deposit.id());
        }

        assertEquals(EXPECTED_IDS, ids);
    }

    @Test
    void silverUsesOneInTwoChunkRarityPlacement() {
        AdvancedWorldgenProvider.DepositDefinition silver = deposit("silver_ore");
        assertEquals(AdvancedWorldgenProvider.FrequencyType.RARITY, silver.frequencyType());
        assertEquals(2, silver.frequency());
        assertEquals(-48, silver.minY());
        assertEquals(16, silver.maxY());

        JsonArray placement = AdvancedWorldgenProvider.placedFeature(silver).getAsJsonArray("placement");
        assertEquals("minecraft:rarity_filter", placement.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(2, placement.get(0).getAsJsonObject().get("chance").getAsInt());
        assertUniformHeight(placement.get(2).getAsJsonObject(), -48, 16);
    }

    @Test
    void countedOresTranslateLegacyExclusiveUpperBoundsExactlyOnce() {
        COUNTED_ORES.forEach((id, expected) -> {
            AdvancedWorldgenProvider.DepositDefinition deposit = deposit(id);
            assertEquals(expected.block(), deposit.block());
            assertEquals(expected.veinSize(), deposit.veinSize());
            assertEquals(AdvancedWorldgenProvider.FrequencyType.COUNT, deposit.frequencyType());
            assertEquals(expected.count(), deposit.frequency());
            assertEquals(expected.minYInclusive(), deposit.minY());
            assertEquals(expected.maxYExclusive() - 1, deposit.maxY(),
                    () -> id + " must convert the legacy exclusive upper bound to an inclusive bound");

            JsonObject configured = AdvancedWorldgenProvider.configuredFeature(deposit);
            assertOreConfiguration(configured, expected.block(), expected.veinSize());

            JsonArray placement = AdvancedWorldgenProvider.placedFeature(deposit)
                    .getAsJsonArray("placement");
            assertEquals(4, placement.size());
            JsonObject frequency = placement.get(0).getAsJsonObject();
            assertEquals("minecraft:count", frequency.get("type").getAsString());
            assertEquals(expected.count(), frequency.get("count").getAsInt());
            assertEquals("minecraft:in_square", placement.get(1).getAsJsonObject().get("type").getAsString());
            assertUniformHeight(
                    placement.get(2).getAsJsonObject(),
                    expected.minYInclusive(),
                    expected.maxYExclusive() - 1
            );
            assertEquals("minecraft:biome", placement.get(3).getAsJsonObject().get("type").getAsString());
        });
    }

    @Test
    void limestoneUsesLegacyClampedNormalFrequency() {
        AdvancedWorldgenProvider.DepositDefinition limestone = deposit("limestone");
        assertEquals("magneticraft:limestone", limestone.block());
        assertEquals(32, limestone.veinSize());
        assertEquals(AdvancedWorldgenProvider.FrequencyType.CLAMPED_NORMAL, limestone.frequencyType());
        assertEquals(3, limestone.frequency());
        assertEquals(0.9D, limestone.deviation());
        assertEquals(0, limestone.minCount());
        assertEquals(5, limestone.maxCount());
        assertEquals(16, limestone.minY());
        assertEquals(63, limestone.maxY());
        assertOreConfiguration(
                AdvancedWorldgenProvider.configuredFeature(limestone),
                "magneticraft:limestone",
                32
        );

        JsonArray placement = AdvancedWorldgenProvider.placedFeature(limestone)
                .getAsJsonArray("placement");
        assertEquals(4, placement.size());
        JsonObject frequency = placement.get(0).getAsJsonObject();
        assertEquals("minecraft:count", frequency.get("type").getAsString());
        JsonObject count = frequency.getAsJsonObject("count");
        assertEquals("minecraft:clamped_normal", count.get("type").getAsString());
        JsonObject value = count.getAsJsonObject("value");
        assertEquals(3, value.get("mean").getAsInt());
        assertEquals(0.9D, value.get("deviation").getAsDouble());
        assertEquals(0, value.get("min_inclusive").getAsInt());
        assertEquals(5, value.get("max_inclusive").getAsInt());
        assertUniformHeight(placement.get(2).getAsJsonObject(), 16, 63);
    }

    @Test
    void oilUsesCustomSectorFeatureWithoutGenericOrePlacement() {
        AdvancedWorldgenProvider.DepositDefinition oil = deposit("oil_deposit");
        assertEquals("magneticraft:oil_deposit", oil.block());
        assertEquals(AdvancedWorldgenProvider.FrequencyType.SECTOR, oil.frequencyType());

        JsonObject configured = AdvancedWorldgenProvider.configuredFeature(oil);
        assertEquals("magneticraft:oil_field", configured.get("type").getAsString());
        assertTrue(configured.getAsJsonObject("config").entrySet().isEmpty());

        JsonObject placed = AdvancedWorldgenProvider.placedFeature(oil);
        assertEquals("magneticraft:oil_deposit", placed.get("feature").getAsString());
        JsonArray placement = placed.getAsJsonArray("placement");
        assertEquals(1, placement.size());
        assertEquals("minecraft:biome", placement.get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void everyDepositHasAnOverworldUndergroundOreBiomeModifier() {
        for (AdvancedWorldgenProvider.DepositDefinition deposit : AdvancedWorldgenProvider.deposits()) {
            JsonObject modifier = AdvancedWorldgenProvider.biomeModifier(deposit);
            assertEquals("forge:add_features", modifier.get("type").getAsString());
            assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
            assertEquals("magneticraft:" + deposit.id(), modifier.get("features").getAsString());
            assertEquals("underground_ores", modifier.get("step").getAsString());
        }
    }

    private static AdvancedWorldgenProvider.DepositDefinition deposit(String id) {
        return AdvancedWorldgenProvider.deposits().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void assertOreConfiguration(JsonObject configured, String block, int veinSize) {
        assertEquals("minecraft:ore", configured.get("type").getAsString());
        JsonObject config = configured.getAsJsonObject("config");
        assertEquals(veinSize, config.get("size").getAsInt());
        assertEquals(0.0D, config.get("discard_chance_on_air_exposure").getAsDouble());

        JsonArray targets = config.getAsJsonArray("targets");
        assertEquals(2, targets.size());
        assertEquals(
                Set.of("minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"),
                Set.of(targetTag(targets.get(0).getAsJsonObject()), targetTag(targets.get(1).getAsJsonObject()))
        );
        targets.forEach(target -> assertEquals(
                block,
                target.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString()
        ));
    }

    private static void assertUniformHeight(JsonObject heightRange, int minInclusive, int maxInclusive) {
        assertEquals("minecraft:height_range", heightRange.get("type").getAsString());
        JsonObject height = heightRange.getAsJsonObject("height");
        assertEquals("minecraft:uniform", height.get("type").getAsString());
        assertEquals(minInclusive, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
        assertEquals(maxInclusive, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
    }

    private static String targetTag(JsonObject target) {
        return target.getAsJsonObject("target").get("tag").getAsString();
    }

    private record CountedOreContract(
            String block,
            int veinSize,
            int count,
            int minYInclusive,
            int maxYExclusive
    ) {
    }
}
