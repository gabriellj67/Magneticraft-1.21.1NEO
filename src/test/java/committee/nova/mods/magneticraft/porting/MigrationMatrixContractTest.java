package committee.nova.mods.magneticraft.porting;

import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationMatrixContractTest {
    private static final String CURRENT_COMPLETED_STAGE = "1.0.0";
    private static final Path MATRIX_PATH = Path.of("docs/porting/migration-matrix.json");
    private static final Path ID_MAP_PATH = Path.of("docs/porting/registry-id-map.json");
    private static final Path LEGACY_RECIPE_ROOT = Path.of(
            ".references/nova-1.12/src/main/resources/assets/magneticraft/recipes"
    );
    private static final Path LEGACY_GUIDE_ROOT = Path.of(
            ".references/nova-1.12/src/main/resources/assets/magneticraft/guide/en_us"
    );
    private static final Path BASE_CONTENT_GAMETEST = Path.of(
            "src/gametest/java/committee/nova/mods/magneticraft/gametest/BaseContentGameTests.java"
    );
    private static final String LEGACY_RECIPE_MANIFEST_SHA256 =
            "368528dacda59382b4db906b2a4e9eb05b75871de9d342b92607498c729ab012";
    private static final String LEGACY_GUIDE_MANIFEST_SHA256 =
            "87242cee863635855a905c21278ba0c87e9d74a0e314f125bdaa48b51b96827a";
    private static final String LEGACY_RECIPE_CONTENT_MANIFEST_SHA256 =
            "22169dbd3f31e7e6d61d9bd1bab09ae048d1dcd762c9a8c58620c82b9f3c1813";
    private static final String LEGACY_GUIDE_CONTENT_MANIFEST_SHA256 =
            "628c16fb7164cfdcca98a4122b03f144dcd0db2517bd785be9b72c63eff2d9f8";
    private static final int ACCEPTANCE_CASE_COUNT = 137;
    private static final String ACCEPTANCE_CASE_SHA256 =
            "5fc8b5ab5d6417f877579554c192ba5b29239f1aa30fa2544419b49de182e70d";
    private static final int MAPPING_LEAF_COUNT = 147;
    private static final String MAPPING_LEAF_SHA256 =
            "5068ab14ee3f8324e7dc947b58ce7b24e114e8a59882289df31a5df67fa7d6e8";
    private static final String MAPPING_PROJECTION_SHA256 =
            "40b8ea11c0276feb76c9efa01aee5f629b892d097b1a4c3f39d8678145bbc3f4";
    private static final int MATERIAL_LEAF_COUNT = 77;
    private static final String MATERIAL_LEAF_SHA256 =
            "784b0c5ff88ddf7e6e28216ebe71ae5248c8f0d60c75b2f8dd099f9eb674409e";
    private static final String MATERIAL_PROJECTION_SHA256 =
            "3188a163d2c96ec7fd7fabce09597e5a9d0f165703d209ecf4389d1d8dedfe36";
    private static final int STATE_LEAF_COUNT = 95;
    private static final String STATE_LEAF_SHA256 =
            "ee263416076db2e98ecaacb709eb9c1e944588591f80c59713f2aad31da97928";
    private static final String STATE_PROJECTION_SHA256 =
            "230b94c106210b9481ab634af96321f626b7d147bd3466251f906d38655d2879";
    private static final int EXCLUSION_LEAF_COUNT = 16;
    private static final String EXCLUSION_LEAF_SHA256 =
            "f6beb52866ea02a788d7c1ba4a2e88ecbac78bcb54c7552183363b8505a19dac";
    private static final int DERIVED_RUNTIME_ID_COUNT = 80;
    private static final String DERIVED_RUNTIME_ID_SHA256 =
            "157535436f7f87ae76724cb72156db40b9c2196f8702019790fb052a95e13451";
    private static final int SUPPORTING_REGISTRY_ID_COUNT = 40;
    private static final String SUPPORTING_REGISTRY_ID_SHA256 =
            "fa95e8fc79d6c129e558360938347eeaa3c8389d40a948b17ffaa55a270f1e38";
    private static final int FORBIDDEN_RUNTIME_ID_COUNT = 51;
    private static final String FORBIDDEN_RUNTIME_ID_SHA256 =
            "2dd45be9e2de079ee8c833139c481e9127269b554e4208bb57962f64aecddb11";
    private static final Set<String> DECISIONS = Set.of("retain_rename", "rebuild", "exclude");
    private static final Set<String> STATE_ID_MODES = Set.of(
            "fixed_owner", "internal_owner", "owning_content", "flattened_variant", "flattened_variant_per_owner"
    );
    private static final Set<String> FORBIDDEN_ID_TOKENS = Set.of("item", "big", "rf");
    private static final Set<String> CROSS_STAGE_STATE_ENUMS = Set.of(
            "Facing", "Orientation", "CenterOrientation"
    );
    private static final Pattern RESOURCE_LOCATION = Pattern.compile(
            "^[a-z0-9_.-]+:([a-z0-9]+(?:_[a-z0-9]+)*)$"
    );
    private static final Pattern GUIDE_PAGE_ID = Pattern.compile(
            "^[a-z0-9_.-]+:[a-z0-9]+(?:_[a-z0-9]+)*(?:/[a-z0-9]+(?:_[a-z0-9]+)*)*$"
    );
    private static final Pattern FROZEN_PATH = Pattern.compile("^[a-z0-9]+(?:_[a-z0-9]+)*$");
    private static final Pattern BLOCK_ENTITY_REGISTRATION = Pattern.compile(
            "BLOCK_ENTITY_TYPES\\.register\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern MENU_REGISTRATION = Pattern.compile(
            "MENU_TYPES\\.register\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern RECIPE_TYPE_REGISTRATION = Pattern.compile(
            "RECIPE_TYPES\\.register\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern RECIPE_TYPE_HELPER = Pattern.compile("\\btype\\(\"([a-z0-9_]+)\"\\)");
    private static final Pattern RECIPE_SERIALIZER_REGISTRATION = Pattern.compile(
            "RECIPE_SERIALIZERS\\.register\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern SOUND_REGISTRATION = Pattern.compile("\\bregister\\(\"([a-z0-9_]+)\"\\)");
    private static final Pattern FLUID_DEFINITION = Pattern.compile(
            "(?m)^\\s*[A-Z][A-Z0-9_]*\\(\"([a-z0-9_]+)\""
    );
    private static final Pattern BLOCK_LITERAL_REGISTRATION = Pattern.compile(
            "(?:(?<!\\.)\\bregister(?:Controller)?|ModRegistries\\.BLOCKS\\.register)\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern ITEM_LITERAL_REGISTRATION = Pattern.compile(
            "ModRegistries\\.ITEMS\\.register\\(\\s*\"([a-z0-9_]+)\""
    );
    private static final Pattern DATA_RECIPE_PATH = Pattern.compile(
            "^data/[a-z0-9_.-]+/recipes/[a-z0-9_./-]+\\.json$"
    );
    private static final Pattern ACCEPTANCE_CASE_ID = Pattern.compile(
            "^MIG-[A-Z0-9]+(?:-[A-Z0-9]+)*-[0-9]{3}$"
    );
    private static final Pattern FORBIDDEN_GAMETEST_SET = Pattern.compile(
            "FORBIDDEN_LEGACY_IDS\\s*=\\s*Set\\.of\\((.*?)\\);",
            Pattern.DOTALL
    );
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([a-z0-9_]+)\"");
    private static final List<String> QUALITY_GATE = List.of(
            "compileJava",
            "runData twice with no second-run resource diff",
            "test",
            "runGameTestServer",
            "build",
            "runServer smoke",
            "runClient smoke"
    );
    private static final Map<String, Set<String>> SUPPORTING_RUNTIME_IDS = Map.of(
            "block", Set.of("magneticraft:pumpjack_drill"),
            "block_entity_type", Set.of(
                    "magneticraft:crushing_table",
                    "magneticraft:battery_box",
                    "magneticraft:electric_furnace",
                    "magneticraft:electric_cable",
                    "magneticraft:electric_connector",
                    "magneticraft:electric_pole",
                    "magneticraft:electric_pole_transformer",
                    "magneticraft:tesla_tower",
                    "magneticraft:wireless_energy_receiver",
                    "magneticraft:wind_turbine",
                    "magneticraft:heat_pipe",
                    "magneticraft:heat_sink",
                    "magneticraft:iron_fluid_pipe",
                    "magneticraft:pneumatic_tube",
                    "magneticraft:conveyor_belt",
                    "magneticraft:single_block_machine",
                    "magneticraft:advanced_multiblock",
                    "magneticraft:oil_deposit",
                    "magneticraft:computer",
                    "magneticraft:mining_robot"
            ),
            "menu", Set.of(
                    "magneticraft:battery_box",
                    "magneticraft:electric_furnace",
                    "magneticraft:single_block_machine",
                    "magneticraft:programmable",
                    "magneticraft:advanced_multiblock"
            ),
            "recipe_type", Set.of(
                    "magneticraft:crushing_table",
                    "magneticraft:sluice_box",
                    "magneticraft:gasification_unit",
                    "magneticraft:thermopile",
                    "magneticraft:fluid_fuel",
                    "magneticraft:advanced_processing"
            ),
            "recipe_serializer", Set.of(
                    "magneticraft:crushing_table",
                    "magneticraft:sluice_box",
                    "magneticraft:gasification_unit",
                    "magneticraft:thermopile",
                    "magneticraft:fluid_fuel",
                    "magneticraft:advanced_processing"
            ),
            "sound_event", Set.of(
                    "magneticraft:crushing_table_hit",
                    "magneticraft:crushing_table_complete"
            )
    );
    private static final Map<String, List<String>> LEGACY_STATE_ENUM_VALUES = Map.ofEntries(
            Map.entry("Facing", List.of("down", "up", "north", "south", "east", "west")),
            Map.entry("Orientation", List.of("north", "south", "east", "west")),
            Map.entry("CenterOrientation", List.of(
                    "center_north", "center_south", "center_west", "center_east",
                    "no_center_north", "no_center_south", "no_center_west", "no_center_east"
            )),
            Map.entry("OrientationActive", List.of(
                    "off_north", "off_south", "off_west", "off_east",
                    "on_north", "on_south", "on_west", "on_east"
            )),
            Map.entry("OilAmount", List.of(
                    "full_100", "full_90", "full_80", "full_70", "full_60", "full_50",
                    "full_40", "full_30", "full_20", "full_10", "empty"
            )),
            Map.entry("OreType", List.of("copper", "lead", "cobalt", "tungsten", "pyrite")),
            Map.entry("PartType", List.of("base", "electric", "grate", "striped", "copper_coil", "corrugated_iron")),
            Map.entry("ColumnOrientation", List.of("axis_y", "axis_x", "axis_z")),
            Map.entry("LimestoneKind", List.of("normal", "brick", "cobble")),
            Map.entry("TileInverted", List.of("normal", "inverted")),
            Map.entry("MultiblockOrientation", List.of(
                    "inactive_north", "inactive_south", "inactive_west", "inactive_east",
                    "active_north", "active_south", "active_west", "active_east"
            )),
            Map.entry("WorkingMode", List.of("off", "on")),
            Map.entry("DecayMode", List.of("off", "on")),
            Map.entry("TeslaTowerPart", List.of("bottom", "middle", "top")),
            Map.entry("PoleOrientation", List.of(
                    "north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west",
                    "down_1", "down_2", "down_3", "down_4"
            )),
            Map.entry("RobotOrientation", List.of(
                    "north", "east", "south", "west",
                    "up_north", "up_east", "up_south", "up_west",
                    "down_north", "down_east", "down_south", "down_west"
            ))
    );

    @Test
    void authorityAndBreakingBoundaryArePinned() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        JsonObject idMap = readObject(ID_MAP_PATH);

        assertEquals(1, matrix.get("schema_version").getAsInt());
        assertEquals(1, idMap.get("schema_version").getAsInt());

        JsonObject authority = matrix.getAsJsonObject("authority");
        assertEquals("4108ca9bb332d11965c30e0c592b310d0858f251",
                authority.get("legacy_commit").getAsString());
        assertEquals("1.20.1", authority.get("target_minecraft").getAsString());
        assertEquals("47.4.20", authority.get("target_forge").getAsString());
        assertEquals(17, authority.get("target_java").getAsInt());
        assertEquals(137, authority.get("legacy_json_recipe_manifest_count").getAsInt());
        assertEquals(52, authority.get("legacy_english_guide_manifest_count").getAsInt());

        JsonObject compatibility = matrix.getAsJsonObject("compatibility");
        assertEquals("0.2.0", compatibility.get("breaking_reset_release").getAsString());
        assertEquals("0.2.0", compatibility.get("id_and_persistence_contract_frozen_from").getAsString());
        assertFalse(compatibility.get("legacy_1_12_world_import").getAsBoolean());
        assertFalse(compatibility.get("pre_0_2_0_worlds_supported").getAsBoolean());
        assertFalse(compatibility.get("pre_0_2_0_items_supported").getAsBoolean());
        assertEquals(Set.of("en_us", "zh_cn"), new HashSet<>(strings(compatibility.getAsJsonArray("required_languages"))));

        String raw = Files.readString(MATRIX_PATH).toLowerCase(Locale.ROOT)
                + Files.readString(ID_MAP_PATH).toLowerCase(Locale.ROOT);
        for (String placeholder : List.of("\"tbd\"", "\"unassigned\"", "unknown decision")) {
            assertFalse(raw.contains(placeholder), placeholder);
        }
    }

    @Test
    void legacyLeafAndProjectionManifestsAreLiteralCompleteAndDisjoint() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        JsonObject manifest = idMap.getAsJsonObject("legacy_leaf_manifest");
        assertEquals("document_order_within_group_then_variant_order",
                manifest.get("ordering").getAsString());

        List<String> mappingLeaves = new ArrayList<>();
        List<String> mappingProjections = new ArrayList<>();
        Set<String> canonicalMappingLeaves = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            String legacyRegistry = mapping.get("legacy_registry").getAsString();
            String legacyId = mapping.get("legacy_id").getAsString();
            String targetRegistry = mapping.get("target_registry").getAsString();
            List<String> legacyVariants = strings(mapping.getAsJsonArray("legacy_variants"));
            List<String> targetIds = strings(mapping.getAsJsonArray("target_ids"));
            List<String> targetVariants = strings(mapping.getAsJsonArray("target_variants"));
            assertEquals(legacyVariants.size(), targetIds.size(), mapping.toString());
            assertEquals(legacyVariants.size(), targetVariants.size(), mapping.toString());
            for (int index = 0; index < legacyVariants.size(); index++) {
                String legacyVariant = legacyVariants.get(index);
                String leaf = legacyRegistry + ":" + legacyId + "#" + legacyVariant;
                mappingLeaves.add(leaf);
                mappingProjections.add(leaf + "->" + targetRegistry + ":"
                        + targetIds.get(index) + "#" + targetVariants.get(index));
                assertTrue(canonicalMappingLeaves.add(canonicalLeaf(legacyRegistry, legacyId, legacyVariant)),
                        "Duplicate mapping leaf: " + leaf);
            }
        }

        List<String> materialLeaves = new ArrayList<>();
        List<String> materialProjections = new ArrayList<>();
        Set<String> canonicalMaterialLeaves = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("material_families")) {
            JsonObject family = element.getAsJsonObject();
            String registry = family.get("legacy_registry").getAsString();
            String legacyId = family.get("legacy_id").getAsString();
            String targetPattern = family.get("target_pattern").getAsString();
            JsonObject overrides = family.getAsJsonObject("overrides");
            for (String variant : strings(family.getAsJsonArray("variants"))) {
                String leaf = registry + ":" + legacyId + "#" + variant;
                String target = overrides.has(variant)
                        ? overrides.get(variant).getAsString()
                        : targetPattern.replace("{material}", variant);
                materialLeaves.add(leaf);
                materialProjections.add(leaf + "->item:" + target);
                assertTrue(canonicalMaterialLeaves.add(canonicalLeaf(registry, legacyId, variant)),
                        "Duplicate material leaf: " + leaf);
            }
        }

        List<String> stateLeaves = new ArrayList<>();
        List<String> stateProjections = new ArrayList<>();
        Set<String> canonicalStateLeaves = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("legacy_state_schemas")) {
            JsonObject schema = element.getAsJsonObject();
            String legacyEnum = schema.get("legacy_enum").getAsString();
            for (String value : strings(schema.getAsJsonArray("legacy_values"))) {
                String leaf = "block_state:" + legacyEnum + "#" + value;
                stateLeaves.add(leaf);
                assertTrue(canonicalStateLeaves.add(canonicalLeaf("block_state", legacyEnum, value)),
                        "Duplicate state leaf: " + leaf);
            }
            stateProjections.add(legacyEnum
                    + "|owners=" + String.join(",", strings(schema.getAsJsonArray("target_ids")))
                    + "|states=" + String.join(",", strings(schema.getAsJsonArray("target_states"))));
        }

        List<String> exclusionLeaves = new ArrayList<>();
        Set<String> canonicalExclusionLeaves = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("exclusions")) {
            JsonObject exclusion = element.getAsJsonObject();
            String registry = exclusion.get("legacy_registry").getAsString();
            for (String legacyId : strings(exclusion.getAsJsonArray("legacy_ids"))) {
                String leaf = canonicalLeaf(registry, legacyId, "default");
                exclusionLeaves.add(leaf);
                assertTrue(canonicalExclusionLeaves.add(leaf), "Duplicate exclusion leaf: " + leaf);
            }
        }

        assertPinnedManifest(manifest, "mapping_leaf", MAPPING_LEAF_COUNT, MAPPING_LEAF_SHA256, mappingLeaves);
        assertPinnedHash(manifest, "mapping_projection_sha256", MAPPING_PROJECTION_SHA256, mappingProjections);
        assertPinnedManifest(manifest, "material_leaf", MATERIAL_LEAF_COUNT, MATERIAL_LEAF_SHA256, materialLeaves);
        assertPinnedHash(manifest, "material_projection_sha256", MATERIAL_PROJECTION_SHA256, materialProjections);
        assertPinnedManifest(manifest, "state_leaf", STATE_LEAF_COUNT, STATE_LEAF_SHA256, stateLeaves);
        assertPinnedHash(manifest, "state_projection_sha256", STATE_PROJECTION_SHA256, stateProjections);
        assertPinnedManifest(manifest, "exclusion_leaf", EXCLUSION_LEAF_COUNT, EXCLUSION_LEAF_SHA256,
                exclusionLeaves);

        List<Set<String>> partitions = List.of(
                canonicalMappingLeaves,
                canonicalMaterialLeaves,
                canonicalStateLeaves,
                canonicalExclusionLeaves
        );
        for (int left = 0; left < partitions.size(); left++) {
            for (int right = left + 1; right < partitions.size(); right++) {
                Set<String> overlap = new HashSet<>(partitions.get(left));
                overlap.retainAll(partitions.get(right));
                assertTrue(overlap.isEmpty(), "Legacy leaf partitions overlap: " + overlap);
            }
        }

        List<String> derivedRuntimeIds = new ArrayList<>();
        for (JsonElement familyElement : idMap.getAsJsonArray("derived_registry_families")) {
            JsonObject family = familyElement.getAsJsonObject();
            for (String baseId : strings(family.getAsJsonArray("base_ids"))) {
                for (JsonElement memberElement : family.getAsJsonArray("members")) {
                    JsonObject member = memberElement.getAsJsonObject();
                    derivedRuntimeIds.add(member.get("registry").getAsString() + ":"
                            + member.get("target_pattern").getAsString().replace("{base}", baseId));
                }
            }
        }
        assertPinnedManifest(manifest, "derived_runtime_id", DERIVED_RUNTIME_ID_COUNT,
                DERIVED_RUNTIME_ID_SHA256, derivedRuntimeIds);

        List<String> supportingIds = new ArrayList<>();
        for (JsonElement element : idMap.getAsJsonArray("supporting_registry_mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            supportingIds.add(mapping.get("registry").getAsString() + ":" + mapping.get("target_id").getAsString());
        }
        assertPinnedManifest(manifest, "supporting_registry_id", SUPPORTING_REGISTRY_ID_COUNT,
                SUPPORTING_REGISTRY_ID_SHA256, supportingIds);

        List<String> forbiddenIds = strings(idMap.getAsJsonArray("forbidden_runtime_ids"));
        assertPinnedManifest(manifest, "forbidden_runtime_id", FORBIDDEN_RUNTIME_ID_COUNT,
                FORBIDDEN_RUNTIME_ID_SHA256, forbiddenIds);
    }

    @Test
    void acceptanceCasesResolveExactlyOnceThroughPinnedNonOrphanRoutes() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        JsonObject idMap = readObject(ID_MAP_PATH);
        List<String> references = new ArrayList<>();
        collectAcceptanceTests(matrix, references);
        collectAcceptanceTests(idMap, references);
        TreeSet<String> inventory = new TreeSet<>(references);
        assertEquals(ACCEPTANCE_CASE_COUNT, inventory.size());
        assertEquals(ACCEPTANCE_CASE_SHA256, inventorySha256(new ArrayList<>(inventory)));

        JsonObject registry = matrix.getAsJsonObject("acceptance_case_registry");
        assertEquals("lexicographic_unique", registry.get("id_inventory_order").getAsString());
        assertEquals(ACCEPTANCE_CASE_COUNT, registry.get("id_count").getAsInt());
        assertEquals(ACCEPTANCE_CASE_SHA256, registry.get("id_sha256").getAsString());
        assertNonBlank(registry, "resolution_rule");

        Map<String, String> tokenRoutes = new TreeMap<>();
        Set<String> routeIds = new HashSet<>();
        Set<String> usedRoutes = new HashSet<>();
        for (JsonElement element : registry.getAsJsonArray("routes")) {
            JsonObject route = element.getAsJsonObject();
            assertNonBlank(route, "id");
            assertNonBlank(route, "harness");
            assertNonBlank(route, "locator");
            String routeId = route.get("id").getAsString();
            assertTrue(routeIds.add(routeId), "Duplicate acceptance route: " + routeId);
            JsonArray tokens = route.getAsJsonArray("tokens");
            assertNonEmpty(tokens, routeId);
            for (String token : strings(tokens)) {
                assertTrue(token.matches("[A-Z0-9]+"), "Invalid route token: " + token);
                assertTrue(tokenRoutes.put(token, routeId) == null,
                        "Token resolves through multiple routes: " + token);
            }
        }

        Set<String> usedTokens = new HashSet<>();
        for (String acceptanceId : inventory) {
            assertTrue(ACCEPTANCE_CASE_ID.matcher(acceptanceId).matches(),
                    "Invalid acceptance case ID: " + acceptanceId);
            int tokenEnd = acceptanceId.indexOf('-', "MIG-".length());
            assertTrue(tokenEnd > "MIG-".length(), "Missing first token: " + acceptanceId);
            String token = acceptanceId.substring("MIG-".length(), tokenEnd);
            assertTrue(tokenRoutes.containsKey(token), "No acceptance route for " + acceptanceId);
            usedTokens.add(token);
            usedRoutes.add(tokenRoutes.get(token));
        }
        assertEquals(tokenRoutes.keySet(), usedTokens, "Acceptance route token is orphaned");
        assertEquals(routeIds, usedRoutes, "Acceptance route is orphaned");
    }

    @Test
    void singleBlockMachineCatalogResolvesTwentyFourRealContracts() throws IOException {
        JsonObject catalog = readObject(MATRIX_PATH).getAsJsonObject("single_block_machine_catalog");
        assertNotNull(catalog);
        assertEquals(1, catalog.get("schema_version").getAsInt());
        assertEquals("0.5.0", catalog.get("stage").getAsString());
        assertEquals(24, catalog.get("expected_count").getAsInt());

        Set<String> expectedIds = Set.of(
                "magneticraft:airlock",
                "magneticraft:battery_box",
                "magneticraft:brick_furnace",
                "magneticraft:combustion_chamber",
                "magneticraft:crushing_table",
                "magneticraft:electric_engine",
                "magneticraft:electric_furnace",
                "magneticraft:electric_heater",
                "magneticraft:fabricator",
                "magneticraft:feeding_trough",
                "magneticraft:forge_energy_heater",
                "magneticraft:forge_energy_transformer",
                "magneticraft:gasification_unit",
                "magneticraft:infinite_energy_source",
                "magneticraft:inserter",
                "magneticraft:pneumatic_filter",
                "magneticraft:pneumatic_relay",
                "magneticraft:pneumatic_transposer",
                "magneticraft:sluice_box",
                "magneticraft:small_tank",
                "magneticraft:steam_boiler",
                "magneticraft:thermopile",
                "magneticraft:water_generator",
                "magneticraft:wooden_crate"
        );
        Set<String> allowedObtainability = new HashSet<>(strings(
                catalog.getAsJsonArray("obtainability_values")
        ));
        assertEquals(Set.of("survival", "creative_only"), allowedObtainability);

        Path generatedRoot = Path.of(catalog.get("generated_root").getAsString());
        assertTrue(Files.isDirectory(generatedRoot), "Missing generated resource root: " + generatedRoot);
        JsonArray entries = catalog.getAsJsonArray("entries");
        assertEquals(catalog.get("expected_count").getAsInt(), entries.size());

        Set<String> targetIds = new HashSet<>();
        Set<String> creativeOnly = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String targetId = entry.get("target_id").getAsString();
            assertTrue(targetId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), targetId);
            assertTrue(targetIds.add(targetId), "Duplicate single-block target ID: " + targetId);
            assertDecision(entry.get("disposition").getAsString(), targetId);

            String obtainability = entry.get("obtainability").getAsString();
            assertTrue(allowedObtainability.contains(obtainability), targetId + ": " + obtainability);
            if (obtainability.equals("creative_only")) {
                creativeOnly.add(targetId);
            }

            JsonArray legacySources = entry.getAsJsonArray("legacy_sources");
            assertNonEmpty(legacySources, targetId);
            for (JsonElement sourceElement : legacySources) {
                JsonObject source = sourceElement.getAsJsonObject();
                assertNonBlank(source, "path");
                assertNonBlank(source, "symbol");
                Path sourcePath = Path.of(source.get("path").getAsString());
                assertTrue(Files.isRegularFile(sourcePath), "Missing legacy source for " + targetId + ": " + sourcePath);
                String symbol = source.get("symbol").getAsString();
                assertTrue(Files.readString(sourcePath).contains(symbol),
                        "Missing legacy symbol for " + targetId + ": " + sourcePath + "#" + symbol);
            }

            String targetPath = targetId.substring(targetId.indexOf(':') + 1);
            JsonObject resources = entry.getAsJsonObject("generated_resources");
            assertNotNull(resources, targetId);
            String guideResource = resources.get("guide").getAsString();
            assertEquals("assets/magneticraft/guide/machines/" + targetPath + ".json", guideResource);
            Path guidePath = generatedRoot.resolve(guideResource);
            assertTrue(Files.isRegularFile(guidePath), "Missing generated guide for " + targetId + ": " + guidePath);
            assertEquals(targetId, readObject(guidePath).get("id").getAsString());

            JsonElement craftingRecipe = resources.get("crafting_recipe");
            assertNotNull(craftingRecipe, targetId + " must declare its crafting-recipe decision");
            Path expectedCraftingPath = generatedRoot.resolve(
                    "data/magneticraft/recipes/crafting/" + targetPath + ".json"
            );
            if (obtainability.equals("creative_only")) {
                assertTrue(craftingRecipe.isJsonNull(), targetId + " must not declare a survival recipe");
                assertFalse(Files.exists(expectedCraftingPath), targetId + " leaked a survival recipe");
            } else {
                assertTrue(craftingRecipe.isJsonPrimitive(), targetId + " has no crafting resource");
                assertEquals(
                        "data/magneticraft/recipes/crafting/" + targetPath + ".json",
                        craftingRecipe.getAsString()
                );
                assertTrue(Files.isRegularFile(expectedCraftingPath),
                        "Missing survival recipe for " + targetId + ": " + expectedCraftingPath);
            }

            JsonArray processingTypes = resources.getAsJsonArray("processing_recipe_types");
            JsonArray processingDirectories = resources.getAsJsonArray("processing_resource_directories");
            assertNotNull(processingTypes, targetId);
            assertNotNull(processingDirectories, targetId);
            Set<String> recipeTypes = new HashSet<>(strings(processingTypes));
            for (JsonElement directoryElement : processingDirectories) {
                Path directory = generatedRoot.resolve(directoryElement.getAsString());
                assertTrue(Files.isDirectory(directory),
                        "Missing processing resource directory for " + targetId + ": " + directory);
                try (Stream<Path> files = Files.list(directory)) {
                    List<Path> recipes = files
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .toList();
                    assertFalse(recipes.isEmpty(), "Empty processing resource directory for " + targetId);
                    for (Path recipe : recipes) {
                        String recipeType = readObject(recipe).get("type").getAsString();
                        assertTrue(recipeTypes.contains(recipeType),
                                targetId + " does not declare generated recipe type " + recipeType);
                    }
                }
            }

            JsonArray testLocators = entry.getAsJsonArray("test_locators");
            assertNonEmpty(testLocators, targetId);
            for (JsonElement locatorElement : testLocators) {
                JsonObject locator = locatorElement.getAsJsonObject();
                assertNonBlank(locator, "path");
                assertNonBlank(locator, "method");
                Path testPath = Path.of(locator.get("path").getAsString());
                assertTrue(Files.isRegularFile(testPath), "Missing test source for " + targetId + ": " + testPath);
                String method = locator.get("method").getAsString();
                Pattern declaration = Pattern.compile(
                        "\\b(?:public\\s+static\\s+)?void\\s+" + Pattern.quote(method) + "\\s*\\("
                );
                assertTrue(declaration.matcher(Files.readString(testPath)).find(),
                        "Missing test method for " + targetId + ": " + testPath + "#" + method);
            }
        }

        assertEquals(expectedIds, targetIds);
        assertEquals(Set.of("magneticraft:infinite_energy_source"), creativeOnly);
    }

    @Test
    void registryProjectionIsAlignedUniqueAndNormalized() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        assertEquals("docs/porting/migration-matrix.json", idMap.get("source_matrix").getAsString());
        assertTrue(idMap.get("generated_projection").getAsBoolean());
        assertEquals("0.2.0", idMap.get("frozen_from_release").getAsString());

        Set<String> legacyKeys = new HashSet<>();
        Set<String> targetKeys = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            assertDecision(mapping.get("disposition").getAsString(), mapping.toString());
            assertStage(mapping.get("stage").getAsString(), mapping.toString());
            assertNonBlank(mapping, "source");
            assertNonEmpty(mapping.getAsJsonArray("acceptance_tests"), mapping.toString());

            List<String> legacyValues = strings(mapping.getAsJsonArray("legacy_variants"));
            List<String> targetIds = strings(mapping.getAsJsonArray("target_ids"));
            List<String> targetVariants = strings(mapping.getAsJsonArray("target_variants"));
            assertEquals(legacyValues.size(), targetIds.size(), mapping.toString());
            assertEquals(targetIds.size(), targetVariants.size(), mapping.toString());

            String legacyRegistry = mapping.get("legacy_registry").getAsString();
            String legacyId = mapping.get("legacy_id").getAsString();
            String targetRegistry = mapping.get("target_registry").getAsString();
            for (int index = 0; index < legacyValues.size(); index++) {
                String legacyKey = legacyRegistry + ":" + legacyId + "#" + legacyValues.get(index);
                assertTrue(legacyKeys.add(legacyKey), "Duplicate legacy mapping: " + legacyKey);

                String targetId = targetIds.get(index);
                assertNormalizedTargetId(targetId);
                String targetKey = targetRegistry + ":" + targetId + "#" + targetVariants.get(index);
                assertTrue(targetKeys.add(targetKey), "Duplicate target mapping: " + targetKey);
            }
        }

        int materialLeaves = 0;
        Set<String> materialLegacyKeys = new HashSet<>();
        Set<String> materialTargets = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("material_families")) {
            JsonObject family = element.getAsJsonObject();
            String legacyId = family.get("legacy_id").getAsString();
            String form = family.get("form").getAsString();
            List<String> variants = strings(family.getAsJsonArray("variants"));
            assertEquals(family.get("expected_leaf_count").getAsInt(), variants.size(), legacyId);
            String pattern = family.get("target_pattern").getAsString();
            JsonObject overrides = family.getAsJsonObject("overrides");
            for (String material : variants) {
                assertTrue(materialLegacyKeys.add(legacyId + "#" + material), legacyId + "#" + material);
                String target = overrides.has(material)
                        ? overrides.get(material).getAsString()
                        : pattern.replace("{material}", material);
                assertNormalizedTargetId(target);
                assertTrue(materialTargets.add(target), "Duplicate material target: " + target);
                if (target.startsWith("magneticraft:")) {
                    assertTrue(target.endsWith("_" + form), target);
                }
                materialLeaves++;
            }
        }
        assertEquals(77, materialLeaves);

        for (JsonElement element : idMap.getAsJsonArray("supporting_registry_mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            assertNonBlank(mapping, "registry");
            assertNormalizedTargetId(mapping.get("target_id").getAsString());
            assertNormalizedTargetId(mapping.get("owner").getAsString());
        }

        Set<String> excluded = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("exclusions")) {
            JsonObject exclusion = element.getAsJsonObject();
            assertEquals("exclude", exclusion.get("disposition").getAsString());
            assertNonBlank(exclusion, "source");
            assertNonBlank(exclusion, "evidence");
            for (String id : strings(exclusion.getAsJsonArray("legacy_ids"))) {
                assertTrue(excluded.add(exclusion.get("legacy_registry").getAsString() + ":" + id), id);
            }
        }
        assertTrue(excluded.contains("item:broken_gear"));
        assertTrue(excluded.contains("behavior:tcp_ssl_network_card"));
    }

    @Test
    void normalizedRegistryRowsResolveDataPathsBehaviorAndLeafEvidence() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        Set<String> behaviorIds = behaviorContractIds(readObject(MATRIX_PATH));
        JsonObject normalized = idMap.getAsJsonObject("normalized_row_contracts");
        assertNonBlank(normalized, "join_rule");
        JsonObject profiles = normalized.getAsJsonObject("data_path_profiles");
        JsonObject mappingContracts = normalized.getAsJsonObject("mapping_contracts");
        JsonObject stateContracts = normalized.getAsJsonObject("state_schema_contracts");

        Set<String> mappingKeys = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            String key = mapping.get("legacy_registry").getAsString() + ":" + mapping.get("legacy_id").getAsString();
            assertTrue(mappingKeys.add(key), "Duplicate normalized mapping key: " + key);
            assertTrue(mappingContracts.has(key), "Missing normalized row contract: " + key);
            assertProfileContract(mappingContracts.getAsJsonObject(key), profiles, behaviorIds, key);
        }
        assertEquals(mappingKeys, mappingContracts.keySet(), "Orphan normalized mapping contract");

        Set<String> stateKeys = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("legacy_state_schemas")) {
            String key = element.getAsJsonObject().get("legacy_enum").getAsString();
            assertTrue(stateKeys.add(key), "Duplicate state contract key: " + key);
            assertTrue(stateContracts.has(key), "Missing state data/behavior contract: " + key);
            assertProfileContract(stateContracts.getAsJsonObject(key), profiles, behaviorIds, key);
        }
        assertEquals(stateKeys, stateContracts.keySet(), "Orphan state schema contract");

        for (JsonElement element : idMap.getAsJsonArray("material_families")) {
            JsonObject family = element.getAsJsonObject();
            String legacyId = family.get("legacy_id").getAsString();
            assertEquals("item", family.get("legacy_registry").getAsString(), legacyId);
            assertDecision(family.get("disposition").getAsString(), legacyId);
            assertStage(family.get("stage").getAsString(), legacyId);
            assertNonBlank(family, "source");
            assertNonBlank(family, "notes");
            assertNonEmpty(family.getAsJsonArray("acceptance_tests"), legacyId);
            assertProfileContract(family, profiles, behaviorIds, legacyId);
        }
    }

    @Test
    void forgeFluidFamiliesExpandEveryFrozenRuntimeRegistration() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        JsonArray families = idMap.getAsJsonArray("derived_registry_families");
        assertEquals(1, families.size());
        JsonObject family = families.get(0).getAsJsonObject();
        assertEquals("magneticraft_fluids", family.get("id").getAsString());
        assertDecision(family.get("disposition").getAsString(), family.toString());
        assertStage(family.get("stage").getAsString(), family.toString());
        assertNonBlank(family, "source");
        assertNonBlank(family, "data_path_profile");
        assertNonBlank(family, "notes");
        assertNonEmpty(family.getAsJsonArray("behavior_contract_ids"), family.toString());
        assertNonEmpty(family.getAsJsonArray("acceptance_tests"), family.toString());

        JsonObject fluidMapping = null;
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            if (mapping.get("legacy_registry").getAsString().equals("fluid")
                    && mapping.get("legacy_id").getAsString().equals("magneticraft_fluids")) {
                fluidMapping = mapping;
                break;
            }
        }
        assertNotNull(fluidMapping);
        List<String> baseIds = strings(family.getAsJsonArray("base_ids"));
        assertEquals(strings(fluidMapping.getAsJsonArray("target_ids")), baseIds);
        assertEquals(baseIds.size(), new HashSet<>(baseIds).size());

        Map<String, String> expectedMembers = Map.of(
                "fluid_type", "fluid_type:{base}",
                "source_fluid", "fluid:{base}",
                "flowing_fluid", "fluid:{base}_flowing",
                "liquid_block", "block:{base}",
                "bucket", "item:{base}_bucket"
        );
        Set<String> actualMembers = new HashSet<>();
        Set<String> expandedRuntimeKeys = new HashSet<>();
        for (JsonElement element : family.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            String role = member.get("role").getAsString();
            String registry = member.get("registry").getAsString();
            String pattern = member.get("target_pattern").getAsString();
            assertTrue(actualMembers.add(role), "Duplicate fluid family role: " + role);
            assertEquals(expectedMembers.get(role), registry + ":" + pattern, role);
            for (String baseId : baseIds) {
                String targetId = pattern.replace("{base}", baseId);
                assertNormalizedTargetId(targetId);
                assertTrue(expandedRuntimeKeys.add(registry + ":" + targetId),
                        "Duplicate derived runtime ID: " + registry + ":" + targetId);
            }
        }
        assertEquals(expectedMembers.keySet(), actualMembers);
        assertEquals(baseIds.size() * expectedMembers.size(), expandedRuntimeKeys.size());

        Set<String> currentFluidDefinitions = extractNamespacedIds(
                FLUID_DEFINITION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/content/fluid/FluidDefinition.java")
        );
        assertEquals(new HashSet<>(baseIds), currentFluidDefinitions);
    }

    @Test
    void everyLegacyJsonRecipeHasExactlyOneDecision() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        JsonObject recipes = matrix.getAsJsonObject("legacy_json_recipes");
        Set<String> behaviorIds = behaviorContractIds(matrix);
        List<String> inventory = strings(recipes.getAsJsonArray("inventory"));
        assertEquals(137, inventory.size());
        assertEquals(137, new HashSet<>(inventory).size());

        List<String> assigned = new ArrayList<>();
        Set<String> legacyGroupIds = new HashSet<>();
        for (JsonElement element : recipes.getAsJsonArray("decision_groups")) {
            JsonObject group = element.getAsJsonObject();
            assertNonBlank(group, "id");
            String groupId = group.get("id").getAsString();
            assertTrue(legacyGroupIds.add(groupId), "Duplicate legacy recipe group: " + groupId);
            assertDecision(group.get("disposition").getAsString(), group.toString());
            assertStage(group.get("stage").getAsString(), group.toString());
            assertNonBlank(group, "target_rule");
            assertNonBlank(group, "source");
            assertNonEmpty(group.getAsJsonArray("acceptance_tests"), group.toString());
            assertNonEmpty(group.getAsJsonArray("members"), group.toString());
            List<String> members = strings(group.getAsJsonArray("members"));
            assigned.addAll(members);

            JsonArray targetPaths = group.getAsJsonArray("target_paths");
            assertNotNull(targetPaths, groupId);
            assertEquals(members.size(), targetPaths.size(), groupId);
            boolean excluded = group.get("disposition").getAsString().equals("exclude");
            for (int index = 0; index < targetPaths.size(); index++) {
                JsonElement targetElement = targetPaths.get(index);
                if (excluded) {
                    assertTrue(targetElement.isJsonNull(),
                            "Excluded recipe must have a null target: " + groupId + "/" + members.get(index));
                    continue;
                }
                assertTrue(targetElement.isJsonPrimitive(),
                        "Recipe target path must be a string: " + groupId + "/" + members.get(index));
                String targetPath = targetElement.getAsString();
                assertTrue(DATA_RECIPE_PATH.matcher(targetPath).matches(), "Invalid recipe data path: " + targetPath);
                assertFalse(targetPath.contains(".."), "Recipe data path escapes its root: " + targetPath);
                assertFalse(targetPath.contains("//"), "Recipe data path has an empty segment: " + targetPath);
                assertFalse(targetPath.contains("{"), "Legacy recipe target is unresolved: " + targetPath);
                if (group.get("stage").getAsString().equals("0.2.0")
                        && targetPath.startsWith("data/magneticraft/")) {
                    assertTrue(Files.isRegularFile(Path.of("src/generated/resources").resolve(targetPath)),
                            "Frozen 0.2.0 recipe was not generated: " + targetPath);
                }
            }
        }

        assertEquals(137, assigned.size());
        assertEquals(137, new HashSet<>(assigned).size());
        assertEquals(new HashSet<>(inventory), new HashSet<>(assigned));

        JsonObject normalized = matrix.getAsJsonObject("normalized_recipe_contracts");
        assertNonBlank(normalized, "join_rule");
        JsonObject legacyContracts = normalized.getAsJsonObject("legacy_json_groups");
        assertEquals(legacyGroupIds, legacyContracts.keySet(), "Normalized legacy recipe contract drift");
        for (String groupId : legacyGroupIds) {
            assertDirectContract(legacyContracts.getAsJsonObject(groupId), behaviorIds, groupId);
        }

        Set<String> programmaticIds = new HashSet<>();
        Set<String> projectedRecipeTypes = supportingIds(readObject(ID_MAP_PATH), "recipe_type");
        for (JsonElement element : matrix.getAsJsonArray("programmatic_recipe_groups")) {
            JsonObject group = element.getAsJsonObject();
            String groupId = group.get("id").getAsString();
            assertTrue(programmaticIds.add(groupId), "Duplicate programmatic recipe group: " + groupId);
            if (!group.get("disposition").getAsString().equals("exclude")) {
                String targetType = group.get("target_type").getAsString();
                assertNormalizedTargetId(targetType);
                if (targetType.startsWith("magneticraft:")) {
                    assertTrue(projectedRecipeTypes.contains(targetType),
                            "Programmatic target type is not projected: " + groupId + " -> " + targetType);
                }
            }
        }
        JsonObject programmaticContracts = normalized.getAsJsonObject("programmatic_groups");
        assertEquals(programmaticIds, programmaticContracts.keySet(), "Normalized programmatic contract drift");
        for (String groupId : programmaticIds) {
            assertDirectContract(programmaticContracts.getAsJsonObject(groupId), behaviorIds, groupId);
        }
    }

    @Test
    void exclusionsAreEvidenceBackedAndReferenceKnownContracts() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        Set<String> behaviorIds = behaviorContractIds(readObject(MATRIX_PATH));
        int leafCount = 0;
        Set<String> leaves = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("exclusions")) {
            JsonObject exclusion = element.getAsJsonObject();
            assertEquals("exclude", exclusion.get("disposition").getAsString());
            assertStage(exclusion.get("stage").getAsString(), exclusion.toString());
            assertNonBlank(exclusion, "legacy_registry");
            assertNonBlank(exclusion, "source");
            assertNonBlank(exclusion, "evidence");
            JsonArray targetDataPaths = exclusion.getAsJsonArray("target_data_paths");
            assertNotNull(targetDataPaths, exclusion.toString());
            assertEquals(0, targetDataPaths.size(), "Excluded content must not declare target data paths");
            assertNonEmpty(exclusion.getAsJsonArray("acceptance_tests"), exclusion.toString());
            JsonArray contractIds = exclusion.getAsJsonArray("behavior_contract_ids");
            assertNonEmpty(contractIds, exclusion.toString());
            for (String contractId : strings(contractIds)) {
                assertTrue(behaviorIds.contains(contractId), "Unknown exclusion behavior contract: " + contractId);
            }
            List<String> legacyIds = strings(exclusion.getAsJsonArray("legacy_ids"));
            assertFalse(legacyIds.isEmpty(), "Exclusion has no legacy IDs: " + exclusion);
            for (String legacyId : legacyIds) {
                assertTrue(FROZEN_PATH.matcher(legacyId).matches(), "Invalid excluded legacy ID: " + legacyId);
                assertTrue(leaves.add(canonicalLeaf(
                        exclusion.get("legacy_registry").getAsString(), legacyId, "default")),
                        "Duplicate excluded legacy leaf: " + legacyId);
                leafCount++;
            }
        }
        assertEquals(EXCLUSION_LEAF_COUNT, leafCount);
    }

    @Test
    void qualityGateIsPinnedInExecutionOrder() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        if (matrix.has("quality_gate")) {
            assertEquals(QUALITY_GATE, strings(matrix.getAsJsonArray("quality_gate")));
        }
    }

    @Test
    void releaseRoutePointsToExecutableCurrentEvidence() throws IOException {
        JsonArray routes = readObject(MATRIX_PATH)
                .getAsJsonObject("acceptance_case_registry")
                .getAsJsonArray("routes");
        JsonObject releaseRoute = null;
        for (JsonElement element : routes) {
            JsonObject route = element.getAsJsonObject();
            if (route.get("id").getAsString().equals("release-manual-and-performance")) {
                releaseRoute = route;
                break;
            }
        }

        assertNotNull(releaseRoute, "Missing release acceptance route");
        assertEquals("junit_gametest_runtime_matrix_and_visual_checklist",
                releaseRoute.get("harness").getAsString());
        String locator = releaseRoute.get("locator").getAsString();
        assertFalse(locator.contains("docs/porting/release-readiness.md"),
                "Release-candidate evidence still points only to the historical report");
        assertTrue(locator.contains("docs/porting/1.0.0-visual-release.md"));
        assertTrue(locator.contains("IncrementalGraphTest.java"));
        assertTrue(locator.contains("PhysicalNetworkManagerLogisticsTest.java"));
        assertTrue(locator.contains("ReleaseCandidateBudgetContractTest.java"));
        assertTrue(locator.contains("LegacyVisualAssetContractTest.java"));
        assertTrue(locator.contains("scripts/convert_legacy_models.py"));
        assertTrue(locator.contains("SingleBlockMachineGameTests.java"));
        for (String path : locator.split(";")) {
            assertTrue(Files.isRegularFile(Path.of(path)), "Missing release-candidate evidence: " + path);
        }
    }

    @Test
    void pinnedLegacyInventoryFingerprintsMatchTheirDeclaredAuthority() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        JsonObject authority = matrix.getAsJsonObject("authority");

        assertEquals(LEGACY_RECIPE_MANIFEST_SHA256,
                authority.get("legacy_json_recipe_manifest_sha256").getAsString());
        assertEquals(LEGACY_RECIPE_MANIFEST_SHA256,
                inventorySha256(strings(matrix.getAsJsonObject("legacy_json_recipes").getAsJsonArray("inventory"))));
        assertEquals(LEGACY_RECIPE_CONTENT_MANIFEST_SHA256,
                authority.get("legacy_json_recipe_content_manifest_sha256").getAsString());
        assertEquals(LEGACY_GUIDE_MANIFEST_SHA256,
                authority.get("legacy_english_guide_manifest_sha256").getAsString());
        assertEquals(LEGACY_GUIDE_MANIFEST_SHA256,
                inventorySha256(strings(matrix.getAsJsonObject("legacy_guides").getAsJsonArray("inventory"))));
        assertEquals(LEGACY_GUIDE_CONTENT_MANIFEST_SHA256,
                authority.get("legacy_english_guide_content_manifest_sha256").getAsString());
    }

    @Test
    void everyLegacyStateEnumHasAnExplicitTargetProjection() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        JsonObject rules = idMap.getAsJsonObject("legacy_state_schema_rules");
        assertEquals("zero_based_index_in_legacy_values", rules.get("metadata_ordinal").getAsString());
        assertEquals(
                "fixed/owning/internal owners reuse legacy ordinal; flattened variants use the same target index; "
                        + "per-owner flattening expands owner-major",
                rules.get("target_state_projection").getAsString()
        );
        assertNonBlank(rules, "target_id_projection");
        assertNonBlank(rules, "stage_semantics");

        JsonArray schemas = idMap.getAsJsonArray("legacy_state_schemas");
        assertEquals(LEGACY_STATE_ENUM_VALUES.size(), schemas.size());

        Set<String> actualEnums = new HashSet<>();
        for (JsonElement element : schemas) {
            JsonObject schema = element.getAsJsonObject();
            String legacyEnum = schema.get("legacy_enum").getAsString();
            assertTrue(actualEnums.add(legacyEnum), "Duplicate legacy state schema: " + legacyEnum);
            assertEquals(LEGACY_STATE_ENUM_VALUES.get(legacyEnum), strings(schema.getAsJsonArray("legacy_values")),
                    "Unexpected or incomplete values for " + legacyEnum);
            assertNonBlank(schema, "legacy_property");
            assertNonBlank(schema, "target_id_mode");
            String targetIdMode = schema.get("target_id_mode").getAsString();
            assertTrue(STATE_ID_MODES.contains(targetIdMode), "Unknown state target mode: " + targetIdMode);
            assertDecision(schema.get("disposition").getAsString(), legacyEnum);
            assertStage(schema.get("stage").getAsString(), legacyEnum);
            assertNonBlank(schema, "source");
            assertNonEmpty(schema.getAsJsonArray("acceptance_tests"), legacyEnum);

            List<String> legacyValues = strings(schema.getAsJsonArray("legacy_values"));
            List<String> targetStates = strings(schema.getAsJsonArray("target_states"));
            assertTrue(targetStates.stream().noneMatch(String::isBlank), "Blank target state for " + legacyEnum);
            assertEquals(targetStates.size(), new HashSet<>(targetStates).size(),
                    "Duplicate target state projection for " + legacyEnum);

            List<String> targetIds = strings(schema.getAsJsonArray("target_ids"));
            if (schema.get("disposition").getAsString().equals("exclude")) {
                assertTrue(targetIds.isEmpty(), "Excluded state schema must not invent a target ID: " + legacyEnum);
                assertNonBlank(schema, "evidence");
            } else {
                assertFalse(targetIds.isEmpty(), "Missing target owners for " + legacyEnum);
            }
            targetIds.forEach(MigrationMatrixContractTest::assertNormalizedTargetId);
            assertEquals(targetIds.size(), new HashSet<>(targetIds).size(), "Duplicate target owner for " + legacyEnum);

            switch (targetIdMode) {
                case "fixed_owner", "internal_owner" -> {
                    assertEquals(1, targetIds.size(), legacyEnum);
                    assertEquals(legacyValues.size(), targetStates.size(), legacyEnum);
                }
                case "owning_content" -> {
                    assertFalse(targetIds.isEmpty(), legacyEnum);
                    assertEquals(legacyValues.size(), targetStates.size(), legacyEnum);
                }
                case "flattened_variant" -> {
                    assertEquals(legacyValues.size(), targetIds.size(), legacyEnum);
                    assertEquals(legacyValues.size(), targetStates.size(), legacyEnum);
                }
                case "flattened_variant_per_owner" -> {
                    JsonArray ownerGroups = schema.getAsJsonArray("owner_groups");
                    assertNotNull(ownerGroups, legacyEnum);
                    assertTrue(ownerGroups.size() > 0, legacyEnum);
                    List<String> expandedIds = new ArrayList<>();
                    Set<String> owners = new HashSet<>();
                    for (JsonElement ownerElement : ownerGroups) {
                        JsonObject owner = ownerElement.getAsJsonObject();
                        assertNonBlank(owner, "legacy_owner");
                        assertTrue(owners.add(owner.get("legacy_owner").getAsString()),
                                "Duplicate legacy owner for " + legacyEnum);
                        List<String> ownerIds = strings(owner.getAsJsonArray("target_ids"));
                        assertEquals(legacyValues.size(), ownerIds.size(), owner.toString());
                        expandedIds.addAll(ownerIds);
                    }
                    assertEquals(targetIds, expandedIds, "Owner-major target projection mismatch for " + legacyEnum);
                    assertEquals(targetIds.size(), targetStates.size(), legacyEnum);
                }
                default -> throw new AssertionError("Unhandled state target mode: " + targetIdMode);
            }

            if (legacyEnum.equals("DecayMode")) {
                assertEquals("rebuild", schema.get("disposition").getAsString());
                assertEquals("0.5.0", schema.get("stage").getAsString());
                assertEquals(List.of("magneticraft:air_bubble"), targetIds);
                assertEquals("decaying", schema.get("target_property").getAsString());
                assertEquals(List.of("false", "true"), strings(schema.getAsJsonArray("target_values")));
                assertEquals(List.of("decaying=false", "decaying=true"), targetStates);
                assertNonBlank(schema, "evidence");
            }

            if (CROSS_STAGE_STATE_ENUMS.contains(legacyEnum)) {
                assertTrue(schema.has("owner_stages") && schema.get("owner_stages").isJsonObject(),
                        "Cross-stage schema needs owner_stages: " + legacyEnum);
                JsonObject ownerStages = schema.getAsJsonObject("owner_stages");
                assertEquals(new HashSet<>(targetIds), ownerStages.keySet(),
                        "Owner-stage coverage mismatch for " + legacyEnum);
                List<String> stages = new ArrayList<>();
                ownerStages.entrySet().forEach(entry -> {
                    assertNormalizedTargetId(entry.getKey());
                    String ownerStage = entry.getValue().getAsString();
                    assertStage(ownerStage, entry.getKey());
                    stages.add(ownerStage);
                });
                assertEquals(stages.stream().min(String::compareTo).orElseThrow(),
                        schema.get("stage").getAsString(), "Earliest stage mismatch for " + legacyEnum);
            }
        }
        assertEquals(LEGACY_STATE_ENUM_VALUES.keySet(), actualEnums);
    }

    @Test
    void forbiddenRuntimeIdsAreUniqueNormalizedAndAbsentFromTargets() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        List<String> forbiddenIds = strings(idMap.getAsJsonArray("forbidden_runtime_ids"));
        Set<String> uniqueForbiddenIds = new HashSet<>(forbiddenIds);
        assertFalse(forbiddenIds.isEmpty());
        assertEquals(forbiddenIds.size(), uniqueForbiddenIds.size(), "Duplicate forbidden runtime ID");
        for (String id : forbiddenIds) {
            assertTrue(FROZEN_PATH.matcher(id).matches(), "Non-normalized forbidden runtime ID: " + id);
            assertEquals(id.toLowerCase(Locale.ROOT), id, "Uppercase forbidden runtime ID: " + id);
        }

        Set<String> targetPaths = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            strings(element.getAsJsonObject().getAsJsonArray("target_ids"))
                    .forEach(id -> targetPaths.add(resourcePath(id)));
        }
        for (JsonElement element : idMap.getAsJsonArray("material_families")) {
            JsonObject family = element.getAsJsonObject();
            JsonObject overrides = family.getAsJsonObject("overrides");
            String pattern = family.get("target_pattern").getAsString();
            for (String material : strings(family.getAsJsonArray("variants"))) {
                String target = overrides.has(material)
                        ? overrides.get(material).getAsString()
                        : pattern.replace("{material}", material);
                targetPaths.add(resourcePath(target));
            }
        }
        for (JsonElement element : idMap.getAsJsonArray("supporting_registry_mappings")) {
            targetPaths.add(resourcePath(element.getAsJsonObject().get("target_id").getAsString()));
        }
        for (JsonElement element : idMap.getAsJsonArray("legacy_state_schemas")) {
            strings(element.getAsJsonObject().getAsJsonArray("target_ids"))
                    .forEach(id -> targetPaths.add(resourcePath(id)));
        }
        assertTrue(targetPaths.stream().noneMatch(uniqueForbiddenIds::contains),
                "A forbidden legacy ID remains a frozen target: " + targetPaths);
    }

    @Test
    void supportingRegistryProjectionMatchesEveryCurrentJavaRegistration() throws IOException {
        JsonArray mappings = readObject(ID_MAP_PATH).getAsJsonArray("supporting_registry_mappings");
        Map<String, Set<String>> projected = new HashMap<>();
        Set<String> keys = new HashSet<>();
        for (JsonElement element : mappings) {
            JsonObject mapping = element.getAsJsonObject();
            String registry = mapping.get("registry").getAsString();
            String targetId = mapping.get("target_id").getAsString();
            assertTrue(SUPPORTING_RUNTIME_IDS.containsKey(registry), "Unknown supporting registry: " + registry);
            assertTrue(keys.add(registry + ":" + targetId), "Duplicate supporting mapping: " + mapping);
            projected.computeIfAbsent(registry, ignored -> new HashSet<>()).add(targetId);

            boolean hasLegacyId = mapping.has("legacy_id")
                    && !mapping.get("legacy_id").getAsString().isBlank();
            boolean isInternal = mapping.has("internal") && mapping.get("internal").getAsBoolean();
            assertTrue(hasLegacyId ^ isInternal,
                    "Supporting mapping must declare exactly one of legacy_id or internal=true: " + mapping);
            assertNormalizedTargetId(targetId);
            assertNormalizedTargetId(mapping.get("owner").getAsString());
            if (mapping.has("additional_owners")) {
                strings(mapping.getAsJsonArray("additional_owners"))
                        .forEach(MigrationMatrixContractTest::assertNormalizedTargetId);
            }
        }
        assertEquals(SUPPORTING_RUNTIME_IDS, projected);

        Set<String> blockEntityIds = extractNamespacedIds(
                BLOCK_ENTITY_REGISTRATION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModBlockEntities.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModComputerContent.java")
        );
        Set<String> menuIds = extractNamespacedIds(
                MENU_REGISTRATION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModMenus.java")
        );
        Path recipeSource = Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModRecipeTypes.java");
        Set<String> recipeTypeIds = extractNamespacedIds(RECIPE_TYPE_REGISTRATION, recipeSource);
        recipeTypeIds.addAll(extractNamespacedIds(RECIPE_TYPE_HELPER, recipeSource));
        Set<String> recipeSerializerIds = extractNamespacedIds(RECIPE_SERIALIZER_REGISTRATION, recipeSource);
        Set<String> soundIds = extractNamespacedIds(
                SOUND_REGISTRATION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModSounds.java")
        );

        assertEquals(SUPPORTING_RUNTIME_IDS.get("block_entity_type"), blockEntityIds);
        assertEquals(SUPPORTING_RUNTIME_IDS.get("menu"), menuIds);
        assertEquals(SUPPORTING_RUNTIME_IDS.get("recipe_type"), recipeTypeIds);
        assertEquals(SUPPORTING_RUNTIME_IDS.get("recipe_serializer"), recipeSerializerIds);
        assertEquals(SUPPORTING_RUNTIME_IDS.get("sound_event"), soundIds);
    }

    @Test
    void currentPrimaryBlockAndItemDefinitionsAreCoveredByTheRegistryProjection() throws IOException {
        JsonObject idMap = readObject(ID_MAP_PATH);
        Set<String> projectedBlocks = new HashSet<>();
        Set<String> projectedItems = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            String targetRegistry = mapping.get("target_registry").getAsString();
            for (String targetId : strings(mapping.getAsJsonArray("target_ids"))) {
                if (!targetId.startsWith("magneticraft:")) {
                    continue;
                }
                switch (targetRegistry) {
                    case "block_item" -> {
                        projectedBlocks.add(targetId);
                        projectedItems.add(targetId);
                    }
                    case "block_state" -> {
                        projectedBlocks.add(targetId);
                        projectedItems.add(targetId);
                    }
                    case "item", "item_state" -> projectedItems.add(targetId);
                    default -> {
                        // Other primary registries are checked by their dedicated projection tests.
                    }
                }
            }
        }
        for (JsonElement element : idMap.getAsJsonArray("material_families")) {
            JsonObject family = element.getAsJsonObject();
            JsonObject overrides = family.getAsJsonObject("overrides");
            String pattern = family.get("target_pattern").getAsString();
            for (String material : strings(family.getAsJsonArray("variants"))) {
                String targetId = overrides.has(material)
                        ? overrides.get(material).getAsString()
                        : pattern.replace("{material}", material);
                if (targetId.startsWith("magneticraft:")) {
                    projectedItems.add(targetId);
                }
            }
        }
        for (JsonElement element : idMap.getAsJsonArray("legacy_state_schemas")) {
            for (String targetId : strings(element.getAsJsonObject().getAsJsonArray("target_ids"))) {
                if (targetId.startsWith("magneticraft:")) {
                    projectedBlocks.add(targetId);
                    projectedItems.add(targetId);
                }
            }
        }
        for (JsonElement familyElement : idMap.getAsJsonArray("derived_registry_families")) {
            JsonObject family = familyElement.getAsJsonObject();
            for (JsonElement memberElement : family.getAsJsonArray("members")) {
                JsonObject member = memberElement.getAsJsonObject();
                String registry = member.get("registry").getAsString();
                for (String baseId : strings(family.getAsJsonArray("base_ids"))) {
                    String targetId = member.get("target_pattern").getAsString().replace("{base}", baseId);
                    if (registry.equals("block")) {
                        projectedBlocks.add(targetId);
                    } else if (registry.equals("item")) {
                        projectedItems.add(targetId);
                    }
                }
            }
        }
        for (JsonElement element : idMap.getAsJsonArray("supporting_registry_mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            String targetId = mapping.get("target_id").getAsString();
            switch (mapping.get("registry").getAsString()) {
                case "block" -> projectedBlocks.add(targetId);
                case "item" -> projectedItems.add(targetId);
                case "block_item" -> {
                    projectedBlocks.add(targetId);
                    projectedItems.add(targetId);
                }
                default -> {
                    // Non-primary supporting registries are checked by their dedicated projection test.
                }
            }
        }

        Set<String> currentBlocks = new HashSet<>();
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            currentBlocks.add(namespaced(definition.id()));
        }
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            currentBlocks.add(namespaced(definition.id()));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            currentBlocks.add(namespaced(definition.id()));
        }
        for (FluidDefinition definition : FluidDefinition.values()) {
            currentBlocks.add(namespaced(definition.id()));
        }
        currentBlocks.addAll(extractNamespacedIds(
                BLOCK_LITERAL_REGISTRATION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModNetworkBlocks.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModMachineBlocks.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModAdvancedBlocks.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModComputerContent.java")
        ));

        Set<String> currentItems = new HashSet<>(currentBlocks);
        currentItems.remove(namespaced("air_bubble"));
        currentItems.remove(namespaced("pumpjack_drill"));
        for (FluidDefinition definition : FluidDefinition.values()) {
            currentItems.remove(namespaced(definition.id()));
            currentItems.add(namespaced(definition.id() + "_bucket"));
        }
        for (MaterialForm form : MaterialForm.values()) {
            for (Metal metal : Metal.values()) {
                if (form.appliesTo(metal)) {
                    currentItems.add(namespaced(form.id(metal)));
                }
            }
        }
        for (CraftingComponent component : CraftingComponent.values()) {
            currentItems.add(namespaced(component.id()));
        }
        for (HammerType hammer : HammerType.values()) {
            currentItems.add(namespaced(hammer.id()));
        }
        currentItems.addAll(extractNamespacedIds(
                ITEM_LITERAL_REGISTRATION,
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModItems.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModMachineItems.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModNetworkItems.java"),
                Path.of("src/main/java/committee/nova/mods/magneticraft/init/ModComputerContent.java")
        ));

        assertCoveredByProjection(currentBlocks, projectedBlocks, "block");
        assertCoveredByProjection(currentItems, projectedItems, "item");

        Set<String> forbidden = new HashSet<>(strings(idMap.getAsJsonArray("forbidden_runtime_ids")));
        Set<String> currentPaths = new HashSet<>();
        currentBlocks.forEach(id -> currentPaths.add(resourcePath(id)));
        currentItems.forEach(id -> currentPaths.add(resourcePath(id)));
        Set<String> forbiddenCurrentIds = new HashSet<>(currentPaths);
        forbiddenCurrentIds.retainAll(forbidden);
        assertTrue(forbiddenCurrentIds.isEmpty(), "Current primary registry still exposes old aliases: "
                + forbiddenCurrentIds);

        assertTrue(Files.isRegularFile(BASE_CONTENT_GAMETEST), BASE_CONTENT_GAMETEST.toString());
        Matcher setMatcher = FORBIDDEN_GAMETEST_SET.matcher(Files.readString(BASE_CONTENT_GAMETEST));
        assertTrue(setMatcher.find(), "BaseContentGameTests does not declare FORBIDDEN_LEGACY_IDS");
        Set<String> runtimeForbiddenIds = new HashSet<>();
        Matcher literalMatcher = STRING_LITERAL.matcher(setMatcher.group(1));
        while (literalMatcher.find()) {
            assertTrue(runtimeForbiddenIds.add(literalMatcher.group(1)),
                    "Duplicate GameTest forbidden ID: " + literalMatcher.group(1));
        }
        assertEquals(forbidden, runtimeForbiddenIds,
                "The JUnit projection and runtime GameTest forbidden-ID lists drifted apart");
    }

    @Test
    void programmaticRecipesAndGuidePagesAreFullyAssigned() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        int generatedRecipeCount = 0;
        Set<String> groups = new HashSet<>();
        for (JsonElement element : matrix.getAsJsonArray("programmatic_recipe_groups")) {
            JsonObject group = element.getAsJsonObject();
            String id = group.get("id").getAsString();
            assertTrue(groups.add(id), "Duplicate programmatic recipe group: " + id);
            assertNonBlank(group, "legacy_source");
            String disposition = group.get("disposition").getAsString();
            assertDecision(disposition, id);
            String stage = group.get("stage").getAsString();
            assertStage(stage, id);
            assertNonEmpty(group.getAsJsonArray("acceptance_tests"), id);
            if (!disposition.equals("exclude")) {
                assertNonBlank(group, "target_type");
                assertNonBlank(group, "target_path_template");
                assertTrue(group.has("variants"), "Missing explicit variants: " + id);
                String targetTemplate = group.get("target_path_template").getAsString();
                JsonArray variants = group.getAsJsonArray("variants");
                if (stage.compareTo(CURRENT_COMPLETED_STAGE) <= 0) {
                    for (JsonElement variantElement : variants) {
                        String variant = variantElement.getAsString();
                        String targetPath = targetTemplate
                                .replace("{machine}_{variant}", variant)
                                .replace("{variant}", variant);
                        assertFalse(
                                targetPath.contains("{"),
                                "Unresolved target path template: " + id + ":" + targetPath
                        );
                        assertTrue(
                                Files.isRegularFile(Path.of("src/generated/resources").resolve(targetPath)),
                                "Missing generated recipe target: " + id + ":" + targetPath
                        );
                    }
                }
                int count = variants.size();
                assertTrue(count > 0, id);
                if (group.has("expected_recipe_count")) {
                    assertEquals(group.get("expected_recipe_count").getAsInt(), count, id);
                }
                generatedRecipeCount += count;
            } else {
                assertTrue(group.has("legacy_variants"), "Excluded group needs evidence inventory: " + id);
            }
        }
        assertEquals(262, generatedRecipeCount);

        JsonObject guides = matrix.getAsJsonObject("legacy_guides");
        List<String> inventory = strings(guides.getAsJsonArray("inventory"));
        assertEquals(52, inventory.size());
        assertEquals(52, new HashSet<>(inventory).size());

        JsonArray pages = guides.getAsJsonArray("pages");
        assertNotNull(pages);
        assertEquals(52, pages.size());
        Set<String> assignedLegacyPaths = new HashSet<>();
        Set<String> targetPageIds = new HashSet<>();
        Set<String> englishResources = new HashSet<>();
        Set<String> chineseResources = new HashSet<>();
        Set<String> excludedLegacyPaths = new HashSet<>();
        int rebuiltPageCount = 0;
        String sourceRoot = guides.get("source_root").getAsString();
        for (JsonElement element : pages) {
            JsonObject page = element.getAsJsonObject();
            assertNonBlank(page, "legacy_path");
            assertNonBlank(page, "source");
            assertNonBlank(page, "disposition");
            assertNonBlank(page, "stage");
            String disposition = page.get("disposition").getAsString();
            assertDecision(disposition, page.toString());
            assertStage(page.get("stage").getAsString(), page.toString());
            assertNonEmpty(page.getAsJsonArray("acceptance_tests"), page.toString());

            String legacyPath = page.get("legacy_path").getAsString();
            assertTrue(assignedLegacyPaths.add(legacyPath), "Duplicate legacy guide assignment: " + legacyPath);
            assertEquals(sourceRoot + "/" + legacyPath, page.get("source").getAsString());

            if (disposition.equals("exclude")) {
                assertFalse(page.has("target_page_id"), "Excluded guide has a target page: " + page);
                assertFalse(page.has("target_resources"), "Excluded guide has target resources: " + page);
                assertNonBlank(page, "evidence");
                assertNonBlank(page, "notes");
                assertTrue(excludedLegacyPaths.add(legacyPath), "Duplicate excluded guide: " + legacyPath);
                continue;
            }

            assertEquals("rebuild", disposition, "Retained guide pages must be rebuilt");
            rebuiltPageCount++;
            assertNonBlank(page, "target_page_id");
            String targetPageId = page.get("target_page_id").getAsString();
            assertTrue(GUIDE_PAGE_ID.matcher(targetPageId).matches(), "Invalid guide page ID: " + targetPageId);
            assertTrue(targetPageIds.add(targetPageId), "Duplicate target guide page: " + targetPageId);
            assertTrue(page.has("target_resources") && page.get("target_resources").isJsonObject(),
                    "Missing target_resources: " + page);
            JsonObject resources = page.getAsJsonObject("target_resources");
            assertNonBlank(resources, "en_us");
            assertNonBlank(resources, "zh_cn");
            String semanticPath = targetPageId.substring(targetPageId.indexOf(':') + 1);
            String englishResource = resources.get("en_us").getAsString();
            String chineseResource = resources.get("zh_cn").getAsString();
            assertEquals("assets/magneticraft/guide/en_us/" + semanticPath + ".md", englishResource);
            assertEquals("assets/magneticraft/guide/zh_cn/" + semanticPath + ".md", chineseResource);
            assertTrue(englishResources.add(englishResource), "Duplicate en_us guide resource: " + englishResource);
            assertTrue(chineseResources.add(chineseResource), "Duplicate zh_cn guide resource: " + chineseResource);
            Path englishPath = Path.of("src/main/resources").resolve(englishResource);
            Path chinesePath = Path.of("src/main/resources").resolve(chineseResource);
            assertTrue(Files.isRegularFile(englishPath), "Missing en_us guide page: " + englishResource);
            assertTrue(Files.isRegularFile(chinesePath), "Missing zh_cn guide page: " + chineseResource);
            String english = Files.readString(englishPath, StandardCharsets.UTF_8);
            String chinese = Files.readString(chinesePath, StandardCharsets.UTF_8);
            assertTrue(english.startsWith("# ") && english.length() >= 80, "Incomplete en_us guide: " + englishResource);
            assertTrue(chinese.startsWith("# ") && chinese.length() >= 80, "Incomplete zh_cn guide: " + chineseResource);
            assertFalse(english.toUpperCase(Locale.ROOT).contains("TODO"), "TODO in en_us guide: " + englishResource);
            assertFalse(chinese.toUpperCase(Locale.ROOT).contains("TODO"), "TODO in zh_cn guide: " + chineseResource);
            assertFalse(english.equals(chinese), "Guide locales are not independently authored: " + semanticPath);
        }
        assertEquals(new HashSet<>(inventory), assignedLegacyPaths);
        assertEquals(51, rebuiltPageCount);
        assertEquals(Set.of("machines/utilities/6-copper-tank.md"), excludedLegacyPaths);
        assertFalse(Files.exists(Path.of(
                "src/main/resources/assets/magneticraft/guide/en_us/machines/utilities/copper_tank.md"
        )));
        assertFalse(Files.exists(Path.of(
                "src/main/resources/assets/magneticraft/guide/zh_cn/machines/utilities/copper_tank.md"
        )));
        assertEquals(51, targetPageIds.size());
        assertEquals(51, englishResources.size());
        assertEquals(51, chineseResources.size());
        assertTrue(targetPageIds.contains("magneticraft:machines/generators/combustion_chamber"));
        assertTrue(targetPageIds.contains("magneticraft:machines/processing/industrial_combustion_chamber"));

        assertEquals("rebuild", guides.get("disposition").getAsString());
        assertEquals("0.8.0", guides.get("stage").getAsString());
        assertNonBlank(guides, "target_rule");
        assertNonEmpty(guides.getAsJsonArray("acceptance_tests"), guides.toString());
    }

    @Test
    void everyBehaviorContractHasEvidenceStageAndAcceptanceCases() throws IOException {
        JsonArray contracts = readObject(MATRIX_PATH).getAsJsonArray("behavior_contracts");
        assertTrue(contracts.size() >= 20, "The behavior matrix is unexpectedly narrow");
        Set<String> ids = new HashSet<>();
        for (JsonElement element : contracts) {
            JsonObject contract = element.getAsJsonObject();
            String id = contract.get("id").getAsString();
            assertTrue(ids.add(id), "Duplicate behavior contract: " + id);
            assertDecision(contract.get("disposition").getAsString(), id);
            String stage = contract.get("stage").getAsString();
            assertStage(stage, id);
            assertNonBlank(contract, "contract");
            assertNonEmpty(contract.getAsJsonArray("legacy_sources"), id);
            assertNonEmpty(contract.getAsJsonArray("acceptance_tests"), id);
            if (stage.equals("0.3.0") || stage.equals("0.4.0") || stage.equals("0.5.0")) {
                assertNonBlank(contract, "evidence");
                String[] evidence = contract.get("evidence").getAsString().split("#", 2);
                assertEquals(2, evidence.length, stage + " evidence must include a stable heading anchor: " + id);
                Path evidencePath = Path.of(evidence[0]);
                assertTrue(Files.isRegularFile(evidencePath), "Missing " + stage + " evidence document: " + evidencePath);
                assertTrue(Files.readString(evidencePath).contains("## " + evidence[1]),
                        "Missing " + stage + " evidence heading for " + id + ": " + evidence[1]);
            }
        }
        assertTrue(ids.contains("electricity.long_distance_network"));
        assertTrue(ids.contains("computer.outbound_network"));
        assertTrue(ids.contains("integrations.jade_privacy"));
    }

    @Test
    void ignoredLegacySnapshotMatchesThePinnedManifestsWhenPresent() throws IOException {
        JsonObject matrix = readObject(MATRIX_PATH);
        if (Files.isDirectory(LEGACY_RECIPE_ROOT)) {
            List<String> inventory = strings(
                    matrix.getAsJsonObject("legacy_json_recipes").getAsJsonArray("inventory")
            );
            Set<String> actual = new HashSet<>();
            try (Stream<Path> paths = Files.list(LEGACY_RECIPE_ROOT)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString())
                        .forEach(actual::add);
            }
            assertEquals(
                    new HashSet<>(inventory),
                    actual
            );
            assertEquals(LEGACY_RECIPE_CONTENT_MANIFEST_SHA256,
                    contentManifestSha256(LEGACY_RECIPE_ROOT, inventory));
        }

        if (Files.isDirectory(LEGACY_GUIDE_ROOT)) {
            List<String> inventory = strings(matrix.getAsJsonObject("legacy_guides").getAsJsonArray("inventory"));
            Set<String> actual = new HashSet<>();
            try (Stream<Path> paths = Files.walk(LEGACY_GUIDE_ROOT)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".md"))
                        .map(LEGACY_GUIDE_ROOT::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/'))
                        .forEach(actual::add);
            }
            assertEquals(
                    new HashSet<>(inventory),
                    actual
            );
            assertEquals(LEGACY_GUIDE_CONTENT_MANIFEST_SHA256,
                    contentManifestSha256(LEGACY_GUIDE_ROOT, inventory));
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path.toString());
        JsonElement element = JsonParser.parseString(Files.readString(path));
        assertTrue(element.isJsonObject(), path.toString());
        return element.getAsJsonObject();
    }

    private static List<String> strings(JsonArray array) {
        assertNotNull(array);
        List<String> values = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static Set<String> extractNamespacedIds(Pattern pattern, Path... sources) throws IOException {
        Set<String> ids = new HashSet<>();
        for (Path source : sources) {
            assertTrue(Files.isRegularFile(source), source.toString());
            Matcher matcher = pattern.matcher(Files.readString(source));
            while (matcher.find()) {
                ids.add("magneticraft:" + matcher.group(1));
            }
        }
        return ids;
    }

    private static Set<String> behaviorContractIds(JsonObject matrix) {
        Set<String> ids = new HashSet<>();
        for (JsonElement element : matrix.getAsJsonArray("behavior_contracts")) {
            String id = element.getAsJsonObject().get("id").getAsString();
            assertTrue(ids.add(id), "Duplicate behavior contract: " + id);
        }
        return ids;
    }

    private static void collectAcceptanceTests(JsonElement element, List<String> acceptanceIds) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectAcceptanceTests(child, acceptanceIds);
            }
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("acceptance_tests")) {
                assertTrue(entry.getValue().isJsonArray(), "acceptance_tests must be an array");
                JsonArray tests = entry.getValue().getAsJsonArray();
                assertNonEmpty(tests, entry.getValue().toString());
                for (JsonElement test : tests) {
                    assertTrue(test.isJsonPrimitive() && test.getAsJsonPrimitive().isString(),
                            "Acceptance case ID must be a string: " + test);
                    assertFalse(test.getAsString().isBlank(), "Blank acceptance case ID");
                    acceptanceIds.add(test.getAsString());
                }
            } else {
                collectAcceptanceTests(entry.getValue(), acceptanceIds);
            }
        }
    }

    private static void assertPinnedManifest(
            JsonObject manifest,
            String fieldPrefix,
            int expectedCount,
            String expectedSha256,
            List<String> inventory
    ) {
        assertEquals(expectedCount, manifest.get(fieldPrefix + "_count").getAsInt(), fieldPrefix);
        assertEquals(expectedSha256, manifest.get(fieldPrefix + "_sha256").getAsString(), fieldPrefix);
        assertEquals(expectedCount, inventory.size(), fieldPrefix);
        assertEquals(expectedSha256, inventorySha256(inventory), fieldPrefix);
    }

    private static void assertPinnedHash(
            JsonObject manifest,
            String field,
            String expectedSha256,
            List<String> inventory
    ) {
        assertEquals(expectedSha256, manifest.get(field).getAsString(), field);
        assertEquals(expectedSha256, inventorySha256(inventory), field);
    }

    private static String canonicalLeaf(String registry, String legacyId, String variant) {
        String canonicalVariant = variant.isBlank() ? "default" : variant;
        return registry + ":" + legacyId + "#" + canonicalVariant;
    }

    private static void assertDirectContract(JsonObject contract, Set<String> behaviorIds, String context) {
        assertNotNull(contract, context);
        JsonArray dataPaths = contract.getAsJsonArray("data_paths");
        assertNonEmpty(dataPaths, context);
        for (String dataPath : strings(dataPaths)) {
            assertFalse(dataPath.isBlank(), "Blank data path for " + context);
            assertFalse(dataPath.contains("\\"), "Data path must use forward slashes: " + dataPath);
        }
        JsonArray contractIds = contract.getAsJsonArray("behavior_contract_ids");
        assertNonEmpty(contractIds, context);
        for (String contractId : strings(contractIds)) {
            assertTrue(behaviorIds.contains(contractId),
                    "Unknown behavior contract for " + context + ": " + contractId);
        }
    }

    private static Set<String> supportingIds(JsonObject idMap, String registry) {
        Set<String> ids = new HashSet<>();
        for (JsonElement element : idMap.getAsJsonArray("supporting_registry_mappings")) {
            JsonObject mapping = element.getAsJsonObject();
            if (mapping.get("registry").getAsString().equals(registry)) {
                ids.add(mapping.get("target_id").getAsString());
            }
        }
        return ids;
    }

    private static String namespaced(String path) {
        return "magneticraft:" + path;
    }

    private static void assertCoveredByProjection(
            Set<String> currentIds,
            Set<String> projectedIds,
            String registry
    ) {
        currentIds.forEach(MigrationMatrixContractTest::assertNormalizedTargetId);
        Set<String> unprojected = new TreeSet<>(currentIds);
        unprojected.removeAll(projectedIds);
        assertTrue(unprojected.isEmpty(), "Current " + registry + " IDs missing from projection: " + unprojected);
    }

    private static void assertProfileContract(
            JsonObject contract,
            JsonObject profiles,
            Set<String> behaviorIds,
            String context
    ) {
        assertNonBlank(contract, "data_path_profile");
        String profileId = contract.get("data_path_profile").getAsString();
        assertTrue(profiles.has(profileId), "Unknown data path profile for " + context + ": " + profileId);
        JsonArray paths = profiles.getAsJsonArray(profileId);
        assertNonEmpty(paths, context + ":" + profileId);
        for (String path : strings(paths)) {
            assertFalse(path.isBlank(), "Blank data path in profile " + profileId);
            assertFalse(path.contains("\\"), "Data path must use forward slashes: " + path);
        }

        JsonArray contractIds = contract.getAsJsonArray("behavior_contract_ids");
        assertNonEmpty(contractIds, context);
        for (String contractId : strings(contractIds)) {
            assertTrue(behaviorIds.contains(contractId),
                    "Unknown behavior contract for " + context + ": " + contractId);
        }
    }

    private static String resourcePath(String id) {
        assertNormalizedTargetId(id);
        return id.substring(id.indexOf(':') + 1);
    }

    private static String inventorySha256(List<String> inventory) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (String.join("\n", inventory) + "\n").getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static String contentManifestSha256(Path root, List<String> inventory) throws IOException {
        try {
            MessageDigest manifest = MessageDigest.getInstance("SHA-256");
            for (String relativePath : inventory) {
                Path file = root.resolve(relativePath);
                assertTrue(Files.isRegularFile(file), "Missing pinned legacy source: " + file);
                String entry = relativePath + "\n" + sha256(Files.readAllBytes(file)) + "\n";
                manifest.update(entry.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(manifest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static void assertNormalizedTargetId(String id) {
        var matcher = RESOURCE_LOCATION.matcher(id);
        assertTrue(matcher.matches(), "Non-normalized target ID: " + id);
        if (id.startsWith("magneticraft:")) {
            Set<String> tokens = Set.of(matcher.group(1).split("_"));
            for (String forbidden : FORBIDDEN_ID_TOKENS) {
                assertFalse(tokens.contains(forbidden), "Ambiguous token in target ID: " + id);
            }
        }
    }

    private static void assertDecision(String decision, String context) {
        assertTrue(DECISIONS.contains(decision), context + ": " + decision);
    }

    private static void assertStage(String stage, String context) {
        assertTrue(stage.matches("(?:0\\.[2-9]\\.0|1\\.0\\.0)"), context + ": " + stage);
    }

    private static void assertNonBlank(JsonObject object, String field) {
        assertTrue(object.has(field), "Missing " + field + ": " + object);
        assertFalse(object.get(field).getAsString().isBlank(), "Blank " + field + ": " + object);
    }

    private static void assertNonEmpty(JsonArray array, String context) {
        assertNotNull(array, context);
        assertTrue(array.size() > 0, context);
    }
}
