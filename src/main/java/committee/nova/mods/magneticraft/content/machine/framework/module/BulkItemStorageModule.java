package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Objects;

/**
 * One-type, high-capacity inventory matching the legacy container contract.
 */
public final class BulkItemStorageModule implements MachineModule, IItemHandler {
    private static final String TYPE_TAG = "type";
    private static final String AMOUNT_TAG = "amount";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final int capacity;
    private ItemStack storedType = ItemStack.EMPTY;
    private int amount;

    public BulkItemStorageModule(ResourceLocation id, MachineModuleHost host, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Bulk-item capacity must be positive");
        }
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.capacity = capacity;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack loadedType = tag.contains(TYPE_TAG, CompoundTag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(TYPE_TAG))
                : ItemStack.EMPTY;
        int loadedAmount = Math.max(0, Math.min(capacity, tag.getInt(AMOUNT_TAG)));
        if (loadedType.isEmpty() || loadedAmount == 0) {
            storedType = ItemStack.EMPTY;
            amount = 0;
            return;
        }
        storedType = loadedType.copyWithCount(1);
        amount = loadedAmount;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!storedType.isEmpty() && amount > 0) {
            tag.put(TYPE_TAG, storedType.save(registries));
            tag.putInt(AMOUNT_TAG, amount);
        }
    }

    /** This module always exposes itself as the item handler, regardless of side. */
    public IItemHandler view() {
        return this;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        checkSlot(slot);
        return storedType.isEmpty()
                ? ItemStack.EMPTY
                : storedType.copyWithCount(Math.min(amount, storedType.getMaxStackSize()));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        checkSlot(slot);
        if (stack.isEmpty()
                || (!storedType.isEmpty() && !ItemStack.isSameItemSameComponents(storedType, stack))) {
            return stack;
        }
        int accepted = Math.min(stack.getCount(), capacity - amount);
        if (accepted <= 0) {
            return stack;
        }
        if (!simulate) {
            if (storedType.isEmpty()) {
                storedType = stack.copyWithCount(1);
            }
            amount += accepted;
            host.markChanged();
        }
        if (accepted == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(accepted);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int requested, boolean simulate) {
        checkSlot(slot);
        int extracted = Math.min(
                Math.max(0, requested),
                storedType.isEmpty() ? 0 : Math.min(amount, storedType.getMaxStackSize())
        );
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = storedType.copyWithCount(extracted);
        if (!simulate) {
            amount -= extracted;
            if (amount == 0) {
                storedType = ItemStack.EMPTY;
            }
            host.markChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        checkSlot(slot);
        return capacity;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        checkSlot(slot);
        return storedType.isEmpty() || ItemStack.isSameItemSameComponents(storedType, stack);
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        if (amount != 0 || !storedType.isEmpty()) {
            amount = 0;
            storedType = ItemStack.EMPTY;
            host.markChanged();
        }
    }

    private static void checkSlot(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException("Bulk item slot " + slot);
        }
    }
}
