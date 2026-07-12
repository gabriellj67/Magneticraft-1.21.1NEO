package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the guide item common while opening its client-only screen locally.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, value = Dist.CLIENT)
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
