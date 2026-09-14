package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsRouteDecision;
import committee.nova.mods.magneticraft.system.network.pressure.PressureLogisticsMath;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bounded, persisted item boxes moving through a weighted pneumatic graph.
 */
public final class LogisticsTubeModule extends AbstractPhysicalNetworkModule implements LogisticsNetworkNode {
    public static final int MAX_PROGRESS = 128;
    public static final int CENTER_PROGRESS = MAX_PROGRESS / 2;
    public static final int MAX_PAYLOADS = 64;
    public static final int MAX_ROUTE_VISITS = PhysicalNetworkManager.MAX_LOGISTICS_ROUTE_VISITS;

    private static final String ITEMS_TAG = "items";
    private static final String ROUTE_CURSOR_TAG = "route_cursor";

    private final int routingWeight;
    private final List<TravelingItem> items = new ArrayList<>();
    private final IItemHandler[] sideHandlers = new IItemHandler[Direction.values().length];
    private int routeCursor;
    private long clientSnapshotTick = Long.MIN_VALUE;

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
        for (Direction direction : Direction.values()) {
            sideHandlers[direction.ordinal()] = new TubeItemHandler(direction);
        }
    }

    @Override
    public int routingWeight() {
        return routingWeight;
    }

    public List<TravelingItemView> itemsSnapshot() {
        double movement = currentProgressPerTick();
        return items.stream()
                .map(item -> new TravelingItemView(
                        item.stack.copy(),
                        item.progress,
                        item.incoming,
                        item.outgoing,
                        movement
                ))
                .toList();
    }

    public double currentProgressPerTick() {
        return PressureLogisticsMath.progressPerTick(pressureNode().pressureKpa());
    }

    public long clientSnapshotTick() {
        return clientSnapshotTick;
    }

    public boolean clientMovementEnabled() {
        return automationEnabled();
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
        double progressPerTick = currentProgressPerTick();

        for (TravelingItem item : List.copyOf(items)) {
            if (item.lastMovedTick == gameTime) {
                continue;
            }
            item.lastMovedTick = gameTime;

            if (item.progress < CENTER_PROGRESS) {
                item.progress = Math.min(CENTER_PROGRESS, item.progress + progressPerTick);
                changed = true;
                if (item.progress >= CENTER_PROGRESS) {
                    structuralChange |= chooseRoute(manager, item);
                }
                // Reaching the center and entering the outgoing half are distinct tick phases.
                continue;
            }
            if (item.progress >= CENTER_PROGRESS && item.outgoing == null) {
                structuralChange |= chooseRoute(manager, item);
            }
            if (item.outgoing != null && item.progress < MAX_PROGRESS) {
                item.progress = Math.min(MAX_PROGRESS, item.progress + progressPerTick);
                changed = true;
            }
            if (item.progress >= MAX_PROGRESS && item.outgoing != null) {
                if (!item.segmentGasCharged) {
                    consumeSegmentGas(item.stack.getCount());
                    item.segmentGasCharged = true;
                    changed = true;
                }
                TransferOutcome outcome = transferOut(manager, item, gameTime);
                if (outcome.status() == TransferStatus.REMOVED) {
                    removed.add(item);
                    structuralChange = true;
                } else if (outcome.status() == TransferStatus.PARTIAL) {
                    item.stack = outcome.remaining();
                    item.progress = MAX_PROGRESS;
                    structuralChange = true;
                } else if (outcome.status() == TransferStatus.INVALID_ROUTE) {
                    item.outgoing = null;
                    item.progress = CENTER_PROGRESS;
                    item.segmentGasCharged = false;
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

    private boolean chooseRoute(PhysicalNetworkManager manager, TravelingItem item) {
        Optional<LogisticsRouteDecision> route = manager.findLogisticsRoute(
                position(),
                item.incoming,
                item.stack,
                routeCursor,
                MAX_ROUTE_VISITS
        );
        if (route.isEmpty()) {
            return false;
        }
        item.outgoing = route.get().direction();
        routeCursor++;
        return true;
    }

    @Override
    protected void loadNetworkData(CompoundTag tag, HolderLookup.Provider registries) {
        items.clear();
        ListTag stored = tag.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size() && items.size() < MAX_PAYLOADS; index++) {
            TravelingItem item = TravelingItem.load(stored.getCompound(index), registries);
            if (!item.stack.isEmpty()) {
                items.add(item);
            }
        }
        routeCursor = Math.max(0, tag.getInt(ROUTE_CURSOR_TAG));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag stored = new ListTag();
        items.forEach(item -> stored.add(item.save(registries)));
        tag.put(ITEMS_TAG, stored);
        tag.putInt(ROUTE_CURSOR_TAG, routeCursor);
    }

    @Override
    public void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
        loadNetworkData(tag, registries);
        clientSnapshotTick = host().level() == null ? Long.MIN_VALUE : host().level().getGameTime();
    }

    @Override
    public void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
        saveNetworkData(tag, registries);
    }

    /** Item-handler view exposed for the given side, or {@code null} if this module hides it from that side. */
    @Nullable
    public IItemHandler view(Direction side) {
        return isSideEnabled(side) ? sideHandlers[side.ordinal()] : null;
    }

    private TransferOutcome transferOut(PhysicalNetworkManager manager, TravelingItem item, long gameTime) {
        Direction direction = item.outgoing;
        BlockPos targetPosition = position().relative(direction);
        PhysicalNetworkNode targetNode = manager.node(NetworkDomain.LOGISTICS, targetPosition).orElse(null);
        if (targetNode instanceof LogisticsTubeModule tube) {
            if (!manager.neighbors(NetworkDomain.LOGISTICS, position()).contains(targetPosition.asLong())) {
                return TransferOutcome.INVALID_ROUTE;
            }
            boolean accepted = tube.enqueue(item.stack, direction.getOpposite(), gameTime);
            return accepted ? TransferOutcome.REMOVE : TransferOutcome.BLOCKED;
        }

        var targetChunk = manager.level().getChunkSource()
                .getChunkNow(targetPosition.getX() >> 4, targetPosition.getZ() >> 4);
        if (targetChunk == null) {
            return TransferOutcome.UNLOADED;
        }
        BlockEntity targetEntity = targetChunk.getBlockEntity(targetPosition);
        if (targetEntity instanceof PneumaticTubeBlockEntity) {
            return TransferOutcome.INVALID_ROUTE;
        }
        IItemHandler handler = targetEntity == null
                ? null
                : Capabilities.ItemHandler.BLOCK.getCapability(
                        manager.level(),
                        targetPosition,
                        targetEntity.getBlockState(),
                        targetEntity,
                        direction.getOpposite()
                );
        if (handler == null) {
            return TransferOutcome.INVALID_ROUTE;
        }
        ItemStack remainder = ItemHandlerTransactions.insertAfterFullSimulation(handler, item.stack);
        if (remainder.getCount() >= item.stack.getCount()) {
            return TransferOutcome.BLOCKED;
        }
        return remainder.isEmpty()
                ? TransferOutcome.REMOVE
                : new TransferOutcome(TransferStatus.PARTIAL, remainder);
    }

    private PressureNode pressureNode() {
        if (host() instanceof PneumaticTubeBlockEntity tube) {
            return tube.pressure().node();
        }
        throw new IllegalStateException("Logistics tube requires a pneumatic pressure host");
    }

    private void consumeSegmentGas(int itemCount) {
        PressureNode node = pressureNode();
        node.gasId().ifPresent(gas -> node.extract(
                gas,
                PressureLogisticsMath.segmentCostKpaLiters(itemCount),
                false
        ));
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
        BlockPos target = position().relative(direction);
        var targetChunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
        if (targetChunk == null) {
            return null;
        }
        BlockEntity blockEntity = targetChunk.getBlockEntity(target);
        if (blockEntity == null || blockEntity instanceof PneumaticTubeBlockEntity) {
            return null;
        }
        return Capabilities.ItemHandler.BLOCK.getCapability(
                level,
                target,
                blockEntity.getBlockState(),
                blockEntity,
                direction.getOpposite()
        );
    }

    public record TravelingItemView(
            ItemStack stack,
            double progress,
            Direction incoming,
            @Nullable Direction outgoing,
            double progressPerTick
    ) {
    }

    private static final class TravelingItem {
        private ItemStack stack;
        private double progress;
        private Direction incoming;
        @Nullable
        private Direction outgoing;
        private boolean segmentGasCharged;
        private long lastMovedTick = Long.MIN_VALUE;

        private TravelingItem(ItemStack stack, double progress, Direction incoming, @Nullable Direction outgoing) {
            this.stack = stack;
            this.progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
            this.incoming = incoming;
            this.outgoing = outgoing;
        }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.put("item", stack.saveOptional(registries));
            tag.putDouble("progress", progress);
            tag.putInt("incoming", incoming.ordinal());
            tag.putInt("outgoing", outgoing == null ? -1 : outgoing.ordinal());
            tag.putBoolean("segment_gas_charged", segmentGasCharged);
            return tag;
        }

        private static TravelingItem load(CompoundTag tag, HolderLookup.Provider registries) {
            Direction incoming = direction(tag.getInt("incoming"), Direction.UP);
            int outgoingOrdinal = tag.getInt("outgoing");
            Direction outgoing = outgoingOrdinal < 0 ? null : direction(outgoingOrdinal, null);
            TravelingItem item = new TravelingItem(
                    ItemStack.parseOptional(registries, tag.getCompound("item")),
                    tag.getDouble("progress"),
                    incoming,
                    outgoing
            );
            item.segmentGasCharged = tag.getBoolean("segment_gas_charged");
            return item;
        }

        @Nullable
        private static Direction direction(int ordinal, @Nullable Direction fallback) {
            Direction[] directions = Direction.values();
            return ordinal >= 0 && ordinal < directions.length ? directions[ordinal] : fallback;
        }
    }

    private enum TransferStatus {
        REMOVED,
        PARTIAL,
        BLOCKED,
        UNLOADED,
        INVALID_ROUTE
    }

    private record TransferOutcome(TransferStatus status, @Nullable ItemStack remaining) {
        private static final TransferOutcome REMOVE = new TransferOutcome(TransferStatus.REMOVED, null);
        private static final TransferOutcome BLOCKED = new TransferOutcome(TransferStatus.BLOCKED, null);
        private static final TransferOutcome UNLOADED = new TransferOutcome(TransferStatus.UNLOADED, null);
        private static final TransferOutcome INVALID_ROUTE = new TransferOutcome(TransferStatus.INVALID_ROUTE, null);
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
