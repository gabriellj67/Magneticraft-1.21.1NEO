package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Server-authoritative horizontal conveyor inventory, routing and collision
 * simulation. Sloped and vertical paths are intentionally excluded.
 */
public final class ConveyorBeltModule implements MachineModule {
    public static final int MAX_PROGRESS = 16;
    public static final int MIN_SPACING = 4;
    public static final int MAX_PARCELS = 16;

    private static final int MANUAL_INSERT_PROGRESS = 2;
    private static final int OPEN_END_LIMIT = 13;
    private static final int CONNECTED_BELT_LIMIT = 15;

    private static final String PARCELS_TAG = "parcels";
    private static final String NEXT_LANE_TAG = "next_lane";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String ITEM_TAG = "item";
    private static final String ROUTE_TAG = "route";
    private static final String LEGACY_LANE_TAG = "lane";
    private static final String PROGRESS_TAG = "progress";
    private static final String LOCKED_TAG = "locked";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final Supplier<Direction> facing;
    private final List<Parcel> parcels = new ArrayList<>();
    private final IItemHandler itemHandler = new BeltItemHandler();
    private Lane nextLane = Lane.LEFT;
    private RedstoneControlMode redstoneMode = RedstoneControlMode.IGNORED;
    private long clientSnapshotTick = Long.MIN_VALUE;

    public ConveyorBeltModule(ResourceLocation id, MachineModuleHost host, Supplier<Direction> facing) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.facing = Objects.requireNonNull(facing);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    public List<ParcelView> parcels() {
        return parcels.stream()
                .map(parcel -> new ParcelView(parcel.stack.copy(), parcel.route, parcel.progress, parcel.locked))
                .toList();
    }

    public RedstoneControlMode redstoneMode() {
        return redstoneMode;
    }

    public long clientSnapshotTick() {
        return clientSnapshotTick;
    }

    public boolean clientMovementEnabled() {
        return automationEnabled();
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        host.markChangedAndSync();
    }

    public boolean insert(ItemStack stack, boolean simulate) {
        ConveyorRoute route = nextLane.route();
        if (!canEnter(route, MANUAL_INSERT_PROGRESS)
                && canEnter(nextLane.other().route(), MANUAL_INSERT_PROGRESS)) {
            route = nextLane.other().route();
        }
        if (stack.isEmpty()
                || parcels.size() >= MAX_PARCELS
                || !canEnter(route, MANUAL_INSERT_PROGRESS)) {
            return false;
        }
        if (!simulate) {
            long tick = host.level() == null ? Long.MIN_VALUE : host.level().getGameTime();
            parcels.add(new Parcel(stack.copy(), route, MANUAL_INSERT_PROGRESS, tick, false));
            nextLane = route.leftSide() ? Lane.RIGHT : Lane.LEFT;
            host.markChangedAndSync();
        }
        return true;
    }

    public ItemStack removeLast() {
        Parcel parcel = parcels.stream().max(Comparator.comparingInt(value -> value.progress)).orElse(null);
        if (parcel == null) {
            return ItemStack.EMPTY;
        }
        parcels.remove(parcel);
        host.markChangedAndSync();
        return parcel.stack;
    }

    public List<ItemStack> removeAll() {
        List<ItemStack> removed = parcels.stream().map(parcel -> parcel.stack.copy()).toList();
        parcels.clear();
        host.markChangedAndSync();
        return removed;
    }

    @Override
    public void serverTick() {
        if (!(host.level() instanceof ServerLevel level)
                || !redstoneMode.allows(level.hasNeighborSignal(host.position()))
                || parcels.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        OutputTarget output = outputTarget(level);
        int movementLimit = output.kind == OutputKind.BELT ? CONNECTED_BELT_LIMIT : OPEN_END_LIMIT;
        ConveyorOccupancy occupancy = occupancy();
        boolean changed = false;
        boolean visualChanged = false;
        boolean structural = false;

        Iterator<Parcel> iterator = parcels.iterator();
        while (iterator.hasNext()) {
            Parcel parcel = iterator.next();
            if (parcel.lastMovedTick == gameTime) {
                continue;
            }
            parcel.lastMovedTick = gameTime;

            if (parcel.progress > movementLimit) {
                TransferOutcome outcome = transfer(output, parcel, gameTime);
                if (outcome.remove()) {
                    occupancy.unmark(parcel.route, parcel.progress);
                    iterator.remove();
                    structural = true;
                    continue;
                }
                if (outcome.remaining() != null) {
                    parcel.stack = outcome.remaining();
                    structural = true;
                }
                visualChanged |= parcel.setLocked(true);
                continue;
            }

            int nextProgress = Math.min(
                    MAX_PROGRESS,
                    parcel.progress + parcel.route.speedPixelsPerTick()
            );
            occupancy.unmark(parcel.route, parcel.progress);
            if (occupancy.isFree(parcel.route, nextProgress)) {
                parcel.progress = nextProgress;
                occupancy.mark(parcel.route, parcel.progress);
                visualChanged |= parcel.setLocked(false);
                changed = true;
            } else {
                occupancy.mark(parcel.route, parcel.progress);
                visualChanged |= parcel.setLocked(true);
            }
        }

        if (structural || visualChanged || (changed && gameTime % 4L == 0L)) {
            host.markChangedAndSync();
        } else if (changed) {
            host.markChanged();
        }
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        parcels.clear();
        ListTag stored = tag.getList(PARCELS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size() && parcels.size() < MAX_PARCELS; index++) {
            Parcel parcel = Parcel.load(stored.getCompound(index), registries);
            if (!parcel.stack.isEmpty() && canEnter(parcel.route, parcel.progress)) {
                parcels.add(parcel);
            }
        }
        nextLane = Lane.byOrdinal(tag.getInt(NEXT_LANE_TAG));
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        int mode = tag.getInt(REDSTONE_MODE_TAG);
        redstoneMode = mode >= 0 && mode < modes.length ? modes[mode] : RedstoneControlMode.IGNORED;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag stored = new ListTag();
        parcels.forEach(parcel -> stored.add(parcel.save(registries)));
        tag.put(PARCELS_TAG, stored);
        tag.putInt(NEXT_LANE_TAG, nextLane.ordinal());
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
    }

    @Override
    public void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
        clientSnapshotTick = host.level() == null ? Long.MIN_VALUE : host.level().getGameTime();
    }

    @Override
    public void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
        save(tag, registries);
    }

    /** Item-handler view exposed for the given side, or {@code null} if this module hides it from that side. */
    @Nullable
    public IItemHandler view(@Nullable Direction side) {
        return side == null || side == Direction.UP ? itemHandler : null;
    }

    private TransferOutcome transfer(OutputTarget output, Parcel parcel, long gameTime) {
        if (output.kind == OutputKind.BELT && output.belt != null) {
            return output.belt.belt().acceptFromBelt(
                    parcel.stack,
                    facing.get(),
                    parcel.route,
                    gameTime
            ) ? TransferOutcome.REMOVE : TransferOutcome.BLOCKED;
        }
        if (output.kind != OutputKind.INVENTORY || output.handler == null) {
            return TransferOutcome.BLOCKED;
        }
        ItemStack remainder = ItemHandlerTransactions.insertAfterFullSimulation(output.handler, parcel.stack);
        if (remainder.getCount() >= parcel.stack.getCount()) {
            return TransferOutcome.BLOCKED;
        }
        return remainder.isEmpty() ? TransferOutcome.REMOVE : new TransferOutcome(false, remainder);
    }

    private boolean acceptFromBelt(
            ItemStack stack,
            Direction sourceFacing,
            ConveyorRoute previousRoute,
            long gameTime
    ) {
        if (!(host.level() instanceof ServerLevel level) || stack.isEmpty() || parcels.size() >= MAX_PARCELS) {
            return false;
        }
        Optional<ConveyorRoute> route = ConveyorRoute.resolveIncoming(
                incomingDirection(sourceFacing),
                previousRoute,
                isCorner(level)
        );
        if (route.isEmpty() || !canEnter(route.get(), 0)) {
            return false;
        }
        parcels.add(new Parcel(stack.copy(), route.get(), 0, gameTime, false));
        host.markChangedAndSync();
        return true;
    }

    private ConveyorRoute.IncomingDirection incomingDirection(Direction sourceFacing) {
        Direction receiverFacing = facing.get();
        if (sourceFacing == receiverFacing) {
            return ConveyorRoute.IncomingDirection.SAME;
        }
        if (sourceFacing == receiverFacing.getOpposite()) {
            return ConveyorRoute.IncomingDirection.OPPOSITE;
        }
        return sourceFacing == receiverFacing.getClockWise()
                ? ConveyorRoute.IncomingDirection.CLOCKWISE
                : ConveyorRoute.IncomingDirection.COUNTER_CLOCKWISE;
    }

    private boolean isCorner(ServerLevel level) {
        Direction direction = facing.get();
        ConveyorBeltBlockEntity back = loadedBelt(level, host.position().relative(direction.getOpposite()));
        ConveyorBeltBlockEntity front = loadedBelt(level, host.position().relative(direction));
        ConveyorBeltBlockEntity left = loadedBelt(level, host.position().relative(direction.getCounterClockWise()));
        ConveyorBeltBlockEntity right = loadedBelt(level, host.position().relative(direction.getClockWise()));
        boolean hasBack = back != null && back.facing() == direction;
        boolean hasLeft = left != null && left.facing() == direction.getClockWise();
        boolean hasRight = right != null && right.facing() == direction.getCounterClockWise();
        return !hasBack && front != null && (hasLeft ^ hasRight);
    }

    private OutputTarget outputTarget(ServerLevel level) {
        Direction direction = facing.get();
        BlockPos frontPosition = host.position().relative(direction);
        var frontChunk = level.getChunkSource().getChunkNow(
                frontPosition.getX() >> 4,
                frontPosition.getZ() >> 4
        );
        if (frontChunk == null) {
            return OutputTarget.BLOCKED;
        }

        BlockEntity frontEntity = frontChunk.getBlockEntity(frontPosition);
        if (frontEntity instanceof ConveyorBeltBlockEntity belt) {
            return !belt.isRemoved() && belt.facing() != direction.getOpposite()
                    ? OutputTarget.belt(belt)
                    : OutputTarget.BLOCKED;
        }
        if (frontEntity != null && !frontEntity.isRemoved()) {
            return inventoryTarget(frontEntity, direction.getOpposite());
        }
        if (!frontChunk.getBlockState(frontPosition).isAir()) {
            return OutputTarget.BLOCKED;
        }

        BlockPos belowFront = frontPosition.below();
        BlockEntity belowEntity = frontChunk.getBlockEntity(belowFront);
        if (belowEntity == null || belowEntity.isRemoved() || belowEntity instanceof ConveyorBeltBlockEntity) {
            return OutputTarget.BLOCKED;
        }
        return inventoryTarget(belowEntity, direction.getOpposite());
    }

    private static OutputTarget inventoryTarget(BlockEntity blockEntity, Direction side) {
        Level level = blockEntity.getLevel();
        IItemHandler handler = level == null
                ? null
                : Capabilities.ItemHandler.BLOCK.getCapability(
                        level,
                        blockEntity.getBlockPos(),
                        blockEntity.getBlockState(),
                        blockEntity,
                        side
                );
        return handler == null ? OutputTarget.BLOCKED : OutputTarget.inventory(handler);
    }

    @Nullable
    private static ConveyorBeltBlockEntity loadedBelt(ServerLevel level, BlockPos position) {
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) {
            return null;
        }
        BlockEntity blockEntity = chunk.getBlockEntity(position);
        return blockEntity instanceof ConveyorBeltBlockEntity belt && !belt.isRemoved() ? belt : null;
    }

    private boolean canEnter(ConveyorRoute route, int progress) {
        return occupancy().isFree(route, progress);
    }

    private ConveyorOccupancy occupancy() {
        ConveyorOccupancy occupancy = new ConveyorOccupancy();
        parcels.forEach(parcel -> occupancy.mark(parcel.route, parcel.progress));
        return occupancy;
    }

    private boolean automationEnabled() {
        return host.level() == null || redstoneMode.allows(host.level().hasNeighborSignal(host.position()));
    }

    private enum Lane {
        LEFT,
        RIGHT;

        private Lane other() {
            return this == LEFT ? RIGHT : LEFT;
        }

        private ConveyorRoute route() {
            return this == LEFT ? ConveyorRoute.LEFT_FORWARD : ConveyorRoute.RIGHT_FORWARD;
        }

        private static Lane byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : LEFT;
        }
    }

    public record ParcelView(ItemStack stack, ConveyorRoute route, int progress, boolean locked) {
    }

    private static final class Parcel {
        private ItemStack stack;
        private final ConveyorRoute route;
        private int progress;
        private long lastMovedTick;
        private boolean locked;

        private Parcel(
                ItemStack stack,
                ConveyorRoute route,
                int progress,
                long lastMovedTick,
                boolean locked
        ) {
            this.stack = stack;
            this.route = route;
            this.progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
            this.lastMovedTick = lastMovedTick;
            this.locked = locked;
        }

        private boolean setLocked(boolean value) {
            if (locked == value) {
                return false;
            }
            locked = value;
            return true;
        }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.put(ITEM_TAG, stack.saveOptional(registries));
            tag.putInt(ROUTE_TAG, route.ordinal());
            tag.putInt(PROGRESS_TAG, progress);
            tag.putBoolean(LOCKED_TAG, locked);
            return tag;
        }

        private static Parcel load(CompoundTag tag, HolderLookup.Provider registries) {
            ConveyorRoute fallback = tag.getInt(LEGACY_LANE_TAG) == Lane.RIGHT.ordinal()
                    ? ConveyorRoute.RIGHT_FORWARD
                    : ConveyorRoute.LEFT_FORWARD;
            ConveyorRoute route = tag.contains(ROUTE_TAG, Tag.TAG_INT)
                    ? ConveyorRoute.byOrdinalOrDefault(tag.getInt(ROUTE_TAG), fallback)
                    : fallback;
            return new Parcel(
                    ItemStack.parseOptional(registries, tag.getCompound(ITEM_TAG)),
                    route,
                    tag.getInt(PROGRESS_TAG),
                    Long.MIN_VALUE,
                    tag.getBoolean(LOCKED_TAG)
            );
        }
    }

    private enum OutputKind {
        BLOCKED,
        BELT,
        INVENTORY
    }

    private record OutputTarget(
            OutputKind kind,
            @Nullable ConveyorBeltBlockEntity belt,
            @Nullable IItemHandler handler
    ) {
        private static final OutputTarget BLOCKED = new OutputTarget(OutputKind.BLOCKED, null, null);

        private static OutputTarget belt(ConveyorBeltBlockEntity belt) {
            return new OutputTarget(OutputKind.BELT, belt, null);
        }

        private static OutputTarget inventory(IItemHandler handler) {
            return new OutputTarget(OutputKind.INVENTORY, null, handler);
        }
    }

    private record TransferOutcome(boolean remove, @Nullable ItemStack remaining) {
        private static final TransferOutcome REMOVE = new TransferOutcome(true, null);
        private static final TransferOutcome BLOCKED = new TransferOutcome(false, null);
    }

    private final class BeltItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return parcels.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return validParcelSlot(slot) ? parcels.get(slot - 1).stack.copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !automationEnabled()) {
                return stack;
            }
            int acceptedCount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getSlotLimit(slot)));
            ItemStack accepted = stack.copyWithCount(acceptedCount);
            if (!insert(accepted, simulate)) {
                return stack;
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
            if (!validParcelSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            Parcel parcel = parcels.get(slot - 1);
            int extractedCount = Math.min(amount, parcel.stack.getCount());
            ItemStack extracted = parcel.stack.copyWithCount(extractedCount);
            if (!simulate) {
                if (extractedCount >= parcel.stack.getCount()) {
                    parcels.remove(slot - 1);
                } else {
                    parcel.stack.shrink(extractedCount);
                }
                host.markChangedAndSync();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty();
        }

        private boolean validParcelSlot(int slot) {
            return slot > 0 && slot <= parcels.size();
        }
    }
}
