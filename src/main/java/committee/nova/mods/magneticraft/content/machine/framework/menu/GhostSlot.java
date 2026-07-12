package committee.nova.mods.magneticraft.content.machine.framework.menu;

import committee.nova.mods.magneticraft.content.machine.framework.module.GhostFilterModule;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Visual filter slot that never owns or consumes a real stack.
 */
public final class GhostSlot extends Slot {
    private final GhostFilterModule filters;
    private final int filterSlot;

    public GhostSlot(GhostFilterModule filters, int filterSlot, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.filters = filters;
        this.filterSlot = filterSlot;
    }

    @Override
    public ItemStack getItem() {
        return filters.getFilter(filterSlot);
    }

    @Override
    public void set(ItemStack stack) {
        filters.setFilter(filterSlot, stack);
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        set(stack);
        return stack;
    }
}
