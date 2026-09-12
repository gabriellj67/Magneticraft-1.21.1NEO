package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Keeps the rebuildable member index coherent with player structure edits.
 */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
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
        if (isBlockPlacement(event.getItemStack().getItem())
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos controllerPosition = MultiblockMembershipService.controllerAt(level, event.getPos());
        if (controllerPosition != null
                && !controllerPosition.equals(event.getPos())
                && level.getBlockEntity(controllerPosition) instanceof AdvancedMultiblockBlockEntity controller
                && controller.formed()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    static boolean isBlockPlacement(Item item) {
        return item instanceof BlockItem;
    }
}
