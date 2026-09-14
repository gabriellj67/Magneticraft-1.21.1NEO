package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Bounded, persistent FIFO buffers shared by the three legacy pneumatic endpoints.
 * The filter alone exposes its back face as an insertion-only tube endpoint.
 *
 * <p>Public (unlike most single-block-package helpers) so {@link
 * committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity#exposedItemHandler}
 * can recognize it via {@code instanceof} - the same "extend the generic module aggregator"
 * pattern used for {@code ShelvingStorageModule}/{@code ElectricalPowerModule} in Phase 4.</p>
 */
public final class PneumaticEndpointModule implements MachineModule {
    static final int MAX_PAYLOADS = 64;

    private static final String INPUT_ITEMS_TAG = "input_items";
    private static final String OUTPUT_ITEMS_TAG = "output_items";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final boolean acceptsExternalInput;
    private final Supplier<Direction> inputSide;
    private final Predicate<ItemStack> inputFilter;
    private final Deque<ItemStack> inputItems = new ArrayDeque<>();
    private final Deque<ItemStack> outputItems = new ArrayDeque<>();
    private final IItemHandler inputHandler;
    private boolean outputBlocked;

    PneumaticEndpointModule(
            ResourceLocation id,
            MachineModuleHost host,
            boolean acceptsExternalInput,
            Supplier<Direction> inputSide,
            Predicate<ItemStack> inputFilter
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.acceptsExternalInput = acceptsExternalInput;
        this.inputSide = Objects.requireNonNull(inputSide);
        this.inputFilter = Objects.requireNonNull(inputFilter);
        this.inputHandler = new InputHandler();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        inputItems.clear();
        outputItems.clear();
        loadQueue(registries, tag.getList(INPUT_ITEMS_TAG, Tag.TAG_COMPOUND), inputItems);
        loadQueue(registries, tag.getList(OUTPUT_ITEMS_TAG, Tag.TAG_COMPOUND), outputItems);
        outputBlocked = false;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(INPUT_ITEMS_TAG, saveQueue(registries, inputItems));
        tag.put(OUTPUT_ITEMS_TAG, saveQueue(registries, outputItems));
    }

    /** Item-handler view exposed for the given side, or {@code null} if this endpoint hides input from it. */
    @Nullable
    public IItemHandler view(@Nullable Direction side) {
        return acceptsExternalInput && side != null && side == inputSide.get() ? inputHandler : null;
    }

    boolean enqueueOutput(ItemStack stack) {
        if (stack.isEmpty() || !canBufferOutput()) {
            return false;
        }
        outputItems.addLast(normalizePayload(stack));
        outputBlocked = false;
        host.markChanged();
        return true;
    }

    boolean canBufferOutput() {
        return !outputBlocked && payloadCount() < MAX_PAYLOADS;
    }

    void moveInputsToOutput() {
        if (inputItems.isEmpty()) {
            return;
        }
        while (!inputItems.isEmpty()) {
            outputItems.addLast(inputItems.removeFirst());
        }
        host.markChanged();
    }

    @Nullable
    ItemStack outputHead() {
        ItemStack stack = outputItems.peekFirst();
        return stack == null ? null : stack.copy();
    }

    void replaceOutputHead(ItemStack remainder) {
        if (outputItems.isEmpty()) {
            return;
        }
        outputItems.removeFirst();
        if (!remainder.isEmpty()) {
            outputItems.addFirst(normalizePayload(remainder));
        }
        outputBlocked = false;
        host.markChanged();
    }

    void markOutputBlocked() {
        outputBlocked = true;
    }

    void clearOutputBlocked() {
        outputBlocked = false;
    }

    List<ItemStack> removeAllItems() {
        ArrayList<ItemStack> removed = new ArrayList<>(payloadCount());
        inputItems.forEach(stack -> removed.add(stack.copy()));
        outputItems.forEach(stack -> removed.add(stack.copy()));
        inputItems.clear();
        outputItems.clear();
        outputBlocked = false;
        host.markChanged();
        return List.copyOf(removed);
    }

    int payloadCount() {
        return inputItems.size() + outputItems.size();
    }

    private void loadQueue(HolderLookup.Provider registries, ListTag stored, Deque<ItemStack> target) {
        for (int index = 0; index < stored.size() && payloadCount() < MAX_PAYLOADS; index++) {
            ItemStack stack = ItemStack.parseOptional(registries, stored.getCompound(index));
            if (!stack.isEmpty()) {
                target.addLast(normalizePayload(stack));
            }
        }
    }

    private static ListTag saveQueue(HolderLookup.Provider registries, Deque<ItemStack> queue) {
        ListTag stored = new ListTag();
        queue.forEach(stack -> stored.add(stack.saveOptional(registries)));
        return stored;
    }

    private static ItemStack normalizePayload(ItemStack stack) {
        return stack.copy();
    }

    private final class InputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0
                    || stack.isEmpty()
                    || outputBlocked
                    || payloadCount() >= MAX_PAYLOADS
                    || !inputFilter.test(stack)) {
                return stack;
            }
            int accepted = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getSlotLimit(slot)));
            if (!simulate) {
                inputItems.addLast(stack.copyWithCount(accepted));
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
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty() && inputFilter.test(stack);
        }
    }
}
