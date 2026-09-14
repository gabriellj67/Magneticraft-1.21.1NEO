package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Keeps the guide item common while opening its client-only screen locally.
 */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, value = Dist.CLIENT)
public final class GuideClientEvents {
    private GuideClientEvents() {
    }

    @SubscribeEvent
    public static void onUseGuide(PlayerInteractEvent.RightClickItem event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || !event.getItemStack().is(ModItems.GUIDE_BOOK.get())) {
            return;
        }
        minecraft.setScreen(new GuideScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
