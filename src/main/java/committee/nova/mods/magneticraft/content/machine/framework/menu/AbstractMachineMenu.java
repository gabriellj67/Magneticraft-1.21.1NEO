package committee.nova.mods.magneticraft.content.machine.framework.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Shared, range-safe player inventory layout and shift-click behavior.
 */
public abstract class AbstractMachineMenu extends AbstractContainerMenu {
    private int machineSlotCount = -1;

    protected AbstractMachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    protected final void finishMachineSlots(Inventory inventory, int left, int top) {
        if (machineSlotCount >= 0) {
            throw new IllegalStateException("Machine slot layout already finalized");
        }
        machineSlotCount = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || machineSlotCount < 0) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        if (!source.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < machineSlotCount
                ? moveItemStackTo(stack, machineSlotCount, slots.size(), true)
                : movePlayerStackToMachine(stack);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        source.onTake(player, stack);
        return original;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostSlot ghostSlot) {
            if (clickType == ClickType.PICKUP) {
                ghostSlot.set(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    protected abstract boolean movePlayerStackToMachine(ItemStack stack);
}
