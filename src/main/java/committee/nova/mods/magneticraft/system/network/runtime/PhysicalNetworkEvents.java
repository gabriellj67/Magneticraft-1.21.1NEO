package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Explicitly releases the rebuildable runtime cache when a level closes.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
