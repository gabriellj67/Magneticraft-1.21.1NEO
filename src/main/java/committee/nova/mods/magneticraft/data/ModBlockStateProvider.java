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
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
    ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            Block block = ModBlocks.get(definition).get();
            simpleBlockWithItem(block, cubeAll(block));
        }

        Block crushingTable = ModMachineBlocks.CRUSHING_TABLE.get();
        BlockModelBuilder crushingModel = models().getBuilder("crushing_table")
                .texture("particle", modLoc("block/crushing_table_side"))
                .texture("side", modLoc("block/crushing_table_side"))
                .texture("bottom", modLoc("block/crushing_table_bottom"))
                .texture("top", modLoc("block/crushing_table_top"));
        crushingModel.element().from(1, 12, 1).to(15, 16, 15)
                .allFaces((direction, face) -> face.texture(direction == Direction.UP
                        ? "#top"
                        : direction == Direction.DOWN ? "#bottom" : "#side"))
                .end();
        addCrushingTableLeg(crushingModel, 2, 2, 5, 5);
        addCrushingTableLeg(crushingModel, 11, 2, 14, 5);
        addCrushingTableLeg(crushingModel, 2, 11, 5, 14);
        addCrushingTableLeg(crushingModel, 11, 11, 14, 14);
        simpleBlockWithItem(crushingTable, crushingModel);

        Block battery = ModMachineBlocks.BATTERY.get();
        ModelFile batteryModel = objModel("battery_box", "battery_box", modLoc("block/battery_box"));
        horizontalBlock(battery, batteryModel);
        simpleBlockItem(battery, batteryModel);

        Block grate = ModMachineBlocks.GRATE.get();
        simpleBlockWithItem(
                grate,
                models().cubeAll("iron_grate", modLoc("block/iron_grate"))
                        .renderType(ResourceLocation.fromNamespaceAndPath("minecraft", "cutout"))
        );

        Block electricFurnace = ModMachineBlocks.ELECTRIC_FURNACE.get();
        ModelFile furnaceOff = models().orientable(
                "electric_furnace",
                modLoc("block/electric_furnace_side"),
                modLoc("block/electric_furnace_front"),
                modLoc("block/electric_furnace_side")
        );
        ModelFile furnaceOn = models().orientable(
                "electric_furnace_on",
                modLoc("block/electric_furnace_side"),
                modLoc("block/electric_furnace_front_on"),
                modLoc("block/electric_furnace_side")
        );
        horizontalBlock(
                electricFurnace,
                state -> state.getValue(ElectricFurnaceBlock.LIT) ? furnaceOn : furnaceOff
        );
        simpleBlockItem(electricFurnace, furnaceOff);

        ModMachineBlocks.machines().forEach((definition, holder) -> {
            Block block = holder.get();
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
                models().cubeAll("tube_light", mcLoc("block/glowstone"))
        );

        conduitBlock(ModNetworkBlocks.ELECTRIC_CABLE.get(), "electric_cable", mcLoc("block/copper_block"));
        registerLongDistanceElectricModels();
        conduitBlock(ModNetworkBlocks.HEAT_PIPE.get(), "heat_pipe", mcLoc("block/iron_block"), 4);
        conduitBlock(
                ModNetworkBlocks.INSULATED_HEAT_PIPE.get(),
                "insulated_heat_pipe",
                mcLoc("block/black_wool"),
                3
        );
        conduitBlock(ModNetworkBlocks.IRON_PIPE.get(), "iron_fluid_pipe", mcLoc("block/iron_block"), 4);
        conduitBlock(ModNetworkBlocks.PNEUMATIC_TUBE.get(), "pneumatic_tube", mcLoc("block/light_gray_concrete"));
        conduitBlock(
                ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(),
                "pneumatic_restriction_tube",
                mcLoc("block/red_concrete")
        );

        Block heatSink = ModNetworkBlocks.HEAT_SINK.get();
        BlockModelBuilder heatSinkModel = models().getBuilder("heat_sink")
                .texture("particle", mcLoc("block/iron_block"))
                .texture("metal", mcLoc("block/iron_block"))
                .texture("face", modLoc("block/iron_grate"));
        heatSinkModel.element().from(0, 11, 0).to(16, 13, 16).textureAll("#metal").end();
        for (int x = 1; x <= 13; x += 3) {
            heatSinkModel.element().from(x, 14, 1).to(x + 1, 16, 15).textureAll("#metal").end();
        }
        heatSinkModel.element().from(2, 13, 2).to(14, 14, 14).textureAll("#face").end();
        directionalBlock(heatSink, heatSinkModel);
        simpleBlockItem(heatSink, heatSinkModel);

        Block conveyor = ModNetworkBlocks.CONVEYOR_BELT.get();
        BlockModelBuilder conveyorModel = models().getBuilder("conveyor_belt")
                .texture("particle", mcLoc("block/black_concrete"))
                .texture("belt", mcLoc("block/black_concrete"))
                .texture("rail", mcLoc("block/iron_block"));
        conveyorModel.element().from(0, 0, 0).to(16, 3, 16).textureAll("#belt").end();
        conveyorModel.element().from(0, 3, 0).to(2, 4, 16).textureAll("#rail").end();
        conveyorModel.element().from(14, 3, 0).to(16, 4, 16).textureAll("#rail").end();
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

        simpleBlock(
                ModAdvancedBlocks.OIL_DEPOSIT.get(),
                models().cubeAll("oil_deposit", mcLoc("block/deepslate"))
        );

        Block computer = ModComputerContent.COMPUTER.get();
        ModelFile computerModel = objModel("computer", "computer", modLoc("block/computer"));
        horizontalBlock(computer, computerModel);
        simpleBlockItem(computer, computerModel);

        Block miningRobot = ModComputerContent.MINING_ROBOT.get();
        ModelFile miningRobotModel = models().orientable(
                "mining_robot",
                mcLoc("block/copper_block"),
                mcLoc("block/dispenser_front"),
                mcLoc("block/iron_block")
        );
        horizontalBlock(miningRobot, miningRobotModel);
        simpleBlockItem(miningRobot, miningRobotModel);

        for (FluidDefinition definition : FluidDefinition.values()) {
            ModelFile model = models().getBuilder(definition.id())
                    .texture("particle", Magneticraft.id("fluid/" + definition.id() + "_still"));
            simpleBlock(ModFluids.get(definition).block().get(), model);
        }
    }

    private static void addCrushingTableLeg(
            BlockModelBuilder model,
            int minX,
            int minZ,
            int maxX,
            int maxZ
    ) {
        model.element().from(minX, 0, minZ).to(maxX, 12, maxZ).textureAll("#side").end();
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
        return models().getBuilder(generatedName)
                .texture("particle", particleTexture)
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/" + sourceName + ".obj"))
                .flipV(true)
                .overrideMaterialLibrary(modLoc("models/block/" + sourceName + ".mtl"))
                .end();
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
        wallMountedEndpoint(
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                "electric_connector",
                mcLoc("block/copper_block"),
                mcLoc("block/iron_block")
        );
        wallMountedEndpoint(
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(),
                "wireless_energy_receiver",
                mcLoc("block/copper_block"),
                mcLoc("block/redstone_block")
        );

        poleModels(ModNetworkBlocks.ELECTRIC_POLE.get(), "electric_pole", false);
        poleModels(ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(), "electric_pole_transformer", true);

        Block teslaTower = ModNetworkBlocks.TESLA_TOWER.get();
        ModelFile teslaBottom = teslaPartModel("tesla_tower_bottom", 2, 14);
        ModelFile teslaMiddle = teslaPartModel("tesla_tower_middle", 5, 11);
        ModelFile teslaTop = teslaTopModel();
        getVariantBuilder(teslaTower).forAllStates(state -> {
            TeslaTowerPart part = state.getValue(TeslaTowerBlock.PART);
            return ConfiguredModel.builder().modelFile(switch (part) {
                case BOTTOM -> teslaBottom;
                case MIDDLE -> teslaMiddle;
                case TOP -> teslaTop;
            }).build();
        });
        simpleBlockItem(teslaTower, teslaBottom);

        Block windTurbine = ModNetworkBlocks.WIND_TURBINE.get();
        ModelFile turbineModel = models().orientable(
                "wind_turbine",
                mcLoc("block/iron_block"),
                mcLoc("block/quartz_block_side"),
                mcLoc("block/copper_block")
        );
        horizontalBlock(windTurbine, state -> turbineModel);
        simpleBlockItem(windTurbine, turbineModel);
    }

    private void wallMountedEndpoint(
            Block block,
            String name,
            ResourceLocation bodyTexture,
            ResourceLocation faceTexture
    ) {
        BlockModelBuilder model = models().getBuilder(name)
                .texture("particle", bodyTexture)
                .texture("body", bodyTexture)
                .texture("face", faceTexture);
        model.element().from(5, 0, 5).to(11, 5, 11).textureAll("#body").end();
        model.element().from(3, 5, 3).to(13, 8, 13).textureAll("#face").end();
        directionalBlock(block, model);
        simpleBlockItem(block, model);
    }

    private void poleModels(Block block, String name, boolean transformer) {
        ModelFile post = models().getBuilder(name + "_post")
                .texture("particle", mcLoc("block/oak_log"))
                .texture("wood", mcLoc("block/oak_log"))
                .element().from(6, 0, 6).to(10, 16, 10).textureAll("#wood").end();
        BlockModelBuilder base = models().getBuilder(name + "_base")
                .texture("particle", mcLoc("block/oak_log"))
                .texture("wood", mcLoc("block/oak_log"))
                .texture("metal", transformer ? mcLoc("block/copper_block") : mcLoc("block/iron_block"));
        base.element().from(6, 0, 6).to(10, 16, 10).textureAll("#wood").end();
        base.element().from(1, 10, 5).to(15, 14, 11).textureAll("#wood").end();
        base.element().from(2, 14, 6).to(5, 16, 10).textureAll("#metal").end();
        base.element().from(11, 14, 6).to(14, 16, 10).textureAll("#metal").end();
        if (transformer) {
            base.element().from(4, 3, 4).to(12, 10, 12).textureAll("#metal").end();
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ElectricPoleBlock.SEGMENT) == PoleSegment.BASE ? base : post)
                .build());
        simpleBlockItem(block, base);
    }

    private ModelFile teslaPartModel(String name, int min, int max) {
        BlockModelBuilder model = models().getBuilder(name)
                .texture("particle", mcLoc("block/copper_block"))
                .texture("copper", mcLoc("block/copper_block"))
                .texture("iron", mcLoc("block/iron_block"));
        model.element().from(min, 0, min).to(max, 16, max).textureAll("#iron").end();
        model.element().from(0, 1, 7).to(16, 4, 9).textureAll("#copper").end();
        model.element().from(7, 1, 0).to(9, 4, 16).textureAll("#copper").end();
        return model;
    }

    private ModelFile teslaTopModel() {
        BlockModelBuilder model = models().getBuilder("tesla_tower_top")
                .texture("particle", mcLoc("block/copper_block"))
                .texture("copper", mcLoc("block/copper_block"))
                .texture("iron", mcLoc("block/iron_block"));
        model.element().from(6, 0, 6).to(10, 9, 10).textureAll("#iron").end();
        model.element().from(2, 7, 2).to(14, 11, 14).textureAll("#copper").end();
        model.element().from(5, 11, 5).to(11, 16, 11).textureAll("#copper").end();
        return model;
    }
}
