package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.model.ModelSceneSelection;
import committee.nova.mods.magneticraft.client.model.ModelTransform;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.PoleSegment;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlock;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerPart;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlock;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Set;

/**
 * Generates solid-block models plus invisible fluid-block state models.
 */
final class ModBlockStateProvider extends BlockStateProvider {
    private final ExistingFileHelper existingFileHelper;

    ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Magneticraft.MOD_ID, existingFileHelper);
        this.existingFileHelper = existingFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            Block block = ModBlocks.get(definition).get();
            simpleBlockWithItem(block, cubeAll(block));
        }

        Block crushingTable = ModMachineBlocks.CRUSHING_TABLE.get();
        ModelFile crushingModel = mcxModel(
                "crushing_table",
                "crushing_table",
                modLoc("blocks/machines/crushing_table_side"),
                ModelSceneSelection.ALL
        );
        simpleBlockWithItem(crushingTable, crushingModel);

        Block battery = ModMachineBlocks.BATTERY.get();
        ModelFile batteryModel = mcxModel(
                "battery_box",
                "battery",
                modLoc("blocks/electric_machines/battery"),
                ModelSceneSelection.ALL
        );
        legacyHorizontalBlock(battery, batteryModel, 180);
        simpleBlockItem(battery, batteryModel);

        Block grate = ModMachineBlocks.GRATE.get();
        simpleBlockWithItem(
                grate,
                models().cubeAll("iron_grate", modLoc("block/iron_grate"))
                        .renderType(ResourceLocation.fromNamespaceAndPath("minecraft", "cutout"))
        );

        Block electricFurnace = ModMachineBlocks.ELECTRIC_FURNACE.get();
        ResourceLocation electricFurnaceTexture = modLoc("blocks/electric_machines/electric_furnace");
        ModelFile furnaceOff = models().orientable(
                "electric_furnace",
                electricFurnaceTexture,
                modLoc("blocks/electric_machines/electric_furnace_front"),
                electricFurnaceTexture
        );
        ModelFile furnaceOn = models().orientable(
                "electric_furnace_on",
                electricFurnaceTexture,
                modLoc("blocks/electric_machines/electric_furnace_front_on"),
                electricFurnaceTexture
        );
        horizontalBlock(
                electricFurnace,
                state -> state.getValue(ElectricFurnaceBlock.LIT) ? furnaceOn : furnaceOff
        );
        simpleBlockItem(electricFurnace, furnaceOff);

        ModMachineBlocks.machines().forEach((definition, holder) ->
                registerSingleBlockMachineModel(holder.get(), definition));
        simpleBlock(
                ModMachineBlocks.AIR_BUBBLE.get(),
                models().cubeAll("air_bubble", mcLoc("block/white_stained_glass"))
                        .renderType(ResourceLocation.fromNamespaceAndPath("minecraft", "translucent"))
        );
        simpleBlockWithItem(
                ModMachineBlocks.TUBE_LIGHT.get(),
                mcxModel(
                        "tube_light",
                        "tube_light",
                        modLoc("blocks/decoration/tube_light"),
                        ModelSceneSelection.ALL
                )
        );

        legacyConduitBlock(ModNetworkBlocks.ELECTRIC_CABLE.get(), "electric_cable");
        registerLongDistanceElectricModels();
        legacyConduitBlock(ModNetworkBlocks.HEAT_PIPE.get(), "heat_pipe");
        legacyConduitBlock(ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), "insulated_heat_pipe");
        legacyConduitBlock(ModNetworkBlocks.IRON_PIPE.get(), "iron_fluid_pipe");
        legacyConduitBlock(ModNetworkBlocks.PNEUMATIC_TUBE.get(), "pneumatic_tube");
        legacyConduitBlock(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(), "pneumatic_restriction_tube");

        Block heatSink = ModNetworkBlocks.HEAT_SINK.get();
        ModelFile heatSinkModel = mcxModel(
                "heat_sink",
                "heat_sink",
                modLoc("blocks/machines/heat_sink"),
                ModelSceneSelection.ALL
        );
        downFacingBlock(heatSink, heatSinkModel);
        simpleBlockItem(heatSink, heatSinkModel);

        Block conveyor = ModNetworkBlocks.CONVEYOR_BELT.get();
        ModelFile conveyorModel = mcxModel(
                "conveyor_belt",
                "conveyor_belt",
                modLoc("blocks/machines/conveyor_belt"),
                ModelSceneSelection.ALL
        );
        horizontalBlock(conveyor, conveyorModel);
        simpleBlockItem(conveyor, conveyorModel);

        simpleBlockWithItem(
                ModAdvancedBlocks.MULTIBLOCK_BASE.get(),
                models().cubeAll("machine_casing", mcLoc("block/iron_block"))
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.CORRUGATED_IRON.get(),
                models().cubeAll("corrugated_iron", mcLoc("block/polished_andesite"))
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.COPPER_COIL.get(),
                models().cubeAll("copper_coil", mcLoc("block/copper_block"))
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.MULTIBLOCK_COLUMN.get(),
                models().cubeAll("machine_support_column", mcLoc("block/iron_block"))
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get(),
                models().cubeAll("striped_machine_casing", mcLoc("block/yellow_concrete"))
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get(),
                models().cubeAll("electrical_machine_casing", mcLoc("block/redstone_block"))
        );
        simpleBlock(
                ModAdvancedBlocks.PUMPJACK_DRILL.get(),
                models().cubeAll("pumpjack_drill", mcLoc("block/copper_block"))
        );
        simpleBlock(
                ModAdvancedBlocks.MULTIBLOCK_GAP.get(),
                emptyModel("multiblock_gap", mcLoc("block/iron_block"))
        );

        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            Block controller = ModAdvancedBlocks.controller(definition).get();
            ModelFile idle = models().cubeAll(definition.id(), mcLoc("block/iron_block"));
            ModelFile formed = emptyModel(definition.id() + "_formed", mcLoc("block/iron_block"));
            ModelFile item = advancedControllerItemModel(definition);
            horizontalBlock(
                    controller,
                    state -> state.getValue(AdvancedMultiblockBlock.FORMED) ? formed : idle
            );
            simpleBlockItem(controller, item);
        }

        simpleBlockWithItem(
                ModAdvancedBlocks.OIL_DEPOSIT.get(),
                models().cubeAll("oil_deposit", mcLoc("block/deepslate"))
        );

        Block computer = ModComputerContent.COMPUTER.get();
        ModelFile computerModel = mcxModel(
                "computer",
                "computer",
                modLoc("blocks/computers/computer1"),
                new ModelSceneSelection(Set.of(), Set.of(), Set.of("screen"), Set.of())
        );
        horizontalBlock(computer, computerModel);
        simpleBlockItem(computer, computerModel);

        Block miningRobot = ModComputerContent.MINING_ROBOT.get();
        ModelFile miningRobotModel = mcxModel(
                "mining_robot",
                "mining_robot",
                modLoc("blocks/computers/mining_robot"),
                new ModelSceneSelection(
                        Set.of(),
                        Set.of(),
                        Set.of("prop1", "prop2", "drill1", "drill2", "drill3", "drill4", "drill5"),
                        Set.of()
                )
        );
        horizontalBlock(miningRobot, miningRobotModel);
        simpleBlockItem(miningRobot, miningRobotModel);

        for (FluidDefinition definition : FluidDefinition.values()) {
            ModelFile model = models().getBuilder(definition.id())
                    .texture("particle", Magneticraft.id("fluid/" + definition.id() + "_still"));
            simpleBlock(ModFluids.get(definition).block().get(), model);
        }
    }

    private void registerSingleBlockMachineModel(Block block, SingleBlockMachineDefinition definition) {
        switch (definition) {
            case BOX -> simpleBlockWithItem(
                    block,
                    models().cubeAll(definition.id(), modLoc("blocks/machines/box"))
            );
            case FABRICATOR -> simpleBlockWithItem(block, models().cube(
                    definition.id(),
                    modLoc("blocks/machines/fabricator_bottom"),
                    modLoc("blocks/machines/fabricator_top"),
                    modLoc("blocks/machines/fabricator_side"),
                    modLoc("blocks/machines/fabricator_side"),
                    modLoc("blocks/machines/fabricator_side"),
                    modLoc("blocks/machines/fabricator_side")
            ));
            case WATER_GENERATOR -> simpleBlockWithItem(
                    block,
                    models().cubeAll(definition.id(), modLoc("blocks/machines/water_generator"))
            );
            case ELECTRIC_HEATER -> registerLitColumn(
                    block,
                    definition.id(),
                    modLoc("blocks/electric_machines/heater"),
                    modLoc("blocks/electric_machines/heater_off"),
                    modLoc("blocks/electric_machines/heater_on")
            );
            case RF_HEATER -> registerLitColumn(
                    block,
                    definition.id(),
                    modLoc("blocks/electric_machines/rf_heater"),
                    modLoc("blocks/electric_machines/rf_heater_off"),
                    modLoc("blocks/electric_machines/rf_heater_on")
            );
            case BRICK_FURNACE -> registerLitHorizontal(
                    block,
                    definition.id(),
                    modLoc("blocks/heat_machines/brick_furnace"),
                    modLoc("blocks/heat_machines/brick_furnace_top"),
                    modLoc("blocks/heat_machines/brick_furnace_front"),
                    modLoc("blocks/heat_machines/brick_furnace_front_on")
            );
            case INFINITE_ENERGY -> simpleBlockWithItem(
                    block,
                    models().cubeColumn(
                            definition.id(),
                            modLoc("blocks/electric_machines/infinite_energy"),
                            modLoc("blocks/electric_machines/infinite_energy_top")
                    )
            );
            case AIRLOCK -> simpleBlockWithItem(
                    block,
                    models().cubeAll(definition.id(), modLoc("blocks/machines/airlock"))
            );
            case THERMOPILE -> simpleBlockWithItem(
                    block,
                    models().cubeColumn(
                            definition.id(),
                            modLoc("blocks/electric_machines/thermopile"),
                            modLoc("blocks/electric_machines/thermopile_top")
                    )
            );
            case RF_TRANSFORMER -> simpleBlockWithItem(
                    block,
                    models().cubeColumn(
                            definition.id(),
                            modLoc("blocks/electric_machines/rf_transformer"),
                            modLoc("blocks/electric_machines/rf_transformer_top")
                    )
            );
            case RELAY, FILTER, TRANSPOSER -> registerPneumaticEndpoint(block, definition);
            case SLUICE_BOX -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/table_sieve_bottom"),
                    mcxModel(definition.id() + "_inventory", "sluice_box_inv",
                            modLoc("blocks/machines/table_sieve_bottom"), ModelSceneSelection.ALL)
            );
            case FEEDING_TROUGH -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/feeding_trough"),
                    mcxModel(definition.id() + "_inventory", "feeding_trough_inv",
                            modLoc("blocks/machines/feeding_trough"), ModelSceneSelection.ALL)
            );
            case SMALL_TANK -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/fluid_machines/small_tank_in"),
                    mcxModel(definition.id() + "_inventory", "small_tank",
                            modLoc("blocks/fluid_machines/small_tank_in"), ModelSceneSelection.ALL)
            );
            case COMBUSTION_CHAMBER -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/combustion_gen_side1"),
                    mcxModel(definition.id() + "_inventory", "combustion_chamber",
                            modLoc("blocks/machines/combustion_gen_side1"), ModelSceneSelection.ALL)
            );
            case STEAM_BOILER -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/boiler"),
                    mcxModel(definition.id() + "_inventory", "steam_boiler",
                            modLoc("blocks/machines/boiler"), ModelSceneSelection.ALL)
            );
            case GASIFICATION_UNIT -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/gasification_unit"),
                    mcxModel(definition.id() + "_inventory", "gasification_unit",
                            modLoc("blocks/machines/gasification_unit"), ModelSceneSelection.ALL)
            );
            case INSERTER -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/machines/inserter"),
                    gltfModel(definition.id() + "_inventory", "inserter",
                            modLoc("blocks/machines/inserter"), ModelSceneSelection.ALL)
            );
            case ELECTRIC_ENGINE -> registerRenderedMachine(
                    block,
                    definition,
                    modLoc("blocks/electric_machines/electric_engine"),
                    gltfModel(definition.id() + "_inventory", "electric_engine",
                            modLoc("blocks/electric_machines/electric_engine"), ModelSceneSelection.ALL)
            );
        }
    }

    private void registerRenderedMachine(
            Block block,
            SingleBlockMachineDefinition definition,
            ResourceLocation particle,
            ModelFile inventoryModel
    ) {
        simpleBlock(block, emptyModel(definition.id() + "_world", particle));
        simpleBlockItem(block, inventoryModel);
    }

    private void registerLitColumn(
            Block block,
            String name,
            ResourceLocation side,
            ResourceLocation endOff,
            ResourceLocation endOn
    ) {
        ModelFile off = models().cubeColumn(name, side, endOff);
        ModelFile on = models().cubeColumn(name + "_on", side, endOn);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(SingleBlockMachineBlock.LIT) ? on : off)
                .build());
        simpleBlockItem(block, off);
    }

    private void registerLitHorizontal(
            Block block,
            String name,
            ResourceLocation side,
            ResourceLocation top,
            ResourceLocation frontOff,
            ResourceLocation frontOn
    ) {
        ModelFile off = models().orientable(name, side, frontOff, top);
        ModelFile on = models().orientable(name + "_on", side, frontOn, top);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(SingleBlockMachineBlock.LIT) ? on : off)
                .rotationY(vanillaHorizontalRotation(state.getValue(SingleBlockMachineBlock.FACING)))
                .build());
        simpleBlockItem(block, off);
    }

    private static int vanillaHorizontalRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private void legacyConduitBlock(Block block, String artifactName) {
        ResourceLocation particle = switch (artifactName) {
                    case "electric_cable" -> modLoc("blocks/electric_connectors/electric_cable");
                    case "heat_pipe" -> modLoc("blocks/fluid_machines/iron_pipe");
                    case "insulated_heat_pipe" -> modLoc("blocks/fluid_machines/insulated_heat_pipe");
                    case "iron_fluid_pipe" -> modLoc("blocks/fluid_machines/iron_pipe");
                    case "pneumatic_tube", "pneumatic_restriction_tube" ->
                            modLoc("blocks/machines/pneumatic_tube");
                    default -> throw new IllegalArgumentException("Unknown historical conduit: " + artifactName);
                };
        String sourceName = switch (artifactName) {
            case "heat_pipe", "iron_fluid_pipe" -> "iron_pipe";
            default -> artifactName;
        };
        String centerPart = switch (artifactName) {
            case "heat_pipe", "iron_fluid_pipe" -> "base";
            case "pneumatic_tube", "pneumatic_restriction_tube" -> "center_full";
            default -> "center";
        };

        ModelFile inventory = switch (artifactName) {
            case "pneumatic_tube", "pneumatic_restriction_tube" ->
                    gltfModel(artifactName, artifactName + "_inv", particle, ModelSceneSelection.ALL);
            case "heat_pipe" -> mcxModel(artifactName, "iron_pipe_dark", particle, ModelSceneSelection.ALL);
            default -> mcxModel(artifactName, sourceName, particle, ModelSceneSelection.ALL);
        };
        simpleBlockItem(block, inventory);

        if (artifactName.startsWith("pneumatic_")) {
            pneumaticConduitBlock(block, artifactName, sourceName, particle);
            return;
        }

        MultiPartBlockStateBuilder multipart = getMultipartBuilder(block);
        multipart.part()
                .modelFile(mcxPart(artifactName + "_" + centerPart, sourceName, particle, centerPart))
                .addModel()
                .end();
        addConduitArms(multipart, artifactName, sourceName, particle, false);
    }

    private void pneumaticConduitBlock(
            Block block,
            String artifactName,
            String sourceName,
            ResourceLocation particle
    ) {
        MultiPartBlockStateBuilder multipart = getMultipartBuilder(block);
        addPneumaticFullCenter(multipart, gltfPart(
                artifactName + "_center_full", sourceName, particle, "center_full"
        ));

        addPneumaticStraightCenter(
                multipart,
                artifactName,
                sourceName,
                particle,
                new boolean[]{false, false, true, true, false, false},
                "center_east_h", "center_west_h", "center_up_h", "center_down_h"
        );
        addPneumaticStraightCenter(
                multipart,
                artifactName,
                sourceName,
                particle,
                new boolean[]{false, false, false, false, true, true},
                "center_north_h", "center_south_h", "center_up_v", "center_down_v"
        );
        addPneumaticStraightCenter(
                multipart,
                artifactName,
                sourceName,
                particle,
                new boolean[]{true, true, false, false, false, false},
                "center_north_v", "center_south_v", "center_east_v", "center_west_v"
        );
        addConduitArms(multipart, artifactName, sourceName, particle, true);
    }

    private void addConduitArms(
            MultiPartBlockStateBuilder multipart,
            String artifactName,
            String sourceName,
            ResourceLocation particle,
            boolean gltf
    ) {
        for (Direction direction : Direction.values()) {
            String partName = direction.getName();
            ModelFile part = gltf
                    ? gltfPart(artifactName + "_" + partName, sourceName, particle, partName)
                    : mcxPart(artifactName + "_" + partName, sourceName, particle, partName);
            multipart.part()
                    .modelFile(part)
                    .addModel()
                    .condition(ConduitBlock.property(direction), true)
                    .end();
        }
    }

    private void addPneumaticStraightCenter(
            MultiPartBlockStateBuilder multipart,
            String artifactName,
            String sourceName,
            ResourceLocation particle,
            boolean[] connections,
            String... partNames
    ) {
        for (String partName : partNames) {
            var part = multipart.part()
                    .modelFile(gltfPart(artifactName + "_" + partName, sourceName, particle, partName))
                    .addModel();
            addExactConnections(part, connections);
            part.end();
        }
    }

    private void addPneumaticFullCenter(MultiPartBlockStateBuilder multipart, ModelFile model) {
        var part = multipart.part().modelFile(model).addModel();
        var allNonStraight = part.nestedGroup();
        addNotExactConnections(
                allNonStraight,
                new boolean[]{false, false, true, true, false, false}
        );
        addNotExactConnections(
                allNonStraight,
                new boolean[]{false, false, false, false, true, true}
        );
        addNotExactConnections(
                allNonStraight,
                new boolean[]{true, true, false, false, false, false}
        );
        allNonStraight.end();
        part.end();
    }

    private void addExactConnections(MultiPartBlockStateBuilder.PartBuilder part, boolean[] connections) {
        for (int index = 0; index < Direction.values().length; index++) {
            part.condition(ConduitBlock.property(Direction.values()[index]), connections[index]);
        }
    }

    private void addNotExactConnections(
            MultiPartBlockStateBuilder.PartBuilder.ConditionGroup parent,
            boolean[] connections
    ) {
        var anyDifference = parent.nestedGroup().useOr();
        for (int index = 0; index < Direction.values().length; index++) {
            anyDifference.condition(ConduitBlock.property(Direction.values()[index]), !connections[index]);
        }
        anyDifference.endNestedGroup();
    }

    private ModelFile mcxPart(
            String generatedName,
            String sourceName,
            ResourceLocation particle,
            String partName
    ) {
        return mcxModel(generatedName, sourceName, particle, includeNodes(partName));
    }

    private ModelFile gltfPart(
            String generatedName,
            String sourceName,
            ResourceLocation particle,
            String partName
    ) {
        return gltfModel(generatedName, sourceName, particle, includeNodes(partName));
    }

    private static ModelSceneSelection includeNodes(String... names) {
        return new ModelSceneSelection(Set.of(names), Set.of(), Set.of(), Set.of());
    }

    private void registerPneumaticEndpoint(Block block, SingleBlockMachineDefinition definition) {
        String texture = switch (definition) {
            case RELAY -> "relay";
            case FILTER -> "filter";
            case TRANSPOSER -> "transposer";
            default -> throw new IllegalArgumentException("Not a pneumatic endpoint: " + definition);
        };
        ResourceLocation side = modLoc("blocks/machines/" + texture + "_side");
        ModelFile model = models().withExistingParent(definition.id(), modLoc("block/pneumatic_endpoint"))
                .texture("particle", modLoc("blocks/machines/" + texture + "_front"))
                .texture("down", side)
                .texture("up", side)
                .texture("north", modLoc("blocks/machines/" + texture + "_front"))
                .texture("east", side)
                .texture("south", modLoc("blocks/machines/" + texture + "_back"))
                .texture("west", side);
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(SingleBlockMachineBlock.FACING);
            int rotationX = switch (facing) {
                case DOWN -> 90;
                case UP -> 270;
                default -> 0;
            };
            int rotationY = switch (facing) {
                case DOWN, UP, EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
        simpleBlockItem(block, model);
    }

    private void legacyHorizontalBlock(Block block, ModelFile model, int offset) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            int rotation = Math.floorMod(switch (facing) {
                case SOUTH -> 180;
                case WEST -> 90;
                case EAST -> 270;
                default -> 0;
            } + offset, 360);
            return ConfiguredModel.builder().modelFile(model).rotationY(rotation).build();
        });
    }

    private ModelFile advancedControllerItemModel(MultiblockDefinition definition) {
        String generatedName = definition.id() + "_item";
        return switch (definition) {
            case BIG_COMBUSTION_CHAMBER -> advancedGltfModel(generatedName, "big_combustion_chamber");
            case BIG_ELECTRIC_FURNACE -> advancedGltfModel(generatedName, "big_electric_furnace");
            case BIG_STEAM_BOILER -> advancedGltfModel(generatedName, "big_steam_boiler");
            case CONTAINER -> advancedMcxModel(generatedName, "container");
            case GRINDER -> advancedGltfModel(generatedName, "grinder");
            case HYDRAULIC_PRESS -> advancedGltfModel(generatedName, "hydraulic_press");
            case OIL_HEATER -> advancedMcxModel(generatedName, "oil_heater");
            case PUMPJACK -> advancedMcxModel(generatedName, "pumpjack");
            case REFINERY -> advancedMcxModel(generatedName, "refinery");
            case SHELVING_UNIT -> advancedMcxModel(generatedName, "shelving_unit");
            case SIEVE -> advancedGltfModel(generatedName, "sieve");
            case SOLAR_MIRROR -> advancedMcxModel(generatedName, "solar_mirror");
            case SOLAR_PANEL -> advancedMcxModel(generatedName, "solar_panel");
            case SOLAR_TOWER -> advancedMcxModel(generatedName, "solar_tower");
            case STEAM_ENGINE -> advancedGltfModel(generatedName, "steam_engine");
            case STEAM_TURBINE -> advancedGltfModel(generatedName, "steam_turbine");
        };
    }

    private ModelFile advancedMcxModel(String generatedName, String sourceName) {
        return mcxModel(generatedName, sourceName, mcLoc("block/iron_block"), ModelSceneSelection.ALL);
    }

    private ModelFile advancedGltfModel(String generatedName, String sourceName) {
        return gltfModel(generatedName, sourceName, mcLoc("block/iron_block"), ModelSceneSelection.ALL);
    }

    private void registerLongDistanceElectricModels() {
        historicalDirectionalBlock(
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                mcxModel(
                        "electric_connector",
                        "connector",
                        modLoc("blocks/electric_connectors/connector"),
                        ModelSceneSelection.ALL
                )
        );
        historicalDirectionalBlock(
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(),
                gltfModel(
                        "wireless_energy_receiver",
                        "energy_receiver",
                        modLoc("blocks/electric_connectors/energy_receiver"),
                        ModelSceneSelection.ALL
                )
        );

        poleModels(ModNetworkBlocks.ELECTRIC_POLE.get(), "electric_pole");
        poleModels(ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(), "electric_pole_transformer");

        Block teslaTower = ModNetworkBlocks.TESLA_TOWER.get();
        ModelFile teslaBottom = gltfModel(
                "tesla_tower_bottom",
                "tesla_tower",
                modLoc("blocks/electric_connectors/tesla_tower"),
                ModelSceneSelection.ALL
        );
        ModelFile teslaEmpty = emptyModel("tesla_tower_member", modLoc("blocks/electric_connectors/tesla_tower"));
        getVariantBuilder(teslaTower).forAllStates(state -> {
            TeslaTowerPart part = state.getValue(TeslaTowerBlock.PART);
            return ConfiguredModel.builder().modelFile(switch (part) {
                case BOTTOM -> teslaBottom;
                case MIDDLE, TOP -> teslaEmpty;
            }).build();
        });
        simpleBlockItem(teslaTower, teslaBottom);

        Block windTurbine = ModNetworkBlocks.WIND_TURBINE.get();
        ModelFile turbineModel = legacySceneModel(
                "wind_turbine",
                "mcx",
                "wind_turbine",
                modLoc("blocks/electric_machines/wind_turbine"),
                new ModelSceneSelection(Set.of("Shape2"), Set.of(), Set.of(), Set.of()),
                new ModelTransform(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F)
        );
        horizontalBlock(windTurbine, state -> turbineModel);
        simpleBlockItem(
                windTurbine,
                legacySceneModel(
                        "wind_turbine_inventory",
                        "mcx",
                        "wind_turbine",
                        modLoc("blocks/electric_machines/wind_turbine"),
                        ModelSceneSelection.ALL,
                        new ModelTransform(0.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F)
                )
        );
    }

    private void historicalDirectionalBlock(
            Block block,
            ModelFile model
    ) {
        directionalBlock(block, model);
        simpleBlockItem(block, model);
    }

    /** Maps a historical model authored against the lower face to the six-way facing property. */
    private void downFacingBlock(Block block, ModelFile model) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(HeatSinkBlock.FACING);
            int rotationX = switch (facing) {
                case DOWN -> 0;
                case UP -> 180;
                case NORTH, SOUTH, WEST, EAST -> 270;
            };
            int rotationY = switch (facing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
    }

    private void poleModels(Block block, String name) {
        ResourceLocation particle = modLoc("blocks/electric_connectors/" + name);
        ModelFile empty = emptyModel(name + "_world", particle);
        ModelFile inventory = mcxModel(name + "_inventory", name, particle, ModelSceneSelection.ALL);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(empty)
                .build());
        simpleBlockItem(block, inventory);
    }

    private ModelFile emptyModel(String name, ResourceLocation particleTexture) {
        return models().getBuilder(name).texture("particle", particleTexture);
    }

    private ModelFile mcxModel(
            String generatedName,
            String sourceName,
            ResourceLocation particleTexture,
            ModelSceneSelection selection
    ) {
        return legacySceneModel(
                generatedName,
                "mcx",
                sourceName,
                particleTexture,
                selection,
                ModelTransform.IDENTITY
        );
    }

    private ModelFile gltfModel(
            String generatedName,
            String sourceName,
            ResourceLocation particleTexture,
            ModelSceneSelection selection
    ) {
        return legacySceneModel(
                generatedName,
                "gltf",
                sourceName,
                particleTexture,
                selection,
                ModelTransform.IDENTITY
        );
    }

    private ModelFile legacySceneModel(
            String generatedName,
            String format,
            String sourceName,
            ResourceLocation particleTexture,
            ModelSceneSelection selection,
            ModelTransform sourceTransform
    ) {
        ResourceLocation source = modLoc("models/block/" + format + "/" + sourceName + "." + format);
        BlockModelBuilder model = models().getBuilder(generatedName)
                .texture("particle", particleTexture)
                .customLoader(LegacySceneModelBuilder::begin)
                .model(source)
                .includeNodes(selection.includeNodes().toArray(String[]::new))
                .includeSubtrees(selection.includeSubtrees().toArray(String[]::new))
                .excludeNodes(selection.excludeNodes().toArray(String[]::new))
                .excludeSubtrees(selection.excludeSubtrees().toArray(String[]::new))
                .translation(
                        sourceTransform.translationX(),
                        sourceTransform.translationY(),
                        sourceTransform.translationZ()
                )
                .end();
        InventoryModelTransform transform = LegacyInventoryTransform.load(
                existingFileHelper,
                source,
                selection,
                sourceTransform
        );
        return withInventoryTransform(model, transform);
    }

    private ModelFile withInventoryTransform(BlockModelBuilder model, InventoryModelTransform transform) {
        var transforms = model.transforms();
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            if (context == ItemDisplayContext.NONE) {
                continue;
            }
            InventoryModelTransform.DisplayTransform display = transform.forContext(context);
            transforms.transform(context)
                    .rotation(display.rotationX(), display.rotationY(), display.rotationZ())
                    .translation(display.translationX(), display.translationY(), display.translationZ())
                    .scale(display.scale())
                    .end();
        }
        transforms.end();
        return model;
    }
}
