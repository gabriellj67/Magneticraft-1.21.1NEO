package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedWorldgenProviderTest {
    private static final Set<String> EXPECTED_IDS = Set.of(
            "galena_ore",
            "cobalt_ore",
            "tungsten_ore",
            "pyrite_ore",
            "limestone",
            "oil_deposit"
    );

    @Test
    void catalogueGeneratesOneConfiguredPlacedAndOverworldModifierPerDeposit() {
        Set<String> ids = new HashSet<>();
        for (AdvancedWorldgenProvider.DepositDefinition deposit : AdvancedWorldgenProvider.deposits()) {
            assertTrue(ids.add(deposit.id()), "Duplicate deposit ID " + deposit.id());

            JsonObject configured = AdvancedWorldgenProvider.configuredFeature(deposit);
            assertEquals("minecraft:ore", configured.get("type").getAsString());
            JsonObject config = configured.getAsJsonObject("config");
            assertEquals(deposit.veinSize(), config.get("size").getAsInt());
            JsonArray targets = config.getAsJsonArray("targets");
            assertEquals(2, targets.size());
            assertEquals(
                    Set.of("minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"),
                    Set.of(targetTag(targets.get(0).getAsJsonObject()), targetTag(targets.get(1).getAsJsonObject()))
            );
            targets.forEach(target -> assertEquals(
                    deposit.block(),
                    target.getAsJsonObject().getAsJsonObject("state").get("Name").getAsString()
            ));

            JsonObject placed = AdvancedWorldgenProvider.placedFeature(deposit);
            assertEquals("magneticraft:" + deposit.id(), placed.get("feature").getAsString());
            JsonArray placement = placed.getAsJsonArray("placement");
            assertEquals(4, placement.size());
            assertEquals("minecraft:in_square", placement.get(1).getAsJsonObject().get("type").getAsString());
            assertEquals("minecraft:height_range", placement.get(2).getAsJsonObject().get("type").getAsString());
            assertEquals("minecraft:biome", placement.get(3).getAsJsonObject().get("type").getAsString());

            JsonObject modifier = AdvancedWorldgenProvider.biomeModifier(deposit);
            assertEquals("forge:add_features", modifier.get("type").getAsString());
            assertEquals("#minecraft:is_overworld", modifier.get("biomes").getAsString());
            assertEquals("magneticraft:" + deposit.id(), modifier.get("features").getAsString());
            assertEquals("underground_ores", modifier.get("step").getAsString());
        }
        assertEquals(EXPECTED_IDS, ids);
    }

    @Test
    void oilUsesBoundedLegacyRarityInsteadOfPerChunkCount() {
        AdvancedWorldgenProvider.DepositDefinition oil = AdvancedWorldgenProvider.deposits().stream()
                .filter(deposit -> deposit.id().equals("oil_deposit"))
                .findFirst()
                .orElseThrow();

        JsonObject frequency = AdvancedWorldgenProvider.placedFeature(oil)
                .getAsJsonArray("placement")
                .get(0)
                .getAsJsonObject();
        assertEquals("minecraft:rarity_filter", frequency.get("type").getAsString());
        assertEquals(50, frequency.get("chance").getAsInt());
    }

    private static String targetTag(JsonObject target) {
        return target.getAsJsonObject("target").get("tag").getAsString();
    }
}
