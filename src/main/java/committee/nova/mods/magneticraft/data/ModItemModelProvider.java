package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.FloppyDiskVisualVariant;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.ElectricalFuseVisualVariant;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.material.NuclearMaterial;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
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
        registerNuclearItemModels();
        ModMachineItems.creativeItems().stream()
                .map(RegistryObject::get)
                .forEach(this::basicItem);
        ModNetworkItems.creativeItems().stream()
                .map(RegistryObject::get)
                .filter(item -> item != ModNetworkItems.FUSE.get()
                        && item != ModNetworkItems.ELECTRICAL_REPAIR_TOOL.get())
                .forEach(this::basicItem);
        registerElectricalFuseModels();
        withExistingParent("electrical_repair_tool", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/electrical_repair_tool"));
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

    private void registerNuclearItemModels() {
        ModNuclearItems.materials().forEach((material, holder) -> withExistingParent(
                holder.getId().getPath(),
                mcLoc("item/generated")
        ).texture("layer0", nuclearMaterialTexture(material)));
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            withExistingParent(grade.id(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/electric_piston"));
        }
        ModNuclearItems.controllerUpgrades().forEach((upgrade, holder) ->
                withExistingParent(upgrade.id(), mcLoc("item/generated"))
                        .texture("layer0", modLoc("item/inserter_speed_upgrade")));
    }

    private ResourceLocation nuclearMaterialTexture(NuclearMaterial material) {
        return switch (material) {
            case URANIUM_DUST, URANIUM_CONCENTRATE, LOW_ENRICHED_URANIUM, DEPLETED_URANIUM,
                    ZIRCON_SAND, ZIRCONIUM_DUST, ZIRCONIUM_ALLOY_BLEND, BORAX, BORON_DUST,
                    BORON_CARBIDE_BLEND, BORON_CARBIDE ->
                    modLoc("item/aluminium_dust");
            case EMPTY_URANIUM_HEXAFLUORIDE_CYLINDER, URANIUM_HEXAFLUORIDE_CYLINDER ->
                    modLoc("item/low_voltage_battery");
            case URANIUM_DIOXIDE_PELLET -> mcLoc("item/clay_ball");
            case ZIRCONIUM_ALLOY_INGOT -> modLoc("item/carbide_ingot");
            case ZIRCONIUM_ALLOY_CLADDING -> modLoc("item/copper_wire_coil");
        };
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

    private void registerElectricalFuseModels() {
        String itemId = ModNetworkItems.FUSE.getId().getPath();
        ItemModelBuilder root = withExistingParent(itemId, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + itemId));
        for (ElectricalFuseVisualVariant variant : ElectricalFuseVisualVariant.values()) {
            String variantId = itemId + "_" + variant.modelSuffix();
            ItemModelBuilder variantModel = withExistingParent(variantId, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + variantId));
            root.override()
                    .predicate(Magneticraft.id("fuse_variant"), variant.predicateValue())
                    .model(variantModel)
                    .end();
        }
    }
}
