package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.FloppyDiskVisualVariant;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
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
                .map(RegistryObject::get)
                .forEach(this::basicItem);
        ModMachineItems.creativeItems().stream()
                .map(RegistryObject::get)
                .forEach(this::basicItem);
        ModNetworkItems.creativeItems().stream()
                .map(RegistryObject::get)
                .forEach(this::basicItem);
        registerFloppyDiskModels();

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

    private void registerFloppyDiskModels() {
        String itemId = ModComputerContent.FLOPPY_DISK.getId().getPath();
        ItemModelBuilder root = withExistingParent(itemId, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/floppy_disk_0"));
        for (FloppyDiskVisualVariant variant : FloppyDiskVisualVariant.values()) {
            if (variant == FloppyDiskVisualVariant.USER) {
                continue;
            }
            ItemModelBuilder variantModel = withExistingParent(
                    itemId + "_" + variant.preset(),
                    mcLoc("item/generated")
            ).texture("layer0", modLoc("item/floppy_disk_" + variant.textureIndex()));
            root.override()
                    .predicate(Magneticraft.id("floppy_variant"), variant.textureIndex())
                    .model(variantModel)
                    .end();
        }
    }
}
