package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.model.ModelSceneSelection;
import committee.nova.mods.magneticraft.client.model.ModelTransform;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
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
        horizontalBlock(battery, batteryModel);
        simpleBlockItem(battery, batteryModel);

        Block grate = ModMachineBlocks.GRATE.get();
        simpleBlockWithItem(
                grate,
                models().cubeAll("iron_grate", modLoc("block/iron_grate"))
                        .renderType(ResourceLocation.fromNamespaceAndPath("minecraft", "cutout"))
        );

        Block electricFurnace = ModMachineBlocks.ELECTRIC_FURNACE.get();
        ModelFile furnaceOff = mcxModel(
                "electric_furnace",
                "electric_furnace",
                modLoc("blocks/electric_machines/electric_furnace"),
                ModelSceneSelection.ALL
        );
        ModelFile furnaceOn = furnaceOff;
        horizontalBlock(
                electricFurnace,
                state -> state.getValue(ElectricFurnaceBlock.LIT) ? furnaceOn : furnaceOff
        );
        simpleBlockItem(electricFurnace, furnaceOff);

        ModMachineBlocks.machines().forEach((definition, holder) -> {
            Block block = holder.get();
            ModelFile historicalModel = historicalSingleBlockModel(definition);
            if (historicalModel != null) {
                registerSingleBlockMachineModel(block, definition, historicalModel);
                return;
            }
            if (definition == SingleBlockMachineDefinition.RELAY
                    || definition == SingleBlockMachineDefinition.FILTER
                    || definition == SingleBlockMachineDefinition.TRANSPOSER) {
                ModelFile model = pneumaticEndpointModel(definition);
                directionalBlock(block, model);
                simpleBlockItem(block, model);
            } else {
                ResourceLocation texture = definition.isWooden()
                        ? mcLoc("block/oak_planks")
                        : switch (definition) {
                            case COMBUSTION_CHAMBER, BRICK_FURNACE -> mcLoc("block/bricks");
                            case STEAM_BOILER, SMALL_TANK, WATER_GENERATOR -> mcLoc("block/iron_block");
                            case AIRLOCK -> mcLoc("block/glass");
                            default -> mcLoc("block/smooth_stone");
                        };
                simpleBlockWithItem(block, models().cubeAll(definition.id(), texture));
            }
        });
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
        conduitBlock(ModNetworkBlocks.HEAT_PIPE.get(), "heat_pipe", mcLoc("block/iron_block"), 4);
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
            ModelFile idle = advancedControllerModel(definition, false);
            ModelFile formed = advancedControllerModel(definition, true);
            horizontalBlock(
                    controller,
                    state -> state.getValue(AdvancedMultiblockBlock.FORMED) ? formed : idle
            );
            simpleBlockItem(controller, idle);
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

    private ModelFile historicalSingleBlockModel(SingleBlockMachineDefinition definition) {
        return switch (definition) {
            case COMBUSTION_CHAMBER -> mcxModel(
                    definition.id(), "combustion_chamber", modLoc("blocks/machines/combustion_gen_top"),
                    ModelSceneSelection.ALL
            );
            case STEAM_BOILER -> mcxModel(
                    definition.id(), "steam_boiler", modLoc("blocks/machines/boiler"),
                    ModelSceneSelection.ALL
            );
            case SMALL_TANK -> mcxModel(
                    definition.id(), "small_tank", modLoc("blocks/fluid_machines/small_tank"),
                    ModelSceneSelection.ALL
            );
            case GASIFICATION_UNIT -> mcxModel(
                    definition.id(), "gasification_unit", modLoc("blocks/machines/gasification_unit"),
                    ModelSceneSelection.ALL
            );
            case INSERTER -> gltfModel(
                    definition.id(), "inserter", modLoc("blocks/machines/inserter"),
                    new ModelSceneSelection(Set.of("Base1", "Base2"), Set.of(), Set.of(), Set.of())
            );
            case ELECTRIC_ENGINE -> gltfModel(
                    definition.id(), "electric_engine", modLoc("blocks/electric_machines/electric_engine"),
                    new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of("Group 18", "piston"))
            );
            default -> null;
        };
    }

    private void registerSingleBlockMachineModel(
            Block block,
            SingleBlockMachineDefinition definition,
            ModelFile model
    ) {
        if (definition.facingMode() == SingleBlockMachineDefinition.FacingMode.NONE) {
            simpleBlock(block, model);
        } else {
            directionalBlock(block, model);
        }
        simpleBlockItem(block, model);
    }

    private void legacyConduitBlock(Block block, String artifactName) {
        ResourceLocation particle = switch (artifactName) {
                    case "electric_cable" -> modLoc("blocks/electric_connectors/electric_cable");
                    case "insulated_heat_pipe" -> modLoc("blocks/fluid_machines/insulated_heat_pipe");
                    case "iron_fluid_pipe" -> modLoc("blocks/fluid_machines/iron_pipe");
                    case "pneumatic_tube", "pneumatic_restriction_tube" ->
                            modLoc("blocks/machines/pneumatic_tube");
                    default -> throw new IllegalArgumentException("Unknown historical conduit: " + artifactName);
                };
        ModelFile model = switch (artifactName) {
            case "pneumatic_tube", "pneumatic_restriction_tube" ->
                    gltfModel(artifactName, artifactName, particle, ModelSceneSelection.ALL);
            case "iron_fluid_pipe" -> mcxModel(artifactName, "iron_pipe", particle, ModelSceneSelection.ALL);
            default -> mcxModel(artifactName, artifactName, particle, ModelSceneSelection.ALL);
        };
        simpleBlockWithItem(block, model);
    }

    private ModelFile pneumaticEndpointModel(SingleBlockMachineDefinition definition) {
        ResourceLocation side = switch (definition) {
            case RELAY -> mcLoc("block/polished_andesite");
            case FILTER -> mcLoc("block/iron_block");
            case TRANSPOSER -> mcLoc("block/deepslate_tiles");
            default -> throw new IllegalArgumentException("Not a pneumatic endpoint: " + definition);
        };
        ResourceLocation front = switch (definition) {
            case RELAY -> mcLoc("block/copper_block");
            case FILTER -> modLoc("block/iron_grate");
            case TRANSPOSER -> mcLoc("block/dispenser_front");
            default -> throw new IllegalArgumentException("Not a pneumatic endpoint: " + definition);
        };
        ResourceLocation back = definition == SingleBlockMachineDefinition.FILTER
                ? mcLoc("block/copper_block")
                : mcLoc("block/iron_block");
        return models().cube(definition.id(), back, front, side, side, side, side);
    }

    private ModelFile advancedControllerModel(MultiblockDefinition definition, boolean formed) {
        String generatedName = definition.id() + (formed ? "_formed" : "");
        if (formed) {
            return emptyModel(generatedName, mcLoc("block/iron_block"));
        }
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

    private void conduitBlock(Block block, String name, ResourceLocation texture) {
        conduitBlock(block, name, texture, 5);
    }

    private void conduitBlock(Block block, String name, ResourceLocation texture, int insetPixels) {
        int far = 16 - insetPixels;
        BlockModelBuilder model = models().getBuilder(name)
                .texture("particle", texture)
                .texture("all", texture);
        model.element().from(insetPixels, insetPixels, insetPixels).to(far, far, far).textureAll("#all").end();
        model.element().from(insetPixels, 0, insetPixels).to(far, insetPixels, far).textureAll("#all").end();
        model.element().from(insetPixels, far, insetPixels).to(far, 16, far).textureAll("#all").end();
        model.element().from(insetPixels, insetPixels, 0).to(far, far, insetPixels).textureAll("#all").end();
        model.element().from(insetPixels, insetPixels, far).to(far, far, 16).textureAll("#all").end();
        model.element().from(0, insetPixels, insetPixels).to(insetPixels, far, far).textureAll("#all").end();
        model.element().from(far, insetPixels, insetPixels).to(16, far, far).textureAll("#all").end();
        simpleBlockWithItem(block, model);
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
        model.transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(InventoryModelTransform.GUI_ROTATION_X, InventoryModelTransform.GUI_ROTATION_Y, 0.0F)
                .translation(transform.translationX(), transform.translationY(), transform.translationZ())
                .scale(transform.scale())
                .end()
                .end();
        return model;
    }
}
