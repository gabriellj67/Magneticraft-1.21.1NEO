package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
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
        ModelFile batteryModel = models().getBuilder("battery")
                .texture("particle", modLoc("block/battery"))
                .customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/battery.obj"))
                .flipV(true)
                .overrideMaterialLibrary(modLoc("models/block/battery.mtl"))
                .end();
        horizontalBlock(battery, batteryModel);
        simpleBlockItem(battery, batteryModel);

        Block grate = ModMachineBlocks.GRATE.get();
        simpleBlockWithItem(
                grate,
                models().cubeAll("grate", modLoc("block/grate"))
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
            ResourceLocation texture = definition.isWooden()
                    ? mcLoc("block/oak_planks")
                    : switch (definition) {
                        case COMBUSTION_CHAMBER, BRICK_FURNACE -> mcLoc("block/bricks");
                        case STEAM_BOILER, SMALL_TANK, WATER_GENERATOR -> mcLoc("block/iron_block");
                        case AIRLOCK -> mcLoc("block/glass");
                        default -> mcLoc("block/smooth_stone");
                    };
            simpleBlockWithItem(block, models().cubeAll(definition.id(), texture));
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
        conduitBlock(ModNetworkBlocks.HEAT_PIPE.get(), "heat_pipe", mcLoc("block/iron_block"));
        conduitBlock(ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), "insulated_heat_pipe", mcLoc("block/black_wool"));
        conduitBlock(ModNetworkBlocks.IRON_PIPE.get(), "iron_pipe", mcLoc("block/iron_block"));
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
                .texture("face", modLoc("block/grate"));
        heatSinkModel.element().from(2, 2, 2).to(14, 14, 14).textureAll("#metal").end();
        for (int x = 1; x <= 13; x += 3) {
            heatSinkModel.element().from(x, 0, 0).to(x + 1, 16, 2).textureAll("#metal").end();
        }
        heatSinkModel.element().from(2, 2, 0).to(14, 14, 2).textureAll("#face").end();
        horizontalBlock(heatSink, heatSinkModel);
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

    private void conduitBlock(Block block, String name, ResourceLocation texture) {
        BlockModelBuilder model = models().getBuilder(name)
                .texture("particle", texture)
                .texture("all", texture);
        model.element().from(5, 5, 5).to(11, 11, 11).textureAll("#all").end();
        model.element().from(5, 0, 5).to(11, 5, 11).textureAll("#all").end();
        model.element().from(5, 11, 5).to(11, 16, 11).textureAll("#all").end();
        model.element().from(5, 5, 0).to(11, 11, 5).textureAll("#all").end();
        model.element().from(5, 5, 11).to(11, 11, 16).textureAll("#all").end();
        model.element().from(0, 5, 5).to(5, 11, 11).textureAll("#all").end();
        model.element().from(11, 5, 5).to(16, 11, 11).textureAll("#all").end();
        simpleBlockWithItem(block, model);
    }
}
