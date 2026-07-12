package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

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
    private LazyOptional<IItemHandler> capability = LazyOptional.empty();

    public ShelvingStorageModule(ResourceLocation id, MachineModuleHost host) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        reviveCapabilities();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains(STORAGE_TAG, CompoundTag.TAG_COMPOUND)) {
            storage.deserializeNBT(tag.getCompound(STORAGE_TAG));
        }
        if (tag.contains(CHESTS_TAG, CompoundTag.TAG_COMPOUND)) {
            chests.deserializeNBT(tag.getCompound(CHESTS_TAG));
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
    public void save(CompoundTag tag) {
        tag.put(STORAGE_TAG, storage.serializeNBT());
        tag.put(CHESTS_TAG, chests.serializeNBT());
    }

    @Override
    public void invalidateCapabilities() {
        capability.invalidate();
        capability = LazyOptional.empty();
    }

    @Override
    public void reviveCapabilities() {
        capability = LazyOptional.of(() -> this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> requested, @Nullable Direction side) {
        return requested == ForgeCapabilities.ITEM_HANDLER ? capability.cast() : LazyOptional.empty();
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
