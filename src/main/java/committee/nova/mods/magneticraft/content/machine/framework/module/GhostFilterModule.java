package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Non-consuming item filter samples used by generic machine menus.
 */
public final class GhostFilterModule implements MachineModule {
    private static final String FILTERS_TAG = "filters";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final List<ItemStack> filters;

    public GhostFilterModule(ResourceLocation id, MachineModuleHost host, int slots) {
        if (slots < 0) {
            throw new IllegalArgumentException("Ghost filter slot count must be non-negative");
        }
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.filters = new ArrayList<>(slots);
        for (int index = 0; index < slots; index++) {
            filters.add(ItemStack.EMPTY);
        }
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag stored = tag.getList(FILTERS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < filters.size(); index++) {
            ItemStack stack = index < stored.size()
                    ? ItemStack.parseOptional(registries, stored.getCompound(index))
                    : ItemStack.EMPTY;
            setFilterInternal(index, stack);
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag stored = new ListTag();
        for (ItemStack filter : filters) {
            stored.add(filter.save(registries));
        }
        tag.put(FILTERS_TAG, stored);
    }

    public int size() {
        return filters.size();
    }

    public ItemStack getFilter(int slot) {
        validateSlot(slot);
        return filters.get(slot).copy();
    }

    public void setFilter(int slot, ItemStack stack) {
        validateSlot(slot);
        ItemStack normalized = normalize(stack);
        if (!ItemStack.matches(filters.get(slot), normalized)) {
            filters.set(slot, normalized);
            host.markChanged();
        }
    }

    private void setFilterInternal(int slot, ItemStack stack) {
        filters.set(slot, normalize(stack));
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= filters.size()) {
            throw new IndexOutOfBoundsException("Ghost filter slot: " + slot);
        }
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
