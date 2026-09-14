package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.FloppyDiskItem;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.PortableEnergyItem;
import committee.nova.mods.magneticraft.content.item.TieredElectricalBlockItem;
import committee.nova.mods.magneticraft.content.item.ElectricalFuseItem;
import committee.nova.mods.magneticraft.content.item.ProtectionBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionKind;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleTransformerBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.TransformerBlockItem;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockItem;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Owns Magneticraft's player-facing creative inventory tab.
 */
public final class ModCreativeTabs {
    public static final String TRANSLATION_KEY = "itemGroup.magneticraft";
    private static final String PORTABLE_VARIANT_TAG = "magneticraft_portable_energy";
    private static final int PORTABLE_VARIANT_SCHEMA = 1;

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = ModRegistries.CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(TRANSLATION_KEY))
                    .icon(() -> new ItemStack(ModItems.component(CraftingComponent.MAGNET).get()))
                    .displayItems((parameters, output) -> {
                        ModBlocks.blockItems().stream().map(DeferredHolder::get).forEach(output::accept);
                        ModMachineBlocks.blockItems().stream().map(DeferredHolder::get)
                                .forEach(item -> acceptBlockItem(output, item));
                        ModNetworkBlocks.blockItems().stream().map(DeferredHolder::get)
                                .forEach(item -> acceptBlockItem(output, item));
                        ModAdvancedBlocks.blockItems().stream().map(DeferredHolder::get)
                                .forEach(item -> acceptAdvancedItem(output, item));
                        ModItems.creativeItems().stream().map(DeferredHolder::get).forEach(output::accept);
                        ModNuclearItems.creativeItems().stream().map(DeferredHolder::get).forEach(output::accept);
                        ModNuclearBlocks.blockItems().stream().map(DeferredHolder::get).forEach(output::accept);
                        ModMachineItems.creativeItems().stream()
                                .map(DeferredHolder::get)
                                .forEach(item -> acceptMachineItem(output, item));
                        ModNetworkItems.creativeItems().stream().map(DeferredHolder::get)
                                .forEach(item -> acceptNetworkItem(output, item));
                        ModComputerContent.creativeItems().stream()
                                .map(DeferredHolder::get)
                                .forEach(item -> acceptComputerItem(output, item));
                        ModFluids.buckets().stream().map(DeferredHolder::get).forEach(output::accept);
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    private static void acceptMachineItem(CreativeModeTab.Output output, Item item) {
        output.accept(item);
        if (!(item instanceof PortableEnergyItem portableEnergyItem)) {
            return;
        }
        ItemStack charged = new ItemStack(item);
        IEnergyStorage storage = charged.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage != null) {
            storage.receiveEnergy(portableEnergyItem.capacity(), false);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, charged, tag -> {
            CompoundTag variant = new CompoundTag();
            variant.putInt("schema_version", PORTABLE_VARIANT_SCHEMA);
            variant.putString("creative_variant", "full");
            tag.put(PORTABLE_VARIANT_TAG, variant);
        });
        output.accept(charged);
    }

    private static void acceptBlockItem(CreativeModeTab.Output output, Item item) {
        if (item instanceof TransformerBlockItem transformerItem) {
            output.accept(transformerItem.stackForProfile(TransformerProfileIds.LV_TO_MV, VoltageTierIds.LOW));
            output.accept(transformerItem.stackForProfile(TransformerProfileIds.MV_TO_HV, VoltageTierIds.MEDIUM));
            return;
        }
        if (item instanceof ElectricPoleTransformerBlockItem transformerPoleItem) {
            output.accept(transformerPoleItem.stackForProfile(TransformerProfileIds.LV_TO_MV, VoltageTierIds.LOW));
            output.accept(transformerPoleItem.stackForProfile(TransformerProfileIds.MV_TO_HV, VoltageTierIds.MEDIUM));
            return;
        }
        if (item instanceof ProtectionBlockItem protectionItem) {
            VoltageTierIds.BUILT_IN.forEach(tier -> {
                if (protectionItem.kind() == ElectricalProtectionKind.FUSE_BOX) {
                    output.accept(TieredElectricalBlockItem.stackForTier(protectionItem.getBlock(), tier));
                } else {
                    ElectricalRatingIds.BUILT_IN.forEach(rating ->
                            output.accept(protectionItem.stackFor(tier, rating))
                    );
                }
            });
            return;
        }
        if (!(item instanceof TieredElectricalBlockItem tieredItem)) {
            output.accept(item);
            return;
        }
        VoltageTierIds.BUILT_IN.forEach(tier -> output.accept(
                TieredElectricalBlockItem.stackForTier(tieredItem.getBlock(), tier)
        ));
    }

    private static void acceptNetworkItem(CreativeModeTab.Output output, Item item) {
        if (item instanceof ElectricalFuseItem fuse) {
            VoltageTierIds.BUILT_IN.forEach(tier -> ElectricalRatingIds.BUILT_IN.forEach(rating ->
                    output.accept(fuse.stackFor(tier, rating))
            ));
            return;
        }
        output.accept(item);
    }

    private static void acceptAdvancedItem(CreativeModeTab.Output output, Item item) {
        output.accept(item);
        if (item instanceof OilDepositBlockItem oilDeposit) {
            output.accept(oilDeposit.emptyStack());
        }
    }

    private static void acceptComputerItem(CreativeModeTab.Output output, Item item) {
        output.accept(item);
        if (!(item instanceof FloppyDiskItem)) {
            return;
        }
        acceptFloppyPreset(output, item, "forth", ScriptLanguage.FORTH);
        acceptFloppyPreset(output, item, "lisp", ScriptLanguage.LISP);
        acceptFloppyPreset(output, item, "shell", ScriptLanguage.SHELL);
        acceptFloppyPreset(output, item, "basic", null);
        acceptFloppyPreset(output, item, "editor", null);
        acceptFloppyPreset(output, item, "asm", null);
    }

    private static void acceptFloppyPreset(
            CreativeModeTab.Output output,
            Item item,
            String preset,
            ScriptLanguage language
    ) {
        ItemStack stack = new ItemStack(item);
        FloppyDiskItem.configurePreset(stack, preset, language);
        output.accept(stack);
    }

    public static void bootstrap() {
    }
}
