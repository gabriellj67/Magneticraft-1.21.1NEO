package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
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
}
