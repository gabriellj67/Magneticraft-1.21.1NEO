package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Legacy shelving storage: every installed vanilla chest unlocks 27 of the
 * 648 backing slots, up to 24 chests.
 */
public final class ShelvingStorageModule implements MachineModule, IItemHandler {
    public static final int MAX_CHESTS = 24;
    public static final int SLOTS_PER_CHEST = 27;
    public static final int MAX_STORAGE_SLOTS = MAX_CHESTS * SLOTS_PER_CHEST;

    private static final String STORAGE_TAG = "storage";
    private static final String CHESTS_TAG = "chests";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ItemStackHandler storage = new ItemStackHandler(MAX_STORAGE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            host.markChanged();
        }
    };
    private final ItemStackHandler chests = new ItemStackHandler(MAX_CHESTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isChestUpgrade(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            host.markChangedAndSync();
        }
    };

    public ShelvingStorageModule(ResourceLocation id, MachineModuleHost host) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        resetPersistentState(registries);
        if (tag.contains(STORAGE_TAG, CompoundTag.TAG_COMPOUND)) {
            storage.deserializeNBT(registries, tag.getCompound(STORAGE_TAG));
        }
        if (tag.contains(CHESTS_TAG, CompoundTag.TAG_COMPOUND)) {
            chests.deserializeNBT(registries, tag.getCompound(CHESTS_TAG));
        }
        for (int slot = 0; slot < chests.getSlots(); slot++) {
            ItemStack stack = chests.getStackInSlot(slot);
            if (!stack.isEmpty() && !isChestUpgrade(stack)) {
                chests.setStackInSlot(slot, ItemStack.EMPTY);
            } else if (stack.getCount() > 1) {
                chests.setStackInSlot(slot, stack.copyWithCount(1));
            }
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(STORAGE_TAG, storage.serializeNBT(registries));
        tag.put(CHESTS_TAG, chests.serializeNBT(registries));
    }

    @Override
    public void resetPersistentState(HolderLookup.Provider registries) {
        storage.deserializeNBT(registries, new ItemStackHandler(MAX_STORAGE_SLOTS).serializeNBT(registries));
        chests.deserializeNBT(registries, new ItemStackHandler(MAX_CHESTS).serializeNBT(registries));
    }

    /** This module always exposes itself as the item handler, regardless of side. */
    public IItemHandler view() {
        return this;
    }

    public boolean installChest(ItemStack stack) {
        if (!isChestUpgrade(stack)) {
            return false;
        }
        for (int slot = 0; slot < chests.getSlots(); slot++) {
            if (chests.getStackInSlot(slot).isEmpty()) {
                chests.setStackInSlot(slot, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    public int installedChests() {
        int count = 0;
        for (int slot = 0; slot < chests.getSlots(); slot++) {
            if (!chests.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int unlockedSlots() {
        return installedChests() * SLOTS_PER_CHEST;
    }

    public List<ItemStack> dropContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            ItemStack stack = storage.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        for (int slot = 0; slot < chests.getSlots(); slot++) {
            ItemStack stack = chests.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return List.copyOf(drops);
    }

    public static boolean isChestUpgrade(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ChestBlock;
    }

    @Override
    public int getSlots() {
        return unlockedSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        checkUnlockedSlot(slot);
        return storage.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        checkUnlockedSlot(slot);
        return storage.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkUnlockedSlot(slot);
        return storage.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        checkUnlockedSlot(slot);
        return storage.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        checkUnlockedSlot(slot);
        return storage.isItemValid(slot, stack);
    }

    private void checkUnlockedSlot(int slot) {
        if (slot < 0 || slot >= unlockedSlots()) {
            throw new IndexOutOfBoundsException("Shelving slot " + slot + " is not unlocked");
        }
    }
}
