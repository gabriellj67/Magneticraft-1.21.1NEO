package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.OreBlockDefinition;
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
    private static final Set<String> BLOCK_DISPLAY_CONTEXTS = Set.of(
            "thirdperson_lefthand",
            "thirdperson_righthand",
            "firstperson_lefthand",
            "firstperson_righthand",
            "head",
            "gui",
            "ground",
            "fixed"
    );
    private static final Set<String> REQUIRED_BLOCK_DISPLAY_CONTEXTS = Set.of(
            "thirdperson_lefthand",
            "thirdperson_righthand",
            "firstperson_lefthand",
            "firstperson_righthand",
            "gui",
            "ground",
            "fixed"
    );

    @Test
    void everyGeneratedJsonFileIsValid() throws IOException {
        try (Stream<Path> paths = Files.walk(GENERATED)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
                assertNotNull(readJson(path), path.toString());
            }
        }
    }

    @Test
    void customSceneBlockItemsHaveBoundedModelLoaderTransforms() throws IOException {
        int customSceneBlockItems = 0;
        Path itemModels = ASSETS.resolve("models/item");
        try (Stream<Path> paths = Files.list(itemModels)) {
            for (Path itemPath : paths.filter(Files::isRegularFile).toList()) {
                JsonObject itemModel = readObject(itemPath);
                if (!itemModel.has("parent")) {
                    continue;
                }
                String parent = itemModel.get("parent").getAsString();
                if (!parent.startsWith("magneticraft:block/")) {
                    continue;
                }

                String blockModelName = parent.substring("magneticraft:block/".length());
                JsonObject blockModel = readObject(ASSETS.resolve("models/block/" + blockModelName + ".json"));
                if (!blockModel.has("loader")
                        || !"magneticraft:legacy_scene".equals(blockModel.get("loader").getAsString())) {
                    continue;
                }

                customSceneBlockItems++;
                assertFalse(blockModel.has("parent"), itemPath + " must own its legacy scene transforms");
                assertTrue(blockModel.has("display"), itemPath + " must declare block display transforms");
                JsonObject display = blockModel.getAsJsonObject("display");
                assertTrue(display.keySet().containsAll(REQUIRED_BLOCK_DISPLAY_CONTEXTS),
                        itemPath + " must preserve every non-identity ModelLoader block display context");
                assertTrue(BLOCK_DISPLAY_CONTEXTS.containsAll(display.keySet()),
                        itemPath + " declares an unknown block display context");
                JsonObject gui = display.getAsJsonObject("gui");
                assertVectorEquals(gui.getAsJsonArray("rotation"), 30.0F, 225.0F, 0.0F, itemPath + " rotation");

                for (String context : display.keySet()) {
                    JsonObject itemDisplay = display.getAsJsonObject(context);
                    if (itemDisplay.has("translation")) {
                        JsonArray translation = itemDisplay.getAsJsonArray("translation");
                        assertEquals(3, translation.size(), itemPath + " " + context + " translation");
                        translation.forEach(component -> assertTrue(
                                Math.abs(component.getAsFloat()) <= 80.0F,
                                itemPath + " " + context + " translation must be valid"
                        ));
                    }

                    JsonArray scale = itemDisplay.has("scale") ? itemDisplay.getAsJsonArray("scale") : null;
                    if (scale != null) {
                        assertEquals(3, scale.size(), itemPath + " " + context + " scale");
                    }
                    float uniformScale = scale == null ? 1.0F : scale.get(0).getAsFloat();
                    assertTrue(uniformScale > 0.0F && uniformScale <= 4.0F,
                            itemPath + " " + context + " scale must be valid");
                    if (scale != null) {
                        assertEquals(uniformScale, scale.get(1).getAsFloat(), 0.000001F,
                                itemPath + " " + context + " Y scale");
                        assertEquals(uniformScale, scale.get(2).getAsFloat(), 0.000001F,
                                itemPath + " " + context + " Z scale");
                    }
                }
            }
        }
        assertTrue(customSceneBlockItems > 0, "Expected generated custom-scene block items");
        assertTrue(guiScale("wind_turbine_inventory") < 0.1F, "Oversized turbine must fit its item slot");
        assertTrue(guiScale("electric_connector") > 1.0F, "Small connector must remain recognizable");

        JsonObject portableBattery = readObject(ASSETS.resolve("models/item/low_voltage_battery.json"));
        assertEquals("minecraft:item/generated", portableBattery.get("parent").getAsString());
        assertFalse(portableBattery.has("display"), "Non-block items must keep their flat item model contract");
    }

    private static void assertVectorEquals(
            JsonArray actual,
            float expectedX,
            float expectedY,
            float expectedZ,
            String message
    ) {
        assertNotNull(actual, message);
        assertEquals(3, actual.size(), message);
        assertEquals(expectedX, actual.get(0).getAsFloat(), 0.000001F, message + " X");
        assertEquals(expectedY, actual.get(1).getAsFloat(), 0.000001F, message + " Y");
        assertEquals(expectedZ, actual.get(2).getAsFloat(), 0.000001F, message + " Z");
    }

    private static float guiScale(String blockModelName) throws IOException {
        JsonObject model = readObject(ASSETS.resolve("models/block/" + blockModelName + ".json"));
        JsonObject gui = model.getAsJsonObject("display").getAsJsonObject("gui");
        return gui.has("scale") ? gui.getAsJsonArray("scale").get(0).getAsFloat() : 1.0F;
    }

    @Test
    void generatedModelsTranslationsAndSourceTexturesCoverTheCatalogues() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        JsonObject blockAtlas = readObject(GENERATED.resolve("assets/minecraft/atlases/blocks.json"));
        Set<String> atlasSprites = new HashSet<>();
        blockAtlas.getAsJsonArray("sources").forEach(source -> {
            JsonObject sourceObject = source.getAsJsonObject();
            if (sourceObject.has("resource")) {
                atlasSprites.add(sourceObject.get("resource").getAsString());
            }
        });
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
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.textureId() + "_still.png"));
            assertPng(SOURCE_TEXTURES.resolve("fluid/" + definition.textureId() + "_flow.png"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.textureId() + "_still.png.mcmeta"));
            assertFile(SOURCE_TEXTURES.resolve("fluid/" + definition.textureId() + "_flow.png.mcmeta"));
            JsonObject bucketModel = readObject(ASSETS.resolve("models/item/" + definition.id() + "_bucket.json"));
            assertEquals("forge:fluid_container", bucketModel.get("loader").getAsString());
            assertEquals("magneticraft:" + definition.id(), bucketModel.get("fluid").getAsString());
            assertTrue(english.has(definition.translationKey()), definition.translationKey());
            assertTrue(english.has("item.magneticraft." + definition.id() + "_bucket"), definition.id());
            assertFile(GENERATED.resolve("data/magneticraft/tags/fluids/" + definition.id() + ".json"));
            assertFile(GENERATED.resolve("data/forge/tags/fluids/" + definition.id() + ".json"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.textureId() + "_still"));
            assertTrue(atlasSprites.contains("magneticraft:fluid/" + definition.textureId() + "_flow"));
        }
        assertEquals(java.util.Arrays.stream(FluidDefinition.values())
                .map(FluidDefinition::textureId).distinct().count() * 2, atlasSprites.size());
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
        for (String storage : Set.of("lead_block", "cobalt_block", "tungsten_block", "carbide_block", "sulfur_block")) {
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
                "brass_dust",
                "carbide_ingot",
                "stone_hammer",
                "iron_hammer",
                "steel_hammer"
        )) {
            expectedCrafting.add(recipe("crafting/" + recipe));
        }
        assertEquals(49, expectedCrafting.size());
        expectedCrafting.forEach(GeneratedDataContractTest::assertFile);

        Set<Path> expectedSmelting = new HashSet<>();
        for (OreBlockDefinition ore : OreBlockDefinition.values()) {
            if (ore.processedMetal() != null) {
                expectedSmelting.add(recipe("smelting/" + ore.block().id()));
            }
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
        assertEquals(52, expectedSmelting.size());
        expectedSmelting.forEach(GeneratedDataContractTest::assertFile);

        JsonObject galena = readObject(recipe("smelting/galena_rocky_chunk"));
        assertEquals(2, galena.getAsJsonObject("result").get("count").getAsInt());
        for (Path path : expectedSmelting) {
            assertEquals("minecraft:smelting", readObject(path).get("type").getAsString(), path.toString());
            assertFalse(path.toString().contains("blasting"), path.toString());
        }
        JsonObject brassBlasting = readObject(recipe("blasting/brass_dust"));
        assertEquals("minecraft:blasting", brassBlasting.get("type").getAsString());
        assertEquals("magneticraft:brass_ingot", brassBlasting.getAsJsonObject("result").get("item").getAsString());

        JsonObject plastic = readObject(recipe("polymerizing/plastic_sheet"));
        assertEquals("magneticraft:polymerizing", plastic.get("type").getAsString());
        assertFalse(plastic.has("ingredient"));
        assertEquals("magneticraft:liquid_plastic", plastic.getAsJsonObject("fluid").get("fluid").getAsString());
        assertEquals(250, plastic.getAsJsonObject("fluid").get("amount").getAsInt());
        assertEquals(100, plastic.get("duration").getAsInt());
        assertEquals(423.15D, plastic.get("minimum_temperature").getAsDouble());
        assertEquals(20.0D, plastic.get("heat_per_tick").getAsDouble());

        JsonObject rubber = readObject(recipe("polymerizing/rubber"));
        assertTrue(rubber.has("ingredient"));
        assertEquals("magneticraft:natural_gas", rubber.getAsJsonObject("fluid").get("fluid").getAsString());
        assertEquals(500, rubber.getAsJsonObject("fluid").get("amount").getAsInt());
        assertEquals(200, rubber.get("duration").getAsInt());
        assertEquals(473.15D, rubber.get("minimum_temperature").getAsDouble());
        assertEquals(40.0D, rubber.get("heat_per_tick").getAsDouble());
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
        assertLegacySceneModel("battery_box", "mcx", "battery");
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
        assertLegacySceneModel("heat_sink", "mcx", "heat_sink");
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
        assertEquals("minecraft:item/generated", wrench.get("parent").getAsString());
        assertEquals("magneticraft:item/wrench", wrench.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(english.has("item.magneticraft.wrench"));
        assertTrue(chinese.has("item.magneticraft.wrench"));
        assertFile(recipe("crafting/wrench"));

        JsonObject wrenchTag = readObject(GENERATED.resolve("data/forge/tags/items/tools/wrenches.json"));
        assertTrue(wrenchTag.getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("magneticraft:wrench")));
        for (String key : Set.of(
                "item.magneticraft.tiered_name",
                "item.magneticraft.tiered_rated_name",
                "electrical_rating.magneticraft.standard",
                "electrical_rating.magneticraft.heavy",
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
    void conduitModelsRenderOnlyTheirConnectedNamedParts() throws IOException {
        assertConduitMultipart("electric_cable", "center");
        assertConduitMultipart("heat_pipe", "base");
        assertConduitMultipart("insulated_heat_pipe", "center");
        assertConduitMultipart("iron_fluid_pipe", "base");
        assertConduitMultipart("pneumatic_tube", "center_full");
        assertConduitMultipart("pneumatic_restriction_tube", "center_full");

        assertEquals(
                "magneticraft:models/block/mcx/iron_pipe_dark.mcx",
                readObject(ASSETS.resolve("models/block/heat_pipe.json")).get("model").getAsString()
        );
        for (String pneumatic : Set.of("pneumatic_tube", "pneumatic_restriction_tube")) {
            assertEquals(
                    "magneticraft:models/block/gltf/" + pneumatic + "_inv.gltf",
                    readObject(ASSETS.resolve("models/block/" + pneumatic + ".json")).get("model").getAsString()
            );
        }
    }

    private static void assertConduitMultipart(String id, String centerPart) throws IOException {
        JsonObject blockState = readObject(ASSETS.resolve("blockstates/" + id + ".json"));
        assertFalse(blockState.has("variants"), id + " must not use one always-connected variant");
        JsonArray multipart = blockState.getAsJsonArray("multipart");
        assertNotNull(multipart, id + " must use multipart connection models");

        String centerModel = id + "_" + centerPart;
        JsonObject center = readObject(ASSETS.resolve("models/block/" + centerModel + ".json"));
        assertEquals(List.of(centerPart), strings(center.getAsJsonArray("include_nodes")), centerModel);

        for (String direction : List.of("down", "up", "north", "south", "west", "east")) {
            String modelName = id + "_" + direction;
            boolean conditionalArm = multipart.asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .anyMatch(part -> part.has("when")
                            && part.getAsJsonObject("when").has(direction)
                            && "true".equals(part.getAsJsonObject("when").get(direction).getAsString())
                            && ("magneticraft:block/" + modelName).equals(
                            part.getAsJsonObject("apply").get("model").getAsString()
                    ));
            assertTrue(conditionalArm, modelName + " must require its matching connection state");

            JsonObject arm = readObject(ASSETS.resolve("models/block/" + modelName + ".json"));
            assertEquals(List.of(direction), strings(arm.getAsJsonArray("include_nodes")), modelName);
        }
    }

    private static List<String> strings(JsonArray array) {
        assertNotNull(array);
        return array.asList().stream().map(JsonElement::getAsString).toList();
    }

    @Test
    void singleBlockMachineDataRecipesAndAssetsAreComplete() throws IOException {
        JsonObject english = readObject(ASSETS.resolve("lang/en_us.json"));
        JsonObject chinese = readObject(ASSETS.resolve("lang/zh_cn.json"));
        Set<SingleBlockMachineDefinition> renderedMachines = Set.of(
                SingleBlockMachineDefinition.SLUICE_BOX,
                SingleBlockMachineDefinition.FEEDING_TROUGH,
                SingleBlockMachineDefinition.SMALL_TANK,
                SingleBlockMachineDefinition.COMBUSTION_CHAMBER,
                SingleBlockMachineDefinition.STEAM_BOILER,
                SingleBlockMachineDefinition.GASIFICATION_UNIT,
                SingleBlockMachineDefinition.INSERTER,
                SingleBlockMachineDefinition.ELECTRIC_ENGINE
        );
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            String id = definition.id();
            assertFile(ASSETS.resolve("blockstates/" + id + ".json"));
            if (renderedMachines.contains(definition)) {
                JsonObject worldModel = readObject(ASSETS.resolve("models/block/" + id + "_world.json"));
                assertFalse(worldModel.has("loader"), id + " world geometry belongs to the block-entity renderer");
                assertFalse(worldModel.has("elements"), id + " world model must not duplicate the full legacy scene");
                assertFile(ASSETS.resolve("models/block/" + id + "_inventory.json"));
            } else {
                assertFile(ASSETS.resolve("models/block/" + id + ".json"));
            }
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
            assertTrue(textures.has("north") && textures.has("south") && textures.has("east"),
                    id + " must distinguish front, back and side faces");
            assertFalse(textures.get("north").equals(textures.get("south")), id + " front/back texture");
            assertFalse(textures.get("north").equals(textures.get("east")), id + " front/side texture");
        }
        for (String status : Set.of("blocked", "unloaded", "filter_rejected")) {
            String key = "message.magneticraft.pneumatic_endpoint." + status;
            assertTrue(english.has(key), key);
            assertTrue(chinese.has(key), key);
        }
        Map.ofEntries(
                Map.entry("wooden_crate", Map.of("all", "magneticraft:block/machines/box")),
                Map.entry("fabricator", Map.of(
                        "down", "magneticraft:block/machines/fabricator_bottom",
                        "up", "magneticraft:block/machines/fabricator_top",
                        "north", "magneticraft:block/machines/fabricator_side"
                )),
                Map.entry("water_generator", Map.of("all", "magneticraft:block/machines/water_generator")),
                Map.entry("electric_heater", Map.of(
                        "side", "magneticraft:block/electric_machines/heater",
                        "end", "magneticraft:block/electric_machines/heater_off"
                )),
                Map.entry("electric_heater_on", Map.of("end", "magneticraft:block/electric_machines/heater_on")),
                Map.entry("forge_energy_heater", Map.of(
                        "side", "magneticraft:block/electric_machines/rf_heater",
                        "end", "magneticraft:block/electric_machines/rf_heater_off"
                )),
                Map.entry("forge_energy_heater_on", Map.of(
                        "end", "magneticraft:block/electric_machines/rf_heater_on"
                )),
                Map.entry("brick_furnace", Map.of(
                        "side", "magneticraft:block/heat_machines/brick_furnace",
                        "top", "magneticraft:block/heat_machines/brick_furnace_top",
                        "front", "magneticraft:block/heat_machines/brick_furnace_front"
                )),
                Map.entry("brick_furnace_on", Map.of(
                        "front", "magneticraft:block/heat_machines/brick_furnace_front_on"
                )),
                Map.entry("infinite_energy_source", Map.of(
                        "side", "magneticraft:block/electric_machines/infinite_energy",
                        "end", "magneticraft:block/electric_machines/infinite_energy_top"
                )),
                Map.entry("airlock", Map.of("all", "magneticraft:block/machines/airlock")),
                Map.entry("thermopile", Map.of(
                        "side", "magneticraft:block/electric_machines/thermopile",
                        "end", "magneticraft:block/electric_machines/thermopile_top"
                )),
                Map.entry("forge_energy_transformer", Map.of(
                        "side", "magneticraft:block/electric_machines/rf_transformer",
                        "end", "magneticraft:block/electric_machines/rf_transformer_top"
                ))
        ).forEach((model, textures) -> assertModelTexturesUnchecked(model, textures));
        for (String id : Set.of("tube_light", "inserter_speed_upgrade", "inserter_stack_upgrade")) {
            assertFile(ASSETS.resolve("models/item/" + id + ".json"));
            assertTrue(english.has((id.equals("tube_light") ? "block" : "item") + ".magneticraft." + id), id);
        }
        assertFile(ASSETS.resolve("blockstates/air_bubble.json"));
        assertFalse(Files.exists(ASSETS.resolve("models/item/air_bubble.json")));
        assertFalse(Files.exists(DATA.resolve("loot_tables/blocks/air_bubble.json")));

        assertRecipeDirectory("sluice_box", 17, "magneticraft:sluice_box");
        assertRecipeDirectory("gasification_unit", 28, "magneticraft:gasification_unit");
        assertRecipeDirectory("thermopile", FluidDefinition.values().length + 17L, "magneticraft:thermopile");
        assertRecipeDirectory("fluid_fuel", 10, "magneticraft:industrial_combustion_chamber");
        JsonObject sand = readObject(recipe("sluice_box/sand"));
        assertEquals(10, sand.getAsJsonArray("results").size());
        assertChanceResult(
                readObject(recipe("sluice_box/galena_rocky_chunk")),
                "magneticraft:silver_dust",
                0.25F
        );
        assertChanceResult(
                readObject(recipe("advanced_processing/sieve_galena_rocky_chunk")),
                "magneticraft:silver_dust",
                0.25F
        );
        assertChanceResult(
                readObject(recipe("sluice_box/iron_rocky_chunk")),
                "magneticraft:nickel_dust",
                0.025F
        );
        JsonObject nickelSluice = readObject(recipe("sluice_box/nickel_rocky_chunk"));
        assertFalse(hasResult(nickelSluice, "magneticraft:tin_dust"));
        assertFalse(hasResult(nickelSluice, "magneticraft:osmium_dust"));
        assertChanceResult(
                readObject(recipe("advanced_processing/sieve_nickel_rocky_chunk")),
                "magneticraft:osmium_dust",
                0.25F
        );
        JsonObject cobaltSieve = readObject(recipe("advanced_processing/sieve_cobalt_rocky_chunk"));
        assertChanceResult(cobaltSieve, "magneticraft:mithril_dust", 0.01F);
        assertFalse(hasResult(cobaltSieve, "magneticraft:osmium_dust"));
        JsonObject log = readObject(recipe("gasification_unit/00_logs"));
        assertEquals("minecraft:charcoal", log.getAsJsonObject("item_result").get("item").getAsString());
        assertEquals(150, log.getAsJsonObject("fluid_result").get("amount").getAsInt());
        JsonObject snow = readObject(recipe("thermopile/snow_layer_8"));
        assertEquals("8", snow.getAsJsonObject("state").get("layers").getAsString());
        JsonObject diesel = readObject(recipe("fluid_fuel/diesel"));
        assertEquals(10_000, diesel.get("duration").getAsInt());
        assertEquals(80.0D, diesel.get("power").getAsDouble());
    }

    private static void assertChanceResult(JsonObject recipe, String item, float chance) {
        JsonObject result = recipe.getAsJsonArray("results").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(candidate -> resultItem(candidate).equals(item))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing result " + item));
        assertEquals(chance, result.get("chance").getAsFloat(), 0.0001F, item);
    }

    private static boolean hasResult(JsonObject recipe, String item) {
        return recipe.getAsJsonArray("results").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .anyMatch(candidate -> resultItem(candidate).equals(item));
    }

    private static String resultItem(JsonObject result) {
        JsonObject stack = result.has("stack") ? result.getAsJsonObject("stack") : result;
        return stack.get("item").getAsString();
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
            JsonObject controllerModel = readObject(ASSETS.resolve("models/block/" + id + ".json"));
            assertEquals(
                    switch (definition) {
                        case POLYMERIZER -> "minecraft:block/orientable";
                        case STIRLING_GENERATOR -> "minecraft:block/cube_column";
                        default -> "minecraft:block/cube_all";
                    },
                    controllerModel.get("parent").getAsString(),
                    id
            );
            if (definition == MultiblockDefinition.POLYMERIZER) {
                JsonObject textures = controllerModel.getAsJsonObject("textures");
                assertEquals("magneticraft:block/polymerizer_front", textures.get("front").getAsString());
                assertEquals("magneticraft:block/polymerizer_side", textures.get("side").getAsString());
                assertEquals("magneticraft:block/polymerizer_side", textures.get("top").getAsString());
            }
            assertFalse(controllerModel.has("loader"), id + " world model must not obscure the hologram");
            assertFile(ASSETS.resolve("models/block/" + id + "_formed.json"));
            assertFile(ASSETS.resolve("models/block/" + id + "_item.json"));
            assertFile(ASSETS.resolve("models/item/" + id + ".json"));
            assertFile(DATA.resolve("loot_tables/blocks/" + id + ".json"));
            assertFile(recipe("crafting/" + id));
            assertFile(ASSETS.resolve("guide/multiblocks/" + id + ".json"));
            assertTrue(english.has("block.magneticraft." + id), id);
            assertTrue(chinese.has("block.magneticraft." + id), id);
        }
        for (String runtimeBlock : List.of("multiblock_gap", "pumpjack_drill")) {
            String translationKey = "block.magneticraft." + runtimeBlock;
            assertTrue(english.has(translationKey), translationKey);
            assertTrue(chinese.has(translationKey), translationKey);
            assertFalse(english.get(translationKey).getAsString().isBlank(), translationKey);
            assertFalse(chinese.get(translationKey).getAsString().isBlank(), translationKey);
        }
        Map<String, String> advancedMcxModels = Map.of(
                "shipping_container", "container",
                "oil_heater", "oil_heater",
                "pumpjack", "pumpjack",
                "refinery", "refinery",
                "shelving_unit", "shelving_unit",
                "solar_mirror", "solar_mirror",
                "solar_panel", "solar_panel",
                "solar_tower", "solar_tower"
        );
        Map<String, String> advancedGltfModels = Map.of(
                "industrial_combustion_chamber", "big_combustion_chamber",
                "industrial_electric_furnace", "big_electric_furnace",
                "industrial_steam_boiler", "big_steam_boiler",
                "grinder", "grinder",
                "hydraulic_press", "hydraulic_press",
                "sieve", "sieve",
                "steam_engine", "steam_engine",
                "steam_turbine", "steam_turbine"
        );
        advancedMcxModels.forEach((generated, source) ->
                assertLegacySceneModelUnchecked(generated + "_item", "mcx", source));
        advancedGltfModels.forEach((generated, source) ->
                assertLegacySceneModelUnchecked(generated + "_item", "gltf", source));
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            JsonObject formed = readObject(ASSETS.resolve(
                    "models/block/" + definition.id() + "_formed.json"
            ));
            assertFalse(formed.has("loader"), definition.id() + " formed controller must be invisible");
            assertFalse(formed.has("elements"), definition.id() + " formed controller must be invisible");
        }

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
        assertLegacySceneModel("computer", "mcx", "computer");
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
            assertEquals("magneticraft:" + entry.getValue(), processing.get("type").getAsString());
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

    private static void assertLegacySceneModel(
            String generatedName,
            String format,
            String sourceName
    ) throws IOException {
        JsonObject model = readObject(ASSETS.resolve("models/block/" + generatedName + ".json"));
        assertEquals("magneticraft:legacy_scene", model.get("loader").getAsString());
        assertEquals(
                "magneticraft:models/block/" + format + "/" + sourceName + "." + format,
                model.get("model").getAsString()
        );
        assertFile(SOURCE_MODELS.resolve("block/" + format + "/" + sourceName + "." + format));
    }

    private static void assertLegacySceneModelUnchecked(String generatedName, String format, String sourceName) {
        try {
            assertLegacySceneModel(generatedName, format, sourceName);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static void assertModelTexturesUnchecked(String generatedName, Map<String, String> expectedTextures) {
        try {
            JsonObject textures = readObject(ASSETS.resolve("models/block/" + generatedName + ".json"))
                    .getAsJsonObject("textures");
            expectedTextures.forEach((key, value) -> assertEquals(value, textures.get(key).getAsString(), generatedName));
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
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
