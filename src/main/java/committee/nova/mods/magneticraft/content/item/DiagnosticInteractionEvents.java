package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gives diagnostic instruments priority over machine block menus and direct interactions.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DiagnosticInteractionEvents {
    private DiagnosticInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Item item = event.getEntity().getItemInHand(event.getHand()).getItem();
        InteractionResult result;
        if (item instanceof VoltmeterItem) {
            result = VoltmeterItem.inspect(
                    event.getLevel(),
                    event.getPos(),
                    event.getHitVec().getDirection(),
                    event.getEntity()
            );
        } else if (item instanceof ThermometerItem) {
            result = ThermometerItem.inspect(
                    event.getLevel(),
                    event.getPos(),
                    event.getHitVec().getDirection(),
                    event.getEntity()
            );
        } else {
            return;
        }
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
