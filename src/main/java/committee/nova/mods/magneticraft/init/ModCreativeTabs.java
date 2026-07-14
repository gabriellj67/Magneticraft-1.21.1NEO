package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.FloppyDiskItem;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.PortableEnergyItem;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.RegistryObject;

/**
 * Owns Magneticraft's player-facing creative inventory tab.
 */
public final class ModCreativeTabs {
    public static final String TRANSLATION_KEY = "itemGroup.magneticraft";
    private static final String PORTABLE_VARIANT_TAG = "magneticraft_portable_energy";
    private static final int PORTABLE_VARIANT_SCHEMA = 1;

    public static final RegistryObject<CreativeModeTab> MAIN = ModRegistries.CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(TRANSLATION_KEY))
                    .icon(() -> new ItemStack(ModItems.component(CraftingComponent.MAGNET).get()))
                    .displayItems((parameters, output) -> {
                        ModBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModMachineBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModNetworkBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModAdvancedBlocks.blockItems().stream().map(RegistryObject::get)
                                .forEach(item -> acceptAdvancedItem(output, item));
                        ModItems.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModMachineItems.creativeItems().stream()
                                .map(RegistryObject::get)
                                .forEach(item -> acceptMachineItem(output, item));
                        ModNetworkItems.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModComputerContent.creativeItems().stream()
                                .map(RegistryObject::get)
                                .forEach(item -> acceptComputerItem(output, item));
                        ModFluids.buckets().stream().map(RegistryObject::get).forEach(output::accept);
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
        charged.getCapability(ForgeCapabilities.ENERGY)
                .ifPresent(storage -> storage.receiveEnergy(portableEnergyItem.capacity(), false));
        var variant = charged.getOrCreateTagElement(PORTABLE_VARIANT_TAG);
        variant.putInt("schema_version", PORTABLE_VARIANT_SCHEMA);
        variant.putString("creative_variant", "full");
        output.accept(charged);
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
