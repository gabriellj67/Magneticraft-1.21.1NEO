package committee.nova.mods.magneticraft.system.network.electric.profile;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.SyncVoltageTiersMessage;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server lifecycle wiring for atomic electrical data and its display projection. */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
            event.getPlayers().forEach(player -> ModNetwork.syncVoltageTiers(player, message));
        });
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ElectricalDataRegistry.INSTANCE.reset();
        PhysicalNetworkService.discardAll();
    }
}
