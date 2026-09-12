package committee.nova.mods.magneticraft.system.nuclear.data;

import committee.nova.mods.magneticraft.Magneticraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Common-server lifecycle wiring for nuclear data. */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class NuclearDataEvents {
    private NuclearDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(NuclearDataReloadListener.INSTANCE);
        event.addListener(ReactorParameterReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NuclearDataRegistry.INSTANCE.reset();
        ReactorParameterRegistry.INSTANCE.reset();
    }
}
