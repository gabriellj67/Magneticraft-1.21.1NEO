package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
    @Nullable
    private final Supplier<FluidStack> infiniteSource;
    private final Map<Direction, IFluidHandler> sidedViews = new EnumMap<>(Direction.class);
    @Nullable
    private IFluidHandler unsidedView;
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
                        : TankAccess.NONE,
                null
        );
    }

    private FluidTankModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Function<Direction, TankAccess> accessBySide,
            @Nullable Supplier<FluidStack> infiniteSource
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.accessBySide = Objects.requireNonNull(accessBySide);
        this.infiniteSource = infiniteSource;
        this.tank = new FluidTank(capacity, Objects.requireNonNull(validator)) {
            @Override
            protected void onContentsChanged() {
                clientSnapshotDirty = true;
                host.markChanged();
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidTankModule.this.infiniteSource == null
                        ? super.drain(resource, action)
                        : infiniteDrain(resource, resource.getAmount());
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidTankModule.this.infiniteSource == null
                        ? super.drain(maxDrain, action)
                        : infiniteDrain(null, maxDrain);
            }
        };
        refillInfiniteSource();
        buildViews();
    }

    public static FluidTankModule directional(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Function<Direction, TankAccess> accessBySide
    ) {
        return new FluidTankModule(id, host, capacity, validator, accessBySide, null);
    }

    public static FluidTankModule infiniteSource(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Supplier<FluidStack> source,
            Function<Direction, TankAccess> accessBySide
    ) {
        Objects.requireNonNull(source);
        return new FluidTankModule(
                id,
                host,
                capacity,
                stack -> {
                    FluidStack configured = source.get();
                    return configured != null && !configured.isEmpty() && stack.isFluidEqual(configured);
                },
                accessBySide,
                source
        );
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (infiniteSource == null) {
            tank.readFromNBT(registries, tag);
        } else {
            refillInfiniteSource();
        }
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tank.writeToNBT(registries, tag);
    }

    @Override
    public void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
        if (infiniteSource == null) {
            tank.readFromNBT(registries, tag);
        } else {
            refillInfiniteSource();
        }
    }

    @Override
    public void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
        // Keep an empty tank snapshot present in the owning BE update tag so a
        // client can clear fluid that was rendered by an earlier snapshot.
        tag.putInt(CLIENT_SNAPSHOT_VERSION_TAG, CLIENT_SNAPSHOT_VERSION);
        tank.writeToNBT(registries, tag);
    }

    @Override
    public void serverTick() {
        if (!clientSnapshotDirty
                || !(host.level() instanceof ServerLevel level)
                || level.getGameTime() % 4L != 0L) {
            return;
        }
        CompoundTag snapshot = tank.writeToNBT(level.registryAccess(), new CompoundTag());
        clientSnapshotDirty = false;
        if (!snapshot.equals(lastClientSnapshot)) {
            lastClientSnapshot = snapshot.copy();
            host.requestClientSync();
        }
    }

    /** Fluid-handler view exposed for the given side, or {@code null} if this module hides input and output from it. */
    @Nullable
    public IFluidHandler view(@Nullable Direction side) {
        return side == null ? unsidedView : sidedViews.get(side);
    }

    public FluidTank tank() {
        return tank;
    }

    /** Creates a non-owning view used by an exact multiblock structure port. */
    public IFluidHandler portHandler(TankAccess access) {
        if (access == null || access == TankAccess.NONE) {
            throw new IllegalArgumentException("A multiblock fluid port must expose input or output");
        }
        return new RestrictedFluidHandler(tank, access);
    }

    private FluidStack infiniteDrain(@Nullable FluidStack requested, int maximum) {
        FluidStack source = infiniteSource == null ? FluidStack.EMPTY : infiniteSource.get();
        if (source == null || source.isEmpty() || maximum <= 0
                || (requested != null && !requested.isFluidEqual(source))) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = source.copy();
        drained.setAmount(Math.min(maximum, tank.getCapacity()));
        return drained;
    }

    private void refillInfiniteSource() {
        if (infiniteSource == null) {
            return;
        }
        FluidStack configured = infiniteSource.get();
        if (configured == null || configured.isEmpty()) {
            throw new IllegalArgumentException("Infinite fluid source must not be empty");
        }
        FluidStack full = configured.copy();
        full.setAmount(tank.getCapacity());
        tank.setFluid(full);
    }

    private void buildViews() {
        unsidedView = viewFor(null);
        for (Direction direction : Direction.values()) {
            IFluidHandler view = viewFor(direction);
            if (view != null) {
                sidedViews.put(direction, view);
            }
        }
    }

    @Nullable
    private IFluidHandler viewFor(@Nullable Direction side) {
        TankAccess access = Objects.requireNonNullElse(accessBySide.apply(side), TankAccess.NONE);
        return access == TankAccess.NONE ? null : new RestrictedFluidHandler(tank, access);
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
