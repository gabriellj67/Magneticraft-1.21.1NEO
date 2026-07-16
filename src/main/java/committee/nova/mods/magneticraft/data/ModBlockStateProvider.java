package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.model.ModelSceneSelection;
import committee.nova.mods.magneticraft.client.model.ModelTransform;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.DecorativeBlockFamily;
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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Set;

/**
 * Generates block models plus invisible fluid-block state models.
 */
final class ModBlockStateProvider extends BlockStateProvider {
    private static final ResourceLocation CUTOUT_RENDER_TYPE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "cutout");
    private static final ResourceLocation UNMOUNTED_MULTIBLOCK_TEXTURE =
            Magneticraft.id("blocks/multiblocks/unmounted_multiblock");
    private static final int ROOF_TILE_VARIANTS = 4;
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
        registerDecorativeFamilies();

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
                        .renderType(CUTOUT_RENDER_TYPE)
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
                models().cubeAll("air_bubble", modLoc("blocks/machines/air_bubble"))
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
        simpleBlock(
                ModMachineBlocks.GEOTHERMAL_DRILL_PIPE.get(),
                models().cubeColumn(
                        "geothermal_drill_pipe",
                        modLoc("blocks/multiblock_parts/pumpjack_drill_side"),
                        modLoc("blocks/multiblock_parts/pumpjack_drill")
                )
        );

        legacyConduitBlock(ModNetworkBlocks.ELECTRIC_CABLE.get(), "electric_cable");
        simpleBlockWithItem(
                ModNetworkBlocks.BURNT_ELECTRIC_CABLE.get(),
                gltfModel(
                        "burnt_electric_cable",
                        "burnt_electric_cable",
                        modLoc("block/burnt_electric_cable"),
                        ModelSceneSelection.ALL
                )
        );
        registerLongDistanceElectricModels();
        legacyConduitBlock(ModNetworkBlocks.HEAT_PIPE.get(), "heat_pipe");
        legacyConduitBlock(ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), "insulated_heat_pipe");
        legacyConduitBlock(ModNetworkBlocks.IRON_PIPE.get(), "iron_fluid_pipe");
        legacyConduitBlock(ModNetworkBlocks.PNEUMATIC_TUBE.get(), "pneumatic_tube");
        legacyConduitBlock(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(), "pneumatic_restriction_tube");
        brassPressurePipeBlock();
        simpleBlockWithItem(
                ModNetworkBlocks.PRESSURE_TANK.get(),
                models().cubeAll("pressure_tank", modLoc("blocks/fluid_machines/pressure_tank"))
                        .renderType(CUTOUT_RENDER_TYPE)
        );

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
                models().cubeBottomTop(
                        "machine_casing",
                        modLoc("blocks/multiblock_parts/base_side"),
                        modLoc("blocks/multiblock_parts/base_bottom"),
                        modLoc("blocks/multiblock_parts/base_top")
                )
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.CORRUGATED_IRON.get(),
                models().cubeColumn(
                        "corrugated_iron",
                        modLoc("blocks/multiblock_parts/corrugated_iron_side"),
                        modLoc("blocks/multiblock_parts/corrugated_iron")
                )
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.COPPER_COIL.get(),
                models().cubeColumn(
                        "copper_coil",
                        modLoc("blocks/multiblock_parts/copper_coil_side"),
                        modLoc("blocks/multiblock_parts/copper_coil")
                )
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.MULTIBLOCK_COLUMN.get(),
                models().cubeColumn(
                        "machine_support_column",
                        modLoc("blocks/multiblock_parts/column_side"),
                        modLoc("blocks/multiblock_parts/column_end")
                )
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get(),
                models().cubeAll(
                        "striped_machine_casing",
                        modLoc("blocks/multiblock_parts/striped")
                )
        );
        simpleBlockWithItem(
                ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get(),
                models().cubeAll(
                        "electrical_machine_casing",
                        modLoc("blocks/multiblock_parts/electric")
                )
        );
        simpleBlock(
                ModAdvancedBlocks.PUMPJACK_DRILL.get(),
                models().cubeColumn(
                        "pumpjack_drill",
                        modLoc("blocks/multiblock_parts/pumpjack_drill_side"),
                        modLoc("blocks/multiblock_parts/pumpjack_drill")
                )
        );
        simpleBlock(
                ModAdvancedBlocks.MULTIBLOCK_GAP.get(),
                emptyModel("multiblock_gap", modLoc("blocks/multiblocks/multiblock_gap"))
        );

        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            Block controller = ModAdvancedBlocks.controller(definition).get();
            ModelFile idle = switch (definition) {
                case POLYMERIZER -> polymerizerControllerModel(definition.id());
                case STIRLING_GENERATOR -> stirlingControllerModel(definition.id());
                default -> models().cubeAll(definition.id(), UNMOUNTED_MULTIBLOCK_TEXTURE);
            };
            ModelFile formed = emptyModel(definition.id() + "_formed", UNMOUNTED_MULTIBLOCK_TEXTURE);
            ModelFile item = advancedControllerItemModel(definition, UNMOUNTED_MULTIBLOCK_TEXTURE);
            horizontalBlock(
                    controller,
                    state -> state.getValue(AdvancedMultiblockBlock.FORMED) ? formed : idle
            );
            simpleBlockItem(controller, item);
        }

        simpleBlockWithItem(
                ModAdvancedBlocks.OIL_DEPOSIT.get(),
                models().cubeAll("oil_deposit", modLoc("blocks/ore_block/oil_source_1"))
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
            case INTERNAL_COMBUSTION_ENGINE -> {
                ModelFile model = models().orientable(
                        definition.id(),
                        modLoc("blocks/electric_machines/combustion_gen_side1"),
                        modLoc("blocks/electric_machines/combustion_gen_back"),
                        modLoc("blocks/electric_machines/combustion_gen_top")
                ).renderType(CUTOUT_RENDER_TYPE);
                directionalSingleBlock(block, model);
                simpleBlockItem(block, model);
            }
            case GEOTHERMAL_PUMP -> {
                ModelFile model = models().orientable(
                        definition.id(),
                        modLoc("block/geothermal_pump"),
                        modLoc("block/geothermal_pump_front_off"),
                        modLoc("block/geothermal_pump_top")
                );
                directionalSingleBlock(block, model);
                simpleBlockItem(block, model);
            }
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

    private void directionalSingleBlock(Block block, ModelFile model) {
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(vanillaHorizontalRotation(state.getValue(SingleBlockMachineBlock.FACING)))
                .build());
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

    private void brassPressurePipeBlock() {
        Block block = ModNetworkBlocks.BRASS_PRESSURE_PIPE.get();
        ResourceLocation texture = modLoc("blocks/fluid_machines/brass_pressure_pipe");
        MultiPartBlockStateBuilder multipart = getMultipartBuilder(block);
        multipart.part()
                .modelFile(cuboidModel("brass_pressure_pipe_center", texture, 4, 4, 4, 12, 12, 12))
                .addModel()
                .end();
        addPressurePipeArm(multipart, Direction.DOWN, texture, 4, 0, 4, 12, 4, 12);
        addPressurePipeArm(multipart, Direction.UP, texture, 4, 12, 4, 12, 16, 12);
        addPressurePipeArm(multipart, Direction.NORTH, texture, 4, 4, 0, 12, 12, 4);
        addPressurePipeArm(multipart, Direction.SOUTH, texture, 4, 4, 12, 12, 12, 16);
        addPressurePipeArm(multipart, Direction.WEST, texture, 0, 4, 4, 4, 12, 12);
        addPressurePipeArm(multipart, Direction.EAST, texture, 12, 4, 4, 16, 12, 12);

        BlockModelBuilder inventory = models().withExistingParent("brass_pressure_pipe", mcLoc("block/block"))
                .texture("particle", texture)
                .texture("all", texture);
        addCuboid(inventory, 4, 4, 4, 12, 12, 12);
        addCuboid(inventory, 4, 0, 4, 12, 4, 12);
        addCuboid(inventory, 4, 12, 4, 12, 16, 12);
        simpleBlockItem(block, inventory);
    }

    private void addPressurePipeArm(
            MultiPartBlockStateBuilder multipart,
            Direction direction,
            ResourceLocation texture,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
        multipart.part()
                .modelFile(cuboidModel(
                        "brass_pressure_pipe_" + direction.getName(),
                        texture,
                        fromX, fromY, fromZ, toX, toY, toZ
                ))
                .addModel()
                .condition(ConduitBlock.property(direction), true)
                .end();
    }

    private BlockModelBuilder cuboidModel(
            String name,
            ResourceLocation texture,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", texture)
                .texture("all", texture);
        addCuboid(model, fromX, fromY, fromZ, toX, toY, toZ);
        return model;
    }

    private static void addCuboid(
            BlockModelBuilder model,
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
        model.element()
                .from(fromX, fromY, fromZ)
                .to(toX, toY, toZ)
                .allFaces((direction, face) -> face.texture("#all"))
                .end();
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

    private static ModelSceneSelection excludeNodes(String... names) {
        return new ModelSceneSelection(Set.of(), Set.of(), Set.of(names), Set.of());
    }

    private static ModelSceneSelection excludeSubtrees(String... names) {
        return new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of(names));
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

    private ModelFile advancedControllerItemModel(
            MultiblockDefinition definition,
            ResourceLocation particle
    ) {
        String generatedName = definition.id() + "_item";
        return switch (definition) {
            case BIG_COMBUSTION_CHAMBER -> advancedGltfModel(generatedName, "big_combustion_chamber", particle);
            case BIG_ELECTRIC_FURNACE -> advancedGltfModel(generatedName, "big_electric_furnace", particle);
            case BIG_STEAM_BOILER -> advancedGltfModel(generatedName, "big_steam_boiler", particle);
            case CONTAINER -> advancedMcxModel(generatedName, "container", particle);
            case GRINDER -> advancedGltfModel(generatedName, "grinder", particle);
            case HYDRAULIC_PRESS -> advancedGltfModel(generatedName, "hydraulic_press", particle);
            case OIL_HEATER -> advancedMcxModel(generatedName, "oil_heater", particle);
            case POLYMERIZER -> polymerizerControllerModel(generatedName);
            case PUMPJACK -> advancedMcxModel(generatedName, "pumpjack", particle);
            case REFINERY -> advancedMcxModel(generatedName, "refinery", particle);
            case SHELVING_UNIT -> advancedMcxModel(generatedName, "shelving_unit", particle);
            case SIEVE -> advancedGltfModel(generatedName, "sieve", particle);
            case SOLAR_MIRROR -> advancedMcxModel(generatedName, "solar_mirror", particle);
            case SOLAR_PANEL -> advancedMcxModel(generatedName, "solar_panel", particle);
            case SOLAR_TOWER -> advancedMcxModel(generatedName, "solar_tower", particle);
            case STIRLING_GENERATOR -> stirlingControllerModel(generatedName);
            case STEAM_ENGINE -> advancedGltfModel(generatedName, "steam_engine", particle);
            case STEAM_TURBINE -> advancedGltfModel(generatedName, "steam_turbine", particle);
        };
    }

    private ModelFile polymerizerControllerModel(String name) {
        ResourceLocation side = modLoc("block/polymerizer_side");
        return models().orientable(name, side, modLoc("block/polymerizer_front"), side);
    }

    private ModelFile stirlingControllerModel(String name) {
        return models().cubeColumn(
                name,
                modLoc("block/stirling_generator"),
                modLoc("block/stirling_generator_head")
        );
    }

    private ModelFile advancedMcxModel(String generatedName, String sourceName, ResourceLocation particle) {
        return mcxModel(generatedName, sourceName, particle, ModelSceneSelection.ALL);
    }

    private ModelFile advancedGltfModel(String generatedName, String sourceName, ResourceLocation particle) {
        return gltfModel(generatedName, sourceName, particle, ModelSceneSelection.ALL);
    }

    private void registerDecorativeFamilies() {
        ModBlocks.decorativeFamilies().forEach((family, blocks) -> {
            if (family.weightedTextures()) {
                registerWeightedRoofTileFamily(family, blocks);
            } else {
                registerStoneDecorationFamily(family, blocks);
            }
        });
    }

    private void registerStoneDecorationFamily(
            DecorativeBlockFamily family,
            ModBlocks.DecorativeFamilyBlocks blocks
    ) {
        ResourceLocation texture = modLoc("block/" + family.baseId());
        String stairsName = family.stairsId();
        ModelFile stairs = models().stairs(stairsName, texture, texture, texture);
        ModelFile stairsInner = models().stairsInner(stairsName + "_inner", texture, texture, texture);
        ModelFile stairsOuter = models().stairsOuter(stairsName + "_outer", texture, texture, texture);
        stairsBlock(blocks.stairs().get(), stairs, stairsInner, stairsOuter);
        simpleBlockItem(blocks.stairs().get(), stairs);

        String slabName = family.slabId();
        ModelFile slab = models().slab(slabName, texture, texture, texture);
        ModelFile slabTop = models().slabTop(slabName + "_top", texture, texture, texture);
        ModelFile doubleSlab = models().getExistingFile(modLoc("block/" + family.baseId()));
        slabBlock(blocks.slab().get(), slab, slabTop, doubleSlab);
        simpleBlockItem(blocks.slab().get(), slab);
    }

    private void registerWeightedRoofTileFamily(
            DecorativeBlockFamily family,
            ModBlocks.DecorativeFamilyBlocks blocks
    ) {
        ModelFile[] cubes = new ModelFile[ROOF_TILE_VARIANTS];
        ModelFile[] stairs = new ModelFile[ROOF_TILE_VARIANTS];
        ModelFile[] stairsInner = new ModelFile[ROOF_TILE_VARIANTS];
        ModelFile[] stairsOuter = new ModelFile[ROOF_TILE_VARIANTS];
        ModelFile[] slabs = new ModelFile[ROOF_TILE_VARIANTS];
        ModelFile[] slabTops = new ModelFile[ROOF_TILE_VARIANTS];
        for (int variant = 0; variant < ROOF_TILE_VARIANTS; variant++) {
            ResourceLocation texture = modLoc("block/roof_tile_" + variant);
            cubes[variant] = models().cubeAll(family.baseId() + "_" + variant, texture);
            stairs[variant] = models().stairs(family.stairsId() + "_" + variant, texture, texture, texture);
            stairsInner[variant] = models().stairsInner(
                    family.stairsId() + "_inner_" + variant, texture, texture, texture
            );
            stairsOuter[variant] = models().stairsOuter(
                    family.stairsId() + "_outer_" + variant, texture, texture, texture
            );
            slabs[variant] = models().slab(family.slabId() + "_" + variant, texture, texture, texture);
            slabTops[variant] = models().slabTop(
                    family.slabId() + "_top_" + variant, texture, texture, texture
            );
        }

        simpleBlock(blocks.base().get(), weightedModels(cubes, 0, 0, false));
        simpleBlockItem(blocks.base().get(), cubes[0]);
        weightedStairsBlock(blocks.stairs().get(), stairs, stairsInner, stairsOuter);
        simpleBlockItem(blocks.stairs().get(), stairs[0]);
        getVariantBuilder(blocks.slab().get())
                .partialState().with(SlabBlock.TYPE, SlabType.BOTTOM)
                .addModels(weightedModels(slabs, 0, 0, false))
                .partialState().with(SlabBlock.TYPE, SlabType.TOP)
                .addModels(weightedModels(slabTops, 0, 0, false))
                .partialState().with(SlabBlock.TYPE, SlabType.DOUBLE)
                .addModels(weightedModels(cubes, 0, 0, false));
        simpleBlockItem(blocks.slab().get(), slabs[0]);
    }

    private void weightedStairsBlock(
            StairBlock block,
            ModelFile[] stairs,
            ModelFile[] stairsInner,
            ModelFile[] stairsOuter
    ) {
        getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction facing = state.getValue(StairBlock.FACING);
            Half half = state.getValue(StairBlock.HALF);
            StairsShape shape = state.getValue(StairBlock.SHAPE);
            int rotationY = (int) facing.getClockWise().toYRot();
            if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                rotationY += 270;
            }
            if (shape != StairsShape.STRAIGHT && half == Half.TOP) {
                rotationY += 90;
            }
            rotationY %= 360;
            ModelFile[] selected = shape == StairsShape.STRAIGHT
                    ? stairs
                    : shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT
                    ? stairsInner
                    : stairsOuter;
            return weightedModels(selected, half == Half.BOTTOM ? 0 : 180, rotationY,
                    rotationY != 0 || half == Half.TOP);
        }, StairBlock.WATERLOGGED);
    }

    /** The four released roof textures were orientation-selected evenly; weighted variants preserve that distribution. */
    private static ConfiguredModel[] weightedModels(
            ModelFile[] models,
            int rotationX,
            int rotationY,
            boolean uvLock
    ) {
        ConfiguredModel[] configured = new ConfiguredModel[models.length];
        for (int index = 0; index < models.length; index++) {
            configured[index] = new ConfiguredModel(models[index], rotationX, rotationY, uvLock, 1);
        }
        return configured;
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

        Block boxTransformer = ModNetworkBlocks.BOX_TRANSFORMER.get();
        ModelFile boxTransformerModel = gltfModel(
                "box_transformer",
                "box_transformer",
                modLoc("block/electrical_enclosure"),
                excludeSubtrees("arrow_forward", "arrow_reverse")
        );
        horizontalBlock(boxTransformer, boxTransformerModel);
        simpleBlockItem(boxTransformer, boxTransformerModel);

        ModelFile fuseBoxModel = gltfModel(
                "fuse_box",
                "fuse_box",
                modLoc("block/electrical_enclosure"),
                excludeNodes("fuse_intact", "fuse_blown")
        );
        horizontalBlock(ModNetworkBlocks.FUSE_BOX.get(), fuseBoxModel);
        simpleBlockItem(ModNetworkBlocks.FUSE_BOX.get(), fuseBoxModel);

        ModelFile circuitBreakerModel = gltfModel(
                "circuit_breaker",
                "circuit_breaker",
                modLoc("block/electrical_breaker_housing"),
                excludeNodes("switch_closed", "switch_open")
        );
        horizontalBlock(ModNetworkBlocks.CIRCUIT_BREAKER.get(), circuitBreakerModel);
        simpleBlockItem(ModNetworkBlocks.CIRCUIT_BREAKER.get(), circuitBreakerModel);

        electricalControlModel(
                ModNetworkBlocks.ELECTRIC_SWITCH.get(),
                "electric_switch",
                modLoc("block/electrical_breaker_housing"),
                modLoc("block/electrical_indicator")
        );
        electricalControlModel(
                ModNetworkBlocks.DIODE.get(),
                "diode",
                modLoc("block/electrical_indicator"),
                modLoc("blocks/ore_block/copper_block")
        );
        electricalControlModel(
                ModNetworkBlocks.RESISTOR.get(),
                "resistor",
                modLoc("blocks/ore_block/copper_block"),
                modLoc("block/electrical_indicator")
        );

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

    private void electricalControlModel(
            Block block,
            String name,
            ResourceLocation front,
            ResourceLocation top
    ) {
        ModelFile model = models().orientable(
                name,
                modLoc("block/electrical_enclosure"),
                front,
                top
        );
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
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
