package committee.nova.mods.magneticraft.system.nuclear.data;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Common-server lifecycle wiring for nuclear data. */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NuclearDataEvents {
    private NuclearDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(NuclearDataReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NuclearDataRegistry.INSTANCE.reset();
    }
}
