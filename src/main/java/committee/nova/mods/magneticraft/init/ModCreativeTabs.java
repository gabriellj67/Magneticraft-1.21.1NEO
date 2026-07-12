package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * Owns Magneticraft's player-facing creative inventory tab.
 */
public final class ModCreativeTabs {
    public static final String TRANSLATION_KEY = "itemGroup.magneticraft";

    public static final RegistryObject<CreativeModeTab> MAIN = ModRegistries.CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(TRANSLATION_KEY))
                    .icon(() -> new ItemStack(ModItems.component(CraftingComponent.MAGNET).get()))
                    .displayItems((parameters, output) -> {
                        ModBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModMachineBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModNetworkBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModAdvancedBlocks.blockItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModItems.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModMachineItems.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModNetworkItems.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModComputerContent.creativeItems().stream().map(RegistryObject::get).forEach(output::accept);
                        ModFluids.buckets().stream().map(RegistryObject::get).forEach(output::accept);
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void bootstrap() {
    }
}
