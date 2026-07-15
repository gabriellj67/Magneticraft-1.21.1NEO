package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reads diagnostic instruments before block use without preempting the target block interaction.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DiagnosticInteractionEvents {
    private DiagnosticInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Item item = event.getEntity().getItemInHand(event.getHand()).getItem();
        if (item instanceof VoltmeterItem) {
            VoltmeterItem.inspect(
                    event.getLevel(),
                    event.getPos(),
                    event.getHitVec().getDirection(),
                    event.getEntity(),
                    event.getEntity().getItemInHand(event.getHand())
            );
        } else if (item instanceof ThermometerItem) {
            ThermometerItem.inspect(
                    event.getLevel(),
                    event.getPos(),
                    event.getHitVec().getDirection(),
                    event.getEntity()
            );
        } else {
            return;
        }
        event.setUseItem(Event.Result.DENY);
    }
}
