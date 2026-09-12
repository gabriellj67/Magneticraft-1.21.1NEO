package committee.nova.mods.magneticraft.system.network.electric.profile;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.SyncVoltageTiersMessage;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Server lifecycle wiring for atomic electrical data and its display projection. */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ElectricalDataEvents {
    private ElectricalDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ElectricalDataReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ElectricalDataRegistry.INSTANCE.current().ifPresent(snapshot -> {
            SyncVoltageTiersMessage message = SyncVoltageTiersMessage.from(snapshot);
            event.getRelevantPlayers().forEach(player -> ModNetwork.syncVoltageTiers(player, message));
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ElectricalDataRegistry.INSTANCE.reset();
        PhysicalNetworkService.discardAll();
    }
}
