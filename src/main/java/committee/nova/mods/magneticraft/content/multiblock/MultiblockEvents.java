package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps the rebuildable member index coherent with player structure edits.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MultiblockEvents {
    private MultiblockEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos controllerPosition = MultiblockMembershipService.controllerAt(level, event.getPos());
        if (controllerPosition != null
                && !controllerPosition.equals(event.getPos())
                && level.getBlockEntity(controllerPosition) instanceof AdvancedMultiblockBlockEntity controller) {
            controller.onMemberBroken(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onMemberInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos controllerPosition = MultiblockMembershipService.controllerAt(level, event.getPos());
        if (controllerPosition != null
                && !controllerPosition.equals(event.getPos())
                && level.getBlockEntity(controllerPosition) instanceof AdvancedMultiblockBlockEntity controller
                && controller.formed()) {
            controller.describe(player);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }
}
