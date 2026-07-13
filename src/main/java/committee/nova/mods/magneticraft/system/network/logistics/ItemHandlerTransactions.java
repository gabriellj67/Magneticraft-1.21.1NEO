package committee.nova.mods.magneticraft.system.network.logistics;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * Slot-agnostic transactional insertion shared by tubes and conveyors.
 */
public final class ItemHandlerTransactions {
    private ItemHandlerTransactions() {
    }

    public static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    public static boolean acceptsAll(IItemHandler handler, ItemStack stack) {
        return insert(handler, stack, true).isEmpty();
    }

    /**
     * Commits only after a full simulated acceptance. A handler that changes
     * between simulation and execution may still return a partial remainder;
     * callers must retain that remainder as the authoritative source payload.
     */
    public static ItemStack insertAfterFullSimulation(IItemHandler handler, ItemStack stack) {
        return acceptsAll(handler, stack) ? insert(handler, stack, false) : stack.copy();
    }
}
