package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
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
import net.minecraftforge.client.model.generators.loaders.ObjModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

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
        ModelFile crushingModel = legacyObjModel(
                "crushing_table",
                "crushing_table",
                modLoc("block/legacy/machines/crushing_table_side")
        );
        simpleBlockWithItem(crushingTable, crushingModel);

        Block battery = ModMachineBlocks.BATTERY.get();
        ModelFile batteryModel = legacyObjModel(
                "battery_box",
                "battery_box",
                modLoc("block/legacy/electric_machines/battery")
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
        ModelFile furnaceOff = legacyObjModel(
                "electric_furnace",
                "electric_furnace",
                modLoc("block/legacy/electric_machines/electric_furnace")
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
                legacyObjModel(
                        "tube_light",
                        "tube_light",
                        modLoc("block/legacy/decoration/tube_light")
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
        ModelFile heatSinkModel = legacyObjModel(
                "heat_sink",
                "heat_sink",
                modLoc("block/legacy/machines/heat_sink")
        );
        downFacingBlock(heatSink, heatSinkModel);
        simpleBlockItem(heatSink, heatSinkModel);

        Block conveyor = ModNetworkBlocks.CONVEYOR_BELT.get();
        ModelFile conveyorModel = legacyObjModel(
                "conveyor_belt",
                "conveyor_belt",
                modLoc("block/legacy/machines/conveyor_belt")
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
        ModelFile computerModel = legacyObjModel(
                "computer",
                "computer_body",
                modLoc("block/legacy/computers/computer1")
        );
        horizontalBlock(computer, computerModel);
        simpleBlockItem(computer, computerModel);

        Block miningRobot = ModComputerContent.MINING_ROBOT.get();
        ModelFile miningRobotModel = legacyObjModel(
                "mining_robot",
                "mining_robot_body",
                modLoc("block/legacy/computers/mining_robot")
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
            case COMBUSTION_CHAMBER -> legacyObjModel(
                    definition.id(), "combustion_chamber", modLoc("block/legacy/machines/combustion_gen_top")
            );
            case STEAM_BOILER -> legacyObjModel(
                    definition.id(), "steam_boiler", modLoc("block/legacy/machines/boiler")
            );
            case SMALL_TANK -> legacyObjModel(
                    definition.id(), "small_tank", modLoc("block/legacy/fluid_machines/small_tank")
            );
            case GASIFICATION_UNIT -> legacyObjModel(
                    definition.id(), "gasification_unit", modLoc("block/legacy/machines/gasification_unit")
            );
            case INSERTER -> legacyObjModel(
                    definition.id(), "inserter_body", modLoc("block/legacy/machines/inserter")
            );
            case ELECTRIC_ENGINE -> legacyObjModel(
                    definition.id(), "electric_engine_body", modLoc("block/legacy/electric_machines/electric_engine")
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
        ModelFile model = legacyObjModel(
                artifactName,
                artifactName,
                switch (artifactName) {
                    case "electric_cable" -> modLoc("block/legacy/electric_connectors/electric_cable");
                    case "insulated_heat_pipe" -> modLoc("block/legacy/fluid_machines/insulated_heat_pipe");
                    case "iron_fluid_pipe" -> modLoc("block/legacy/fluid_machines/iron_pipe");
                    case "pneumatic_tube", "pneumatic_restriction_tube" ->
                            modLoc("block/legacy/machines/pneumatic_tube");
                    default -> throw new IllegalArgumentException("Unknown historical conduit: " + artifactName);
                }
        );
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
        return switch (definition) {
            case GRINDER -> objModel(generatedName, "grinder_block", modLoc("block/grinder"));
            case HYDRAULIC_PRESS -> objModel(
                    generatedName,
                    "hydraulic_press_base",
                    modLoc("block/hydraulic_press")
            );
            case SOLAR_PANEL -> objModel(generatedName, "solar_panel_base", modLoc("block/solar_panel"));
            default -> models().orientable(
                    generatedName,
                    formed ? mcLoc("block/copper_block") : mcLoc("block/iron_block"),
                    formed ? mcLoc("block/redstone_lamp") : mcLoc("block/polished_andesite"),
                    mcLoc("block/iron_block")
            );
        };
    }

    private ModelFile objModel(String generatedName, String sourceName, ResourceLocation particleTexture) {
        ResourceLocation modelLocation = modLoc("models/block/" + sourceName + ".obj");
        BlockModelBuilder model = models().getBuilder(generatedName)
                .texture("particle", particleTexture)
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modelLocation)
                .flipV(true)
                .overrideMaterialLibrary(modLoc("models/block/" + sourceName + ".mtl"))
                .end();
        return withObjInventoryTransform(model, modelLocation);
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
                "electric_connector",
                modLoc("block/legacy/electric_connectors/connector")
        );
        historicalDirectionalBlock(
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(),
                "wireless_energy_receiver",
                modLoc("block/legacy/electric_connectors/energy_receiver")
        );

        poleModels(ModNetworkBlocks.ELECTRIC_POLE.get(), "electric_pole");
        poleModels(ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(), "electric_pole_transformer");

        Block teslaTower = ModNetworkBlocks.TESLA_TOWER.get();
        ModelFile teslaBottom = legacyObjModel(
                "tesla_tower_bottom",
                "tesla_tower",
                modLoc("block/legacy/electric_connectors/tesla_tower")
        );
        ModelFile teslaEmpty = emptyModel("tesla_tower_member", modLoc("block/legacy/electric_connectors/tesla_tower"));
        getVariantBuilder(teslaTower).forAllStates(state -> {
            TeslaTowerPart part = state.getValue(TeslaTowerBlock.PART);
            return ConfiguredModel.builder().modelFile(switch (part) {
                case BOTTOM -> teslaBottom;
                case MIDDLE, TOP -> teslaEmpty;
            }).build();
        });
        simpleBlockItem(teslaTower, teslaBottom);

        Block windTurbine = ModNetworkBlocks.WIND_TURBINE.get();
        ModelFile turbineModel = legacyObjModel(
                "wind_turbine",
                "wind_turbine_hub",
                modLoc("block/legacy/electric_machines/wind_turbine")
        );
        horizontalBlock(windTurbine, state -> turbineModel);
        simpleBlockItem(
                windTurbine,
                legacyObjModel(
                        "wind_turbine_inventory",
                        "wind_turbine_full",
                        modLoc("block/legacy/electric_machines/wind_turbine")
                )
        );
    }

    private void historicalDirectionalBlock(
            Block block,
            String name,
            ResourceLocation particleTexture
    ) {
        ModelFile model = legacyObjModel(name, name, particleTexture);
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
        ResourceLocation particle = modLoc("block/legacy/electric_connectors/" + name);
        ModelFile empty = emptyModel(name + "_world", particle);
        ModelFile inventory = legacyObjModel(name + "_inventory", name, particle);
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(empty)
                .build());
        simpleBlockItem(block, inventory);
    }

    private ModelFile emptyModel(String name, ResourceLocation particleTexture) {
        return models().getBuilder(name).texture("particle", particleTexture);
    }

    private ModelFile legacyObjModel(String generatedName, String artifactName, ResourceLocation particleTexture) {
        ResourceLocation modelLocation = modLoc("models/block/legacy/" + artifactName + ".obj");
        BlockModelBuilder model = models().getBuilder(generatedName)
                .texture("particle", particleTexture)
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modelLocation)
                .flipV(true)
                .overrideMaterialLibrary(modLoc("models/block/legacy/" + artifactName + ".mtl"))
                .end();
        return withObjInventoryTransform(model, modelLocation);
    }

    private ModelFile withObjInventoryTransform(BlockModelBuilder model, ResourceLocation modelLocation) {
        ObjInventoryTransform transform = ObjInventoryTransform.load(existingFileHelper, modelLocation);
        model.transforms()
                .transform(ItemDisplayContext.GUI)
                .rotation(ObjInventoryTransform.GUI_ROTATION_X, ObjInventoryTransform.GUI_ROTATION_Y, 0.0F)
                .translation(transform.translationX(), transform.translationY(), transform.translationZ())
                .scale(transform.scale())
                .end()
                .end();
        return model;
    }
}
