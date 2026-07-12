package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsRouteDecision;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bounded, persisted item boxes moving through a weighted pneumatic graph.
 */
public final class LogisticsTubeModule extends AbstractPhysicalNetworkModule implements LogisticsNetworkNode {
    public static final int MAX_PROGRESS = 128;
    public static final int CENTER_PROGRESS = MAX_PROGRESS / 2;
    public static final int PROGRESS_PER_TICK = 16;
    public static final int MAX_PAYLOADS = 64;
    public static final int MAX_ROUTE_VISITS = 4_096;

    private static final String ITEMS_TAG = "items";
    private static final String ROUTE_CURSOR_TAG = "route_cursor";

    private final int routingWeight;
    private final List<TravelingItem> items = new ArrayList<>();
    private final Map<Direction, LazyOptional<IItemHandler>> capabilities = new EnumMap<>(Direction.class);
    private int routeCursor;

    public LogisticsTubeModule(
            ResourceLocation id,
            MachineModuleHost host,
            int routingWeight
    ) {
        super(id, host, NetworkDomain.LOGISTICS);
        if (routingWeight < 0) {
            throw new IllegalArgumentException("Routing weight must be non-negative");
        }
        this.routingWeight = routingWeight;
        reviveCapabilities();
    }

    @Override
    public int routingWeight() {
        return routingWeight;
    }

    public List<TravelingItemView> itemsSnapshot() {
        return items.stream()
                .map(item -> new TravelingItemView(
                        item.stack.copy(),
                        item.progress,
                        item.incoming,
                        item.outgoing
                ))
                .toList();
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> removed = items.stream().map(item -> item.stack.copy()).toList();
        items.clear();
        markStateChangedAndSync();
        return removed;
    }

    @Override
    public boolean canConnect(Direction side, PhysicalNetworkNode other) {
        return other instanceof LogisticsTubeModule && super.canConnect(side, other);
    }

    @Override
    public List<Direction> acceptingExternalOutputs(ItemStack stack) {
        if (!(host().level() instanceof ServerLevel level) || stack.isEmpty()) {
            return List.of();
        }
        PhysicalNetworkManager manager = manager();
        ArrayList<Direction> result = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (!isSideEnabled(direction)
                    || (manager != null
                    && manager.node(NetworkDomain.LOGISTICS, position().relative(direction)).isPresent())) {
                continue;
            }
            IItemHandler handler = adjacentHandler(level, direction);
            if (handler != null && ItemHandlerTransactions.acceptsAll(handler, stack)) {
                result.add(direction);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void afterNetworkTick(PhysicalNetworkManager manager) {
        if (!automationEnabled() || items.isEmpty()) {
            return;
        }
        long gameTime = manager.level().getGameTime();
        boolean changed = false;
        boolean structuralChange = false;
        List<TravelingItem> removed = new ArrayList<>();

        for (TravelingItem item : List.copyOf(items)) {
            if (item.lastMovedTick == gameTime) {
                continue;
            }
            item.lastMovedTick = gameTime;

            if (item.progress < CENTER_PROGRESS) {
                item.progress = Math.min(CENTER_PROGRESS, item.progress + PROGRESS_PER_TICK);
                changed = true;
            }
            if (item.progress == CENTER_PROGRESS && item.outgoing == null) {
                Optional<LogisticsRouteDecision> route = manager.findLogisticsRoute(
                        position(),
                        item.incoming,
                        item.stack,
                        routeCursor,
                        MAX_ROUTE_VISITS
                );
                if (route.isPresent()) {
                    item.outgoing = route.get().direction();
                    routeCursor++;
                    structuralChange = true;
                }
            }
            if (item.outgoing != null && item.progress < MAX_PROGRESS) {
                item.progress = Math.min(MAX_PROGRESS, item.progress + PROGRESS_PER_TICK);
                changed = true;
            }
            if (item.progress >= MAX_PROGRESS && item.outgoing != null) {
                TransferOutcome outcome = transferOut(manager, item, gameTime);
                if (outcome.remove()) {
                    removed.add(item);
                    structuralChange = true;
                } else if (outcome.remaining() != null) {
                    item.stack = outcome.remaining();
                    item.progress = MAX_PROGRESS;
                    structuralChange = true;
                }
            }
        }

        if (!removed.isEmpty()) {
            items.removeAll(removed);
        }
        if (structuralChange || (changed && gameTime % 4L == 0L)) {
            markStateChangedAndSync();
        } else if (changed) {
            markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        items.clear();
        ListTag stored = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size() && items.size() < MAX_PAYLOADS; index++) {
            TravelingItem item = TravelingItem.load(stored.getCompound(index));
            if (!item.stack.isEmpty()) {
                items.add(item);
            }
        }
        routeCursor = Math.max(0, tag.getInt(ROUTE_CURSOR_TAG));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        ListTag stored = new ListTag();
        items.forEach(item -> stored.add(item.save()));
        tag.put(ITEMS_TAG, stored);
        tag.putInt(ROUTE_CURSOR_TAG, routeCursor);
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        loadNetworkData(tag);
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        saveNetworkData(tag);
    }

    @Override
    public void invalidateCapabilities() {
        capabilities.values().forEach(LazyOptional::invalidate);
        capabilities.clear();
    }

    @Override
    public void reviveCapabilities() {
        for (Direction direction : Direction.values()) {
            capabilities.put(direction, LazyOptional.of(() -> new TubeItemHandler(direction)));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side != null && isSideEnabled(side)) {
            LazyOptional<IItemHandler> result = capabilities.get(side);
            return result == null ? LazyOptional.empty() : result.cast();
        }
        return LazyOptional.empty();
    }

    private TransferOutcome transferOut(PhysicalNetworkManager manager, TravelingItem item, long gameTime) {
        Direction direction = item.outgoing;
        BlockPos targetPosition = position().relative(direction);
        PhysicalNetworkNode targetNode = manager.node(NetworkDomain.LOGISTICS, targetPosition).orElse(null);
        if (targetNode instanceof LogisticsTubeModule tube) {
            boolean accepted = tube.enqueue(item.stack, direction.getOpposite(), gameTime);
            return accepted ? TransferOutcome.REMOVE : TransferOutcome.BLOCKED;
        }

        IItemHandler handler = adjacentHandler(manager.level(), direction);
        if (handler == null || !ItemHandlerTransactions.acceptsAll(handler, item.stack)) {
            return TransferOutcome.BLOCKED;
        }
        ItemStack remainder = ItemHandlerTransactions.insert(handler, item.stack, false);
        return remainder.isEmpty() ? TransferOutcome.REMOVE : new TransferOutcome(false, remainder);
    }

    private boolean enqueue(ItemStack stack, Direction incoming, long gameTime) {
        if (stack.isEmpty() || items.size() >= MAX_PAYLOADS || !isSideEnabled(incoming)) {
            return false;
        }
        TravelingItem item = new TravelingItem(stack.copy(), 0, incoming, null);
        item.lastMovedTick = gameTime;
        items.add(item);
        markStateChangedAndSync();
        return true;
    }

    @Nullable
    private IItemHandler adjacentHandler(ServerLevel level, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(position().relative(direction));
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).orElse(null);
    }

    public record TravelingItemView(
            ItemStack stack,
            int progress,
            Direction incoming,
            @Nullable Direction outgoing
    ) {
    }

    private static final class TravelingItem {
        private ItemStack stack;
        private int progress;
        private Direction incoming;
        @Nullable
        private Direction outgoing;
        private long lastMovedTick = Long.MIN_VALUE;

        private TravelingItem(ItemStack stack, int progress, Direction incoming, @Nullable Direction outgoing) {
            this.stack = stack;
            this.progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
            this.incoming = incoming;
            this.outgoing = outgoing;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("item", stack.save(new CompoundTag()));
            tag.putInt("progress", progress);
            tag.putInt("incoming", incoming.ordinal());
            tag.putInt("outgoing", outgoing == null ? -1 : outgoing.ordinal());
            return tag;
        }

        private static TravelingItem load(CompoundTag tag) {
            Direction incoming = direction(tag.getInt("incoming"), Direction.UP);
            int outgoingOrdinal = tag.getInt("outgoing");
            Direction outgoing = outgoingOrdinal < 0 ? null : direction(outgoingOrdinal, null);
            return new TravelingItem(
                    ItemStack.of(tag.getCompound("item")),
                    tag.getInt("progress"),
                    incoming,
                    outgoing
            );
        }

        @Nullable
        private static Direction direction(int ordinal, @Nullable Direction fallback) {
            Direction[] directions = Direction.values();
            return ordinal >= 0 && ordinal < directions.length ? directions[ordinal] : fallback;
        }
    }

    private record TransferOutcome(boolean remove, @Nullable ItemStack remaining) {
        private static final TransferOutcome REMOVE = new TransferOutcome(true, null);
        private static final TransferOutcome BLOCKED = new TransferOutcome(false, null);
    }

    private final class TubeItemHandler implements IItemHandler {
        private final Direction side;

        private TubeItemHandler(Direction side) {
            this.side = side;
        }

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
                    || !automationEnabled()
                    || !isSideEnabled(side)
                    || items.size() >= MAX_PAYLOADS) {
                return stack;
            }
            int acceptedCount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getSlotLimit(slot)));
            ItemStack accepted = stack.copyWithCount(acceptedCount);
            if (!simulate) {
                long gameTime = host().level() == null ? Long.MIN_VALUE : host().level().getGameTime();
                enqueue(accepted, side, gameTime);
            }
            if (acceptedCount >= stack.getCount()) {
                return ItemStack.EMPTY;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(acceptedCount);
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
            return slot == 0 && !stack.isEmpty();
        }
    }
}
