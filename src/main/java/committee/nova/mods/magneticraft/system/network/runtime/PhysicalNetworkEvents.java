package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Explicitly releases the rebuildable runtime cache when a level closes.
 */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PhysicalNetworkEvents {
    private PhysicalNetworkEvents() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PhysicalNetworkService.discard(serverLevel);
            LongDistanceElectricityService.discard(serverLevel);
        }
    }
}
