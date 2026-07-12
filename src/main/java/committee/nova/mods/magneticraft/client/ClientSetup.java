package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only screen registration boundary.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.BATTERY.get(), BatteryScreen::new);
            MenuScreens.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.CRUSHING_TABLE.get(), CrushingTableRenderer::new);
        });
    }
}
