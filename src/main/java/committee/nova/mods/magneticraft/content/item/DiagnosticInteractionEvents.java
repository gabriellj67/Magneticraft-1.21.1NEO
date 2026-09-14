package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Reads diagnostic instruments before block use without preempting the target block interaction.
 */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
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
        event.setUseItem(TriState.FALSE);
    }
}
