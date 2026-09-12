package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Single inventory truth with explicit sided insertion and extraction views.
 */
public final class ItemInventoryModule implements MachineModule {
    public static final SlotAccess NONE = new SlotAccess(new int[0], new int[0]);

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final int configuredSlots;
    private final BiPredicate<Integer, ItemStack> validator;
    private final Function<Direction, SlotAccess> accessBySide;
    private final ItemStackHandler handler;
    private final Map<Direction, IItemHandler> sidedViews = new EnumMap<>(Direction.class);
    @Nullable
    private IItemHandler unsidedView;

    public ItemInventoryModule(
            ResourceLocation id,
            MachineModuleHost host,
            int slots,
            BiPredicate<Integer, ItemStack> validator,
            Function<Direction, SlotAccess> accessBySide
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.configuredSlots = slots;
        this.validator = Objects.requireNonNull(validator);
        this.accessBySide = Objects.requireNonNull(accessBySide);
        this.handler = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return ItemInventoryModule.this.validator.test(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                ItemInventoryModule.this.host.markChanged();
            }
        };
        buildViews();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        handler.deserializeNBT(registries, tag);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.merge(handler.serializeNBT(registries));
    }

    @Override
    public void resetPersistentState(HolderLookup.Provider registries) {
        handler.deserializeNBT(registries, new ItemStackHandler(configuredSlots).serializeNBT(registries));
    }

    /** Item-handler view exposed for the given side, or {@code null} if this module hides all slots from it. */
    @Nullable
    public IItemHandler view(@Nullable Direction side) {
        return side == null ? unsidedView : sidedViews.get(side);
    }

    public IItemHandlerModifiable menuHandler() {
        return handler;
    }

    public int slots() {
        return handler.getSlots();
    }

    public ItemStack getStackInSlot(int slot) {
        return handler.getStackInSlot(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
    }

    public ItemStack extractInternal(int slot, int amount, boolean simulate) {
        return handler.extractItem(slot, amount, simulate);
    }

    /** Creates a non-owning view used by an exact multiblock structure port. */
    public IItemHandler portHandler(SlotAccess access) {
        if (access == null || access.isEmpty()) {
            throw new IllegalArgumentException("A multiblock item port must expose insertion or extraction");
        }
        return new RestrictedItemHandler(handler, access);
    }

    private void buildViews() {
        unsidedView = viewFor(null);
        for (Direction direction : Direction.values()) {
            IItemHandler view = viewFor(direction);
            if (view != null) {
                sidedViews.put(direction, view);
            }
        }
    }

    @Nullable
    private IItemHandler viewFor(@Nullable Direction side) {
        SlotAccess access = accessBySide.apply(side);
        return access == null || access.isEmpty() ? null : new RestrictedItemHandler(handler, access);
    }

    public record SlotAccess(int[] insertSlots, int[] extractSlots) {
        public SlotAccess {
            insertSlots = insertSlots.clone();
            extractSlots = extractSlots.clone();
        }

        public boolean canInsert(int slot) {
            return contains(insertSlots, slot);
        }

        public boolean canExtract(int slot) {
            return contains(extractSlots, slot);
        }

        public boolean isEmpty() {
            return insertSlots.length == 0 && extractSlots.length == 0;
        }

        private static boolean contains(int[] slots, int target) {
            for (int slot : slots) {
                if (slot == target) {
                    return true;
                }
            }
            return false;
        }
    }

    private record RestrictedItemHandler(IItemHandlerModifiable delegate, SlotAccess access) implements IItemHandler {
        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return access.canInsert(slot) ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return access.canExtract(slot) ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return access.canInsert(slot) && delegate.isItemValid(slot, stack);
        }
    }
}
