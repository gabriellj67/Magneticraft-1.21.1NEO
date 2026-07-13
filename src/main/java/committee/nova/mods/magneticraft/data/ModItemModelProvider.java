package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * Generates flat item models and Forge dynamic bucket models.
 */
final class ModItemModelProvider extends ItemModelProvider {
    ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Magneticraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ModItems.creativeItems().stream()
                .filter(holder -> holder != ModItems.GUIDE_BOOK)
                .map(RegistryObject::get)
                .forEach(this::basicItem);
        withExistingParent(ModItems.GUIDE_BOOK.getId().getPath(), mcLoc("item/book"));
        basicItem(ModMachineItems.LOW_BATTERY.get());
        basicItem(ModMachineItems.MEDIUM_BATTERY.get());
        handheld(ModMachineItems.ELECTRIC_DRILL);
        handheld(ModMachineItems.ELECTRIC_CHAINSAW);
        handheld(ModMachineItems.ELECTRIC_PISTON);
        handheld(ModMachineItems.VOLTMETER);
        handheld(ModMachineItems.THERMOMETER);
        withExistingParent(ModMachineItems.INSERTER_SPEED_UPGRADE.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/sugar"));
        withExistingParent(ModMachineItems.INSERTER_STACK_UPGRADE.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("block/oak_planks"));
        withExistingParent(ModNetworkItems.WRENCH.getId().getPath(), mcLoc("item/handheld"))
                .texture("layer0", mcLoc("item/iron_hoe"));
        withExistingParent(ModNetworkItems.COPPER_WIRE_COIL.getId().getPath(), mcLoc("item/handheld"))
                .texture("layer0", mcLoc("item/copper_ingot"));
        withExistingParent(ModComputerContent.FLOPPY_DISK.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", mcLoc("item/paper"));

        for (FluidDefinition definition : FluidDefinition.values()) {
            ModFluids.FluidFamily family = ModFluids.get(definition);
            withExistingParent(
                    definition.id() + "_bucket",
                    ResourceLocation.fromNamespaceAndPath("forge", "item/bucket")
            )
                    .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(family.source().get())
                    .end();
        }
    }

    private void handheld(RegistryObject<? extends net.minecraft.world.item.Item> item) {
        String path = item.getId().getPath();
        withExistingParent(path, mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + path));
    }
}
