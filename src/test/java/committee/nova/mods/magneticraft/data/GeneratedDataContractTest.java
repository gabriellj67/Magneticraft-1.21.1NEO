package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedDataContractTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ASSETS = GENERATED.resolve("assets/magneticraft");
    private static final Path DATA = GENERATED.resolve("data/magneticraft");
    private static final Path SOURCE_TEXTURES = Path.of("src/main/resources/assets/magneticraft/textures");
    private static final Path SOURCE_MODELS = Path.of("src/main/resources/assets/magneticraft/models");

    @Test
    void everyGeneratedJsonFileIsValid() throws IOException {
        try (Stream<Path> paths = Files.walk(GENERATED)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
                assertNotNull(readJson(path), path.toString());
            }
        }
    }

    @Test
    void generatedModelsTranslationsAndSourceTexturesCoverTheCatalogues() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        JsonObject blockAtlas = readObject(GENERATED.resolve("assets/minecraft/atlases/blocks.json"));
        Set<String> atlasSprites = new HashSet<>();
        blockAtlas.getAsJsonArray("sources").forEach(source ->
                atlasSprites.add(source.getAsJsonObject().get("resource").getAsString())
        );
        assertEquals(english.keySet(), chinese.keySet(), "Language keys must remain symmetric");

        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            assertFile(ASSETS.resolve("blockstates/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/block/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/item/" + definition.id() + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + definition.id() + ".json"));
            assertPng(SOURCE_TEXTURES.resolve("block/" + definition.id() + ".png"));
            assertTrue(english.has("block.magneticraft." + definition.id()), definition.id());
        }

        for (MaterialForm form : MaterialForm.values()) {
            for (Metal metal : Metal.values()) {
                if (form.appliesTo(metal)) {
                    assertItemAssetsAndTranslation(form.id(metal), english);
                }
            }
        }
        for (CraftingComponent component : CraftingComponent.values()) {
            assertItemAssetsAndTranslation(component.id(), english);
        }
        for (HammerType type : HammerType.values()) {
            assertItemAssetsAndTranslation(type.id(), english);
        }

        for (FluidDefinition definition : FluidDefinition.values()) {
            assertFile(ASSETS.resolve("blockstates/" + definition.id() + ".json"));
            assertFile(ASSETS.resolve("models/block/" + definition.id() + ".json"));
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_still.png"));
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_flow.png"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_still.png.mcmeta"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.id() + "_flow.png.mcmeta"));
            JsonObject bucketModel = readObject(ASSETS.resolve("models/item/" + definition.id() + "_bucket.json"));
            assertEquals("forge:fluid_container", bucketModel.get("loader").getAsString());
            assertEquals("magneticraft:" + definition.id(), bucketModel.get("fluid").getAsString());
            assertTrue(english.has(definition.translationKey()), definition.translationKey());
            assertTrue(english.has("item.magneticraft." + definition.id() + "_bucket"), definition.id());
            assertFile(GENERATED.resolve("data/magneticraft/tags/fluids/" + definition.id() + ".json"));
            assertFile(GENERATED.resolve("data/forge/tags/fluids/" + definition.id() + ".json"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.id() + "_still"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.id() + "_flow"));
        }
        assertEquals(FluidDefinition.values().length * 2, atlasSprites.size());
    }

    @Test
    void generatedRecipesCoverEveryLegacyBaseRecipe() throws IOException {
        Set<Path> expectedCrafting = new HashSet<>();
        for (Metal metal : Metal.values()) {
            if (MaterialForm.NUGGET.appliesTo(metal)) {
                expectedCrafting.add(recipe("crafting/" + metal.id() + "_ingot_from_material"));
                expectedCrafting.add(recipe("crafting/" + metal.id() + "_ingot_to_material"));
            }
        }
        for (String storage : Set.of("lead_block", "cobalt_block", "tungsten_block", "sulfur_block")) {
            expectedCrafting.add(recipe("crafting/" + storage + "_from_material"));
            expectedCrafting.add(recipe("crafting/" + storage + "_to_material"));
        }
        for (String recipe : Set.of(
                "limestone_bricks",
                "burnt_limestone_bricks",
                "limestone_tiles",
                "inverted_limestone_tiles",
                "alternator",
                "motor",
                "fine_copper_wire",
                "magnet",
                "iron_mesh",
                "fabric_mesh",
                "stone_hammer",
                "iron_hammer",
                "steel_hammer"
        )) {
            expectedCrafting.add(recipe("crafting/" + recipe));
        }
        assertEquals(45, expectedCrafting.size());
        expectedCrafting.forEach(GeneratedDataContractTest::assertFile);

        Set<Path> expectedSmelting = new HashSet<>();
        for (String ore : Set.of("galena_ore", "cobalt_ore", "tungsten_ore")) {
            expectedSmelting.add(recipe("smelting/" + ore));
        }
        for (Metal metal : Metal.values()) {
            if (MaterialForm.DUST.appliesTo(metal)) {
                expectedSmelting.add(recipe("smelting/" + metal.id() + "_dust"));
            }
            if (MaterialForm.ROCKY_CHUNK.appliesTo(metal)) {
                expectedSmelting.add(recipe("smelting/" + metal.id() + "_rocky_chunk"));
            }
            if (MaterialForm.CHUNK.appliesTo(metal)) {
                Path recipe = recipe("smelting/" + metal.id() + "_chunk");
                expectedSmelting.add(recipe);
                assertEquals(2, readObject(recipe).getAsJsonObject("result").get("count").getAsInt(), recipe.toString());
            }
        }
        expectedSmelting.add(recipe("smelting/limestone"));
        expectedSmelting.add(recipe("smelting/limestone_cobblestone"));
        assertEquals(46, expectedSmelting.size());
        expectedSmelting.forEach(GeneratedDataContractTest::assertFile);

        JsonObject galena = readObject(recipe("smelting/galena_rocky_chunk"));
        assertEquals(2, galena.getAsJsonObject("result").get("count").getAsInt());
        for (Path path : expectedSmelting) {
            assertEquals("minecraft:smelting", readObject(path).get("type").getAsString(), path.toString());
            assertFalse(path.toString().contains("blasting"), path.toString());
        }
    }

    @Test
    void machineFrameworkDataAndAssetsAreComplete() throws IOException {
        for (String block : Set.of("crushing_table", "battery_box", "iron_grate", "electric_furnace")) {
            assertFile(ASSETS.resolve("blockstates/" + block + ".json"));
            assertFile(ASSETS.resolve("models/block/" + block + ".json"));
            assertFile(ASSETS.resolve("models/item/" + block + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + block + ".json"));
        }
        for (String texture : Set.of(
                "crushing_table_top", "crushing_table_side", "crushing_table_bottom",
                "battery_box", "iron_grate", "electric_furnace_side", "electric_furnace_front",
                "electric_furnace_front_on"
        )) {
            assertPng(SOURCE_TEXTURES.resolve("block/" + texture + ".png"));
        }
        assertLegacyObjModel("battery_box", "battery_box");
        assertItemAssetsAndTranslation("low_voltage_battery", readObject(ASSETS.resolve("lang/en_us.json")));

        for (String recipe : Set.of(
                "crushing_table", "low_voltage_battery", "iron_grate", "battery_box", "electric_furnace"
        )) {
            assertFile(recipe("crafting/" + recipe));
        }
        JsonObject electricFurnace = readObject(recipe("crafting/electric_furnace"));
        assertEquals(
                "magneticraft:brick_furnace",
                electricFurnace.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString()
        );
        assertFalse(Files.exists(recipe("crafting/electric_furnace_temporary")));

        Path crushingDirectory = DATA.resolve("recipes/crushing_table");
        try (Stream<Path> paths = Files.list(crushingDirectory)) {
            assertTrue(paths.filter(Files::isRegularFile).count() >= 20, "Legacy crushing catalogue is incomplete");
        }
        JsonObject blazeRod = readObject(recipe("crushing_table/blaze_rod"));
        assertEquals("magneticraft:crushing_table", blazeRod.get("type").getAsString());
        assertEquals(5, blazeRod.getAsJsonObject("result").get("count").getAsInt());

        JsonObject sounds = readObject(Path.of("src/main/resources/assets/magneticraft/sounds.json"));
        assertEquals(3, sounds.getAsJsonObject("crushing_table_hit").getAsJsonArray("sounds").size());
        assertEquals(2, sounds.getAsJsonObject("crushing_table_complete").getAsJsonArray("sounds").size());
        for (String sample : Set.of(
                "crushing_table_hit1", "crushing_table_hit2", "crushing_table_hit3",
                "crushing_table_complete1", "crushing_table_complete2"
        )) {
            assertFile(Path.of("src/main/resources/assets/magneticraft/sounds/" + sample + ".ogg"));
        }
    }

    @Test
    void physicalNetworkDataAndAssetsAreComplete() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        Set<String> blocks = Set.of(
                "electric_cable",
                "heat_pipe",
                "insulated_heat_pipe",
                "heat_sink",
                "iron_fluid_pipe",
                "pneumatic_tube",
                "pneumatic_restriction_tube",
                "conveyor_belt"
        );
        for (String block : blocks) {
            assertFile(ASSETS.resolve("blockstates/" + block + ".json"));
            assertFile(ASSETS.resolve("models/block/" + block + ".json"));
            assertFile(ASSETS.resolve("models/item/" + block + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + block + ".json"));
            assertFile(recipe("crafting/" + block));
            assertTrue(english.has("block.magneticraft." + block), block);
            assertTrue(chinese.has("block.magneticraft." + block), block);
        }
        assertLegacyObjModel("heat_sink", "heat_sink");
        JsonObject heatSinkVariants = readObject(ASSETS.resolve("blockstates/heat_sink.json"))
                .getAsJsonObject("variants");
        assertFalse(heatSinkVariants.getAsJsonObject("facing=down").has("x"));
        assertEquals(180, heatSinkVariants.getAsJsonObject("facing=up").get("x").getAsInt());
        assertEquals(270, heatSinkVariants.getAsJsonObject("facing=north").get("x").getAsInt());
        JsonObject conveyorRecipe = readObject(recipe("crafting/conveyor_belt"));
        assertEquals(
                List.of("BAB", "BCB", "B B"),
                conveyorRecipe.getAsJsonArray("pattern").asList().stream()
                        .map(JsonElement::getAsString)
                        .toList()
        );
        assertEquals(
                "magneticraft:light_plates/iron",
                conveyorRecipe.getAsJsonObject("key").getAsJsonObject("A").get("tag").getAsString()
        );
        assertEquals(
                "forge:ingots/iron",
                conveyorRecipe.getAsJsonObject("key").getAsJsonObject("B").get("tag").getAsString()
        );
        assertEquals(
                "magneticraft:motor",
                conveyorRecipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString()
        );
        assertEquals(12, conveyorRecipe.getAsJsonObject("result").get("count").getAsInt());
        JsonObject pneumaticTube = readObject(recipe("crafting/pneumatic_tube"));
        assertEquals(List.of("AGA"), pneumaticTube.getAsJsonArray("pattern").asList().stream()
                .map(JsonElement::getAsString)
                .toList());
        assertEquals(
                "magneticraft:light_plates/copper",
                pneumaticTube.getAsJsonObject("key").getAsJsonObject("A").get("tag").getAsString()
        );
        assertEquals(8, pneumaticTube.getAsJsonObject("result").get("count").getAsInt());
        JsonObject restrictionTube = readObject(recipe("crafting/pneumatic_restriction_tube"));
        assertEquals("minecraft:crafting_shapeless", restrictionTube.get("type").getAsString());
        assertEquals(2, restrictionTube.getAsJsonArray("ingredients").size());
        JsonObject pneumaticFilter = readObject(recipe("crafting/pneumatic_filter"));
        assertEquals(
                "magneticraft:iron_mesh",
                pneumaticFilter.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString()
        );
        JsonObject wrench = readObject(ASSETS.resolve("models/item/wrench.json"));
        assertEquals("minecraft:item/handheld", wrench.get("parent").getAsString());
        assertEquals("minecraft:item/iron_hoe", wrench.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(english.has("item.magneticraft.wrench"));
        assertTrue(chinese.has("item.magneticraft.wrench"));
        assertFile(recipe("crafting/wrench"));

        JsonObject wrenchTag = readObject(GENERATED.resolve("data/forge/tags/items/tools/wrenches.json"));
        assertTrue(wrenchTag.getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("magneticraft:wrench")));
        for (String key : Set.of(
                "message.magneticraft.connection_enabled",
                "message.magneticraft.connection_disabled",
                "message.magneticraft.redstone_mode",
                "message.magneticraft.fluid_side_mode"
        )) {
            assertTrue(english.has(key), key);
            assertTrue(chinese.has(key), key);
        }
    }

    @Test
    void singleBlockMachineDataRecipesAndAssetsAreComplete() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            String id = definition.id();
            assertFile(ASSETS.resolve("blockstates/" + id + ".json"));
            assertFile(ASSETS.resolve("models/block/" + id + ".json"));
            assertFile(ASSETS.resolve("models/item/" + id + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + id + ".json"));
            assertTrue(english.has("block.magneticraft." + id), id);
            assertTrue(chinese.has("block.magneticraft." + id), id);
            if (definition.hasMenu()) {
                assertTrue(english.has("container.magneticraft." + id), id);
                assertTrue(chinese.has("container.magneticraft." + id), id);
            }
            if (definition != SingleBlockMachineDefinition.INFINITE_ENERGY) {
                assertFile(recipe("crafting/" + id));
            }
        }
        for (var guide : AdvancedGuideDataProvider.singleBlockMachineGuides()) {
            assertFile(ASSETS.resolve("guide/machines/" + guide.id() + ".json"));
            for (JsonObject language : List.of(english, chinese)) {
                assertTrue(language.has(guide.translationKey()), guide.translationKey());
                assertTrue(language.has(guide.descriptionKey()), guide.descriptionKey());
                assertTrue(language.has("gui.magneticraft.guide.category." + guide.category()), guide.id());
                assertTrue(language.has("gui.magneticraft.guide.redstone." + guide.redstoneControl()), guide.id());
                assertTrue(language.has("gui.magneticraft.guide.processing." + guide.processingKind()), guide.id());
                assertTrue(language.has("gui.magneticraft.guide.automation." + guide.automationProfile()), guide.id());
                for (String role : guide.slotRoles()) {
                    assertTrue(language.has("gui.magneticraft.guide.slot." + role), guide.id() + ":" + role);
                }
                for (String port : guide.physicalPorts()) {
                    assertTrue(language.has("gui.magneticraft.guide.port." + port), guide.id() + ":" + port);
                }
            }
        }
        for (SingleBlockMachineDefinition endpoint : Set.of(
                SingleBlockMachineDefinition.RELAY,
                SingleBlockMachineDefinition.FILTER,
                SingleBlockMachineDefinition.TRANSPOSER
        )) {
            String id = endpoint.id();
            JsonObject variants = readObject(ASSETS.resolve("blockstates/" + id + ".json"))
                    .getAsJsonObject("variants");
            Set<String> facings = variants.keySet().stream()
                    .map(key -> key.split(","))
                    .flatMap(java.util.Arrays::stream)
                    .filter(property -> property.startsWith("facing="))
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(6, facings.size(), id + " must render all six endpoint facings");

            JsonObject textures = readObject(ASSETS.resolve("models/block/" + id + ".json"))
                    .getAsJsonObject("textures");
            assertTrue(textures.has("up") && textures.has("down") && textures.has("east"),
                    id + " must distinguish front, back and side faces");
            assertFalse(textures.get("up").equals(textures.get("down")), id + " front/back texture");
            assertFalse(textures.get("up").equals(textures.get("east")), id + " front/side texture");
        }
        for (String status : Set.of("blocked", "unloaded", "filter_rejected")) {
            String key = "message.magneticraft.pneumatic_endpoint." + status;
            assertTrue(english.has(key), key);
            assertTrue(chinese.has(key), key);
        }
        for (String id : Set.of("tube_light", "inserter_speed_upgrade", "inserter_stack_upgrade")) {
            assertFile(ASSETS.resolve("models/item/" + id + ".json"));
            assertTrue(english.has((id.equals("tube_light") ? "block" : "item") + ".magneticraft." + id), id);
        }
        assertFile(ASSETS.resolve("blockstates/air_bubble.json"));
        assertFalse(Files.exists(ASSETS.resolve("models/item/air_bubble.json")));
        assertFalse(Files.exists(DATA.resolve("loot_tables/blocks/air_bubble.json")));

        assertRecipeDirectory("sluice_box", 16, "magneticraft:sluice_box");
        assertRecipeDirectory("gasification_unit", 28, "magneticraft:gasification_unit");
        assertRecipeDirectory("thermopile", 33, "magneticraft:thermopile");
        assertRecipeDirectory("fluid_fuel", 10, "magneticraft:fluid_fuel");
        JsonObject sand = readObject(recipe("sluice_box/sand"));
        assertEquals(9, sand.getAsJsonArray("results").size());
        JsonObject log = readObject(recipe("gasification_unit/00_logs"));
        assertEquals("minecraft:charcoal", log.getAsJsonObject("item_result").get("item").getAsString());
        assertEquals(150, log.getAsJsonObject("fluid_result").get("amount").getAsInt());
        JsonObject snow = readObject(recipe("thermopile/snow_layer_8"));
        assertEquals("8", snow.getAsJsonObject("state").get("layers").getAsString());
        JsonObject diesel = readObject(recipe("fluid_fuel/diesel"));
        assertEquals(10_000, diesel.get("duration").getAsInt());
        assertEquals(80.0D, diesel.get("power").getAsDouble());
    }

    @Test
    void advancedSystemsDataAssetsAndGuidesAreComplete() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));

        for (String part : Set.of(
                "machine_casing",
                "corrugated_iron",
                "copper_coil",
                "machine_support_column",
                "striped_machine_casing",
                "electrical_machine_casing"
        )) {
            assertFile(ASSETS.resolve("blockstates/" + part + ".json"));
            assertFile(ASSETS.resolve("models/block/" + part + ".json"));
            assertFile(ASSETS.resolve("models/item/" + part + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + part + ".json"));
            assertFile(recipe("crafting/" + part));
            assertTrue(english.has("block.magneticraft." + part), part);
            assertTrue(chinese.has("block.magneticraft." + part), part);
        }

        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            String id = definition.id();
            JsonObject blockState = readObject(ASSETS.resolve("blockstates/" + id + ".json"));
            assertEquals(8, blockState.getAsJsonObject("variants").size(), id + " state coverage");
            assertFile(ASSETS.resolve("models/block/" + id + ".json"));
            assertFile(ASSETS.resolve("models/block/" + id + "_formed.json"));
            assertFile(ASSETS.resolve("models/item/" + id + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + id + ".json"));
            assertFile(recipe("crafting/" + id));
            assertFile(ASSETS.resolve("guide/multiblocks/" + id + ".json"));
            assertTrue(english.has("block.magneticraft." + id), id);
            assertTrue(chinese.has("block.magneticraft." + id), id);
        }
        assertObjModel("grinder", "grinder_block", "grinder");
        assertObjModel("grinder_formed", "grinder_block", "grinder");
        assertObjModel("hydraulic_press", "hydraulic_press_base", "hydraulic_press");
        assertObjModel("hydraulic_press_formed", "hydraulic_press_base", "hydraulic_press");
        assertObjModel("solar_panel", "solar_panel_base", "solar_panel");
        assertObjModel("solar_panel_formed", "solar_panel_base", "solar_panel");

        assertFile(ASSETS.resolve("blockstates/oil_deposit.json"));
        assertFile(ASSETS.resolve("models/block/oil_deposit.json"));
        assertFile(ASSETS.resolve("models/item/oil_deposit.json"));
        assertFalse(Files.exists(DATA.resolve("loot_tables/blocks/oil_deposit.json")));

        for (String block : Set.of("computer", "mining_robot")) {
            assertFile(ASSETS.resolve("blockstates/" + block + ".json"));
            assertEquals(
                    4,
                    readObject(ASSETS.resolve("blockstates/" + block + ".json"))
                            .getAsJsonObject("variants")
                            .size(),
                    block + " facing coverage"
            );
            assertFile(ASSETS.resolve("models/block/" + block + ".json"));
            assertFile(ASSETS.resolve("models/item/" + block + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + block + ".json"));
            assertFile(recipe("crafting/" + block));
            assertTrue(english.has("block.magneticraft." + block), block);
            assertTrue(chinese.has("block.magneticraft." + block), block);
        }
        assertLegacyObjModel("computer", "computer_body");
        assertFile(ASSETS.resolve("models/item/floppy_disk.json"));
        assertFile(recipe("crafting/floppy_disk"));
        assertTrue(english.has("item.magneticraft.floppy_disk"));
        assertTrue(chinese.has("item.magneticraft.floppy_disk"));

        for (AdvancedWorldgenProvider.DepositDefinition deposit : AdvancedWorldgenProvider.deposits()) {
            assertFile(DATA.resolve("worldgen/configured_feature/" + deposit.id() + ".json"));
            assertFile(DATA.resolve("worldgen/placed_feature/" + deposit.id() + ".json"));
            assertFile(DATA.resolve("forge/biome_modifier/" + deposit.id() + ".json"));
        }
        assertFile(DATA.resolve("structures/advanced_systems.nbt"));

        Map<String, String> processingMachines = Map.of(
                "grinder_cobblestone", "grinder",
                "sieve_gravel", "sieve",
                "hydraulic_press_iron_light_plate", "hydraulic_press"
        );
        for (Map.Entry<String, String> entry : processingMachines.entrySet()) {
            JsonObject processing = readObject(recipe("advanced_processing/" + entry.getKey()));
            assertEquals("magneticraft:advanced_processing", processing.get("type").getAsString());
            assertEquals(entry.getValue(), processing.get("machine").getAsString());
            assertTrue(processing.get("duration").getAsInt() > 0);
            assertTrue(processing.get("energy_per_tick").getAsInt() >= 0);
            assertTrue(processing.getAsJsonArray("results").size() > 0);
        }

        JsonObject opcodeGuide = readObject(ASSETS.resolve("guide/computer_opcodes.json"));
        assertEquals(ComputerOpcode.values().length, opcodeGuide.getAsJsonArray("opcodes").size());
        for (ComputerOpcode opcode : ComputerOpcode.values()) {
            assertTrue(english.has(opcode.descriptionTranslationKey()), opcode.name());
            assertTrue(chinese.has(opcode.descriptionTranslationKey()), opcode.name());
        }
    }

    private static void assertRecipeDirectory(String directory, long expectedCount, String type) throws IOException {
        Path root = DATA.resolve("recipes/" + directory);
        try (Stream<Path> paths = Files.list(root)) {
            List<Path> recipes = paths.filter(Files::isRegularFile).toList();
            assertEquals(expectedCount, recipes.size(), directory);
            for (Path recipe : recipes) {
                assertEquals(type, readObject(recipe).get("type").getAsString(), recipe.toString());
            }
        }
    }

    private static void assertItemAssetsAndTranslation(String id, JsonObject language) throws IOException {
        assertPng(SOURCE_TEXTURES.resolve("item/" + id + ".png"));
        assertFile(ASSETS.resolve("models/item/" + id + ".json"));
        assertTrue(language.has("item.magneticraft." + id), id);
    }

    private static void assertObjModel(String generatedName, String sourceName, String textureName) throws IOException {
        assertFile(SOURCE_MODELS.resolve("block/" + sourceName + ".obj"));
        assertFile(SOURCE_MODELS.resolve("block/" + sourceName + ".mtl"));
        assertPng(SOURCE_TEXTURES.resolve("block/" + textureName + ".png"));

        JsonObject model = readObject(ASSETS.resolve("models/block/" + generatedName + ".json"));
        assertEquals("forge:obj", model.get("loader").getAsString());
        assertEquals(
                "magneticraft:models/block/" + sourceName + ".obj",
                model.get("model").getAsString()
        );
        assertEquals(
                "magneticraft:models/block/" + sourceName + ".mtl",
                model.get("mtl_override").getAsString()
        );
    }

    private static void assertLegacyObjModel(String generatedName, String artifactName) throws IOException {
        Path legacyModels = SOURCE_MODELS.resolve("block/legacy");
        assertFile(legacyModels.resolve(artifactName + ".obj"));
        assertFile(legacyModels.resolve(artifactName + ".mtl"));

        JsonObject model = readObject(ASSETS.resolve("models/block/" + generatedName + ".json"));
        assertEquals("forge:obj", model.get("loader").getAsString());
        assertEquals(
                "magneticraft:models/block/legacy/" + artifactName + ".obj",
                model.get("model").getAsString()
        );
        assertEquals(
                "magneticraft:models/block/legacy/" + artifactName + ".mtl",
                model.get("mtl_override").getAsString()
        );
    }

    private static Path recipe(String path) {
        return DATA.resolve("recipes/" + path + ".json");
    }

    private static JsonElement readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static JsonObject readObject(Path path) throws IOException {
        assertFile(path);
        return readJson(path).getAsJsonObject();
    }

    private static void assertPng(Path path) throws IOException {
        assertFile(path);
        assertNotNull(ImageIO.read(path.toFile()), "Unreadable PNG: " + path);
    }

    private static void assertFile(Path path) {
        assertTrue(Files.isRegularFile(path), "Missing file: " + path);
    }
}
