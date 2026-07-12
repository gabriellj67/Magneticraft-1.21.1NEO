package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Persisted and lifecycle-safe directional fluid capability module.
 */
public final class FluidTankModule implements MachineModule {
    private static final String CLIENT_SNAPSHOT_VERSION_TAG = "client_snapshot_version";
    private static final int CLIENT_SNAPSHOT_VERSION = 1;

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final Function<Direction, TankAccess> accessBySide;
    private final FluidTank tank;
    private final Map<Direction, LazyOptional<IFluidHandler>> sidedCapabilities = new EnumMap<>(Direction.class);
    private LazyOptional<IFluidHandler> unsidedCapability = LazyOptional.empty();
    private CompoundTag lastClientSnapshot;
    private boolean clientSnapshotDirty = true;

    public FluidTankModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Predicate<Direction> exposedSides
    ) {
        this(
                id,
                host,
                capacity,
                validator,
                (Function<Direction, TankAccess>) side -> exposedSides.test(side)
                        ? TankAccess.BOTH
                        : TankAccess.NONE
        );
    }

    private FluidTankModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Function<Direction, TankAccess> accessBySide
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.accessBySide = Objects.requireNonNull(accessBySide);
        this.tank = new FluidTank(capacity, Objects.requireNonNull(validator)) {
            @Override
            protected void onContentsChanged() {
                clientSnapshotDirty = true;
                host.markChanged();
            }
        };
        reviveCapabilities();
    }

    public static FluidTankModule directional(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Function<Direction, TankAccess> accessBySide
    ) {
        return new FluidTankModule(id, host, capacity, validator, accessBySide);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag) {
        tank.readFromNBT(tag);
    }

    @Override
    public void save(CompoundTag tag) {
        tank.writeToNBT(tag);
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        tank.readFromNBT(tag);
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        // Keep an empty tank snapshot present in the owning BE update tag so a
        // client can clear fluid that was rendered by an earlier snapshot.
        tag.putInt(CLIENT_SNAPSHOT_VERSION_TAG, CLIENT_SNAPSHOT_VERSION);
        tank.writeToNBT(tag);
    }

    @Override
    public void serverTick() {
        if (!clientSnapshotDirty
                || !(host.level() instanceof ServerLevel level)
                || level.getGameTime() % 4L != 0L) {
            return;
        }
        CompoundTag snapshot = tank.writeToNBT(new CompoundTag());
        clientSnapshotDirty = false;
        if (!snapshot.equals(lastClientSnapshot)) {
            lastClientSnapshot = snapshot.copy();
            host.requestClientSync();
        }
    }

    @Override
    public void invalidateCapabilities() {
        unsidedCapability.invalidate();
        unsidedCapability = LazyOptional.empty();
        sidedCapabilities.values().forEach(LazyOptional::invalidate);
        sidedCapabilities.clear();
    }

    @Override
    public void reviveCapabilities() {
        unsidedCapability = viewFor(null);
        for (Direction direction : Direction.values()) {
            sidedCapabilities.put(direction, viewFor(direction));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> requested, @Nullable Direction side) {
        if (requested != ForgeCapabilities.FLUID_HANDLER) {
            return LazyOptional.empty();
        }
        LazyOptional<IFluidHandler> result = side == null ? unsidedCapability : sidedCapabilities.get(side);
        return result == null ? LazyOptional.empty() : result.cast();
    }

    public FluidTank tank() {
        return tank;
    }

    private LazyOptional<IFluidHandler> viewFor(@Nullable Direction side) {
        TankAccess access = Objects.requireNonNullElse(accessBySide.apply(side), TankAccess.NONE);
        return access == TankAccess.NONE
                ? LazyOptional.empty()
                : LazyOptional.of(() -> new RestrictedFluidHandler(tank, access));
    }

    public enum TankAccess {
        NONE(false, false),
        INPUT(true, false),
        OUTPUT(false, true),
        BOTH(true, true);

        private final boolean input;
        private final boolean output;

        TankAccess(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }
    }

    private record RestrictedFluidHandler(FluidTank delegate, TankAccess access) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? delegate.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? delegate.getTankCapacity(0) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && access.input && delegate.isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return access.input ? delegate.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return access.output ? delegate.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return access.output ? delegate.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
