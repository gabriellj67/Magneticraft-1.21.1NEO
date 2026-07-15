package committee.nova.mods.magneticraft.client.electrical;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears server-specific display data when the client leaves a connection. */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientElectricalEvents {
    private ClientElectricalEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientVoltageTierRegistry.reset();
    }
}
