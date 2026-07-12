package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Server-authoritative two-lane conveyor inventory and movement simulation.
 */
public final class ConveyorBeltModule implements MachineModule {
    public static final int MAX_PROGRESS = 16;
    public static final int MIN_SPACING = 4;
    public static final int MAX_PARCELS = 16;

    private static final String PARCELS_TAG = "parcels";
    private static final String NEXT_LANE_TAG = "next_lane";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final Supplier<Direction> facing;
    private final List<Parcel> parcels = new ArrayList<>();
    private LazyOptional<IItemHandler> capability = LazyOptional.empty();
    private Lane nextLane = Lane.LEFT;
    private RedstoneControlMode redstoneMode = RedstoneControlMode.IGNORED;

    public ConveyorBeltModule(ResourceLocation id, MachineModuleHost host, Supplier<Direction> facing) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.facing = Objects.requireNonNull(facing);
        reviveCapabilities();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    public List<ParcelView> parcels() {
        return parcels.stream()
                .map(parcel -> new ParcelView(parcel.stack.copy(), parcel.lane, parcel.progress))
                .toList();
    }

    public RedstoneControlMode redstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        host.markChangedAndSync();
    }

    public boolean insert(ItemStack stack, boolean simulate) {
        Lane lane = nextLane;
        if (!canEnter(lane) && canEnter(lane.other())) {
            lane = lane.other();
        }
        if (!canEnter(lane) || stack.isEmpty() || parcels.size() >= MAX_PARCELS) {
            return false;
        }
        if (!simulate) {
            long tick = host.level() == null ? Long.MIN_VALUE : host.level().getGameTime();
            parcels.add(new Parcel(stack.copy(), lane, 0, tick));
            nextLane = lane.other();
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
        boolean changed = false;
        boolean structural = false;

        for (Lane lane : Lane.values()) {
            List<Parcel> laneParcels = parcels.stream()
                    .filter(parcel -> parcel.lane == lane)
                    .sorted(Comparator.comparingInt((Parcel parcel) -> parcel.progress).reversed())
                    .toList();
            int nextProgress = Integer.MAX_VALUE;
            for (Parcel parcel : laneParcels) {
                if (parcel.lastMovedTick == gameTime) {
                    nextProgress = parcel.progress;
                    continue;
                }
                parcel.lastMovedTick = gameTime;
                if (parcel.progress >= MAX_PROGRESS) {
                    if (transfer(level, parcel, gameTime)) {
                        parcels.remove(parcel);
                        structural = true;
                        continue;
                    }
                } else if (nextProgress - parcel.progress > MIN_SPACING) {
                    parcel.progress++;
                    changed = true;
                }
                nextProgress = parcel.progress;
            }
        }

        if (structural || (changed && gameTime % 4L == 0L)) {
            host.markChangedAndSync();
        } else if (changed) {
            host.markChanged();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        parcels.clear();
        ListTag stored = tag.getList(PARCELS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < stored.size() && parcels.size() < MAX_PARCELS; index++) {
            Parcel parcel = Parcel.load(stored.getCompound(index));
            if (!parcel.stack.isEmpty()) {
                parcels.add(parcel);
            }
        }
        nextLane = Lane.byOrdinal(tag.getInt(NEXT_LANE_TAG));
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        int mode = tag.getInt(REDSTONE_MODE_TAG);
        redstoneMode = mode >= 0 && mode < modes.length ? modes[mode] : RedstoneControlMode.IGNORED;
    }

    @Override
    public void save(CompoundTag tag) {
        ListTag stored = new ListTag();
        parcels.forEach(parcel -> stored.add(parcel.save()));
        tag.put(PARCELS_TAG, stored);
        tag.putInt(NEXT_LANE_TAG, nextLane.ordinal());
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        save(tag);
    }

    @Override
    public void invalidateCapabilities() {
        capability.invalidate();
        capability = LazyOptional.empty();
    }

    @Override
    public void reviveCapabilities() {
        capability = LazyOptional.of(BeltItemHandler::new);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> requested, @Nullable Direction side) {
        if (requested == ForgeCapabilities.ITEM_HANDLER && (side == null || side == Direction.UP)) {
            return capability.cast();
        }
        return LazyOptional.empty();
    }

    private boolean transfer(ServerLevel level, Parcel parcel, long gameTime) {
        BlockPos targetPosition = host.position().relative(facing.get());
        BlockEntity blockEntity = level.getBlockEntity(targetPosition);
        if (blockEntity instanceof ConveyorBeltBlockEntity belt
                && belt.facing() != facing.get().getOpposite()) {
            return belt.belt().acceptFromBelt(parcel.stack, parcel.lane, gameTime);
        }
        if (blockEntity == null) {
            return false;
        }
        IItemHandler handler = blockEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, facing.get().getOpposite())
                .orElse(null);
        if (handler == null || !ItemHandlerTransactions.acceptsAll(handler, parcel.stack)) {
            return false;
        }
        return ItemHandlerTransactions.insert(handler, parcel.stack, false).isEmpty();
    }

    private boolean acceptFromBelt(ItemStack stack, Lane lane, long gameTime) {
        if (!canEnter(lane) || stack.isEmpty() || parcels.size() >= MAX_PARCELS) {
            return false;
        }
        parcels.add(new Parcel(stack.copy(), lane, 0, gameTime));
        host.markChangedAndSync();
        return true;
    }

    private boolean canEnter(Lane lane) {
        return parcels.stream().noneMatch(parcel -> parcel.lane == lane && parcel.progress < MIN_SPACING);
    }

    private boolean automationEnabled() {
        return host.level() == null || redstoneMode.allows(host.level().hasNeighborSignal(host.position()));
    }

    public enum Lane {
        LEFT,
        RIGHT;

        public Lane other() {
            return this == LEFT ? RIGHT : LEFT;
        }

        private static Lane byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : LEFT;
        }
    }

    public record ParcelView(ItemStack stack, Lane lane, int progress) {
    }

    private static final class Parcel {
        private final ItemStack stack;
        private final Lane lane;
        private int progress;
        private long lastMovedTick;

        private Parcel(ItemStack stack, Lane lane, int progress, long lastMovedTick) {
            this.stack = stack;
            this.lane = lane;
            this.progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
            this.lastMovedTick = lastMovedTick;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("item", stack.save(new CompoundTag()));
            tag.putInt("lane", lane.ordinal());
            tag.putInt("progress", progress);
            return tag;
        }

        private static Parcel load(CompoundTag tag) {
            return new Parcel(
                    ItemStack.of(tag.getCompound("item")),
                    Lane.byOrdinal(tag.getInt("lane")),
                    tag.getInt("progress"),
                    Long.MIN_VALUE
            );
        }
    }

    private final class BeltItemHandler implements IItemHandler {
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
