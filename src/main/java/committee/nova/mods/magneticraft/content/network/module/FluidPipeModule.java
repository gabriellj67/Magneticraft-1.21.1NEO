package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.fluid.FluidComponentStorage;
import committee.nova.mods.magneticraft.system.network.fluid.FluidNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A durable 160mB pipe cell whose passive adapter views the loaded component.
 */
public final class FluidPipeModule extends AbstractPhysicalNetworkModule {
    private static final String FLUID_KEY_TAG = "fluid_key";
    private static final String AMOUNT_TAG = "amount";
    private static final String SIDE_MODES_TAG = "side_modes";
    private static final String IO_CURSOR_TAG = "io_cursor";

    private final FluidNode node;
    private final int maxRate;
    private final SideMode[] sideModes = new SideMode[Direction.values().length];
    private final IFluidHandler[] sideHandlers = new IFluidHandler[Direction.values().length];
    private int ioCursor;

    public FluidPipeModule(ResourceLocation id, MachineModuleHost host, int capacity, int maxRate) {
        super(id, host, NetworkDomain.FLUID);
        if (maxRate < 0) {
            throw new IllegalArgumentException("Fluid pipe rate must be non-negative");
        }
        this.node = new FluidNode(capacity);
        this.maxRate = maxRate;
        for (Direction direction : Direction.values()) {
            sideModes[direction.ordinal()] = SideMode.PASSIVE;
            sideHandlers[direction.ordinal()] = new SideFluidHandler(direction);
        }
    }

    public FluidNode node() {
        return node;
    }

    public SideMode sideMode(Direction side) {
        return sideModes[side.ordinal()];
    }

    public void cycleSideMode(Direction side) {
        setSideMode(side, sideMode(side).next());
    }

    public void setSideMode(Direction side, SideMode mode) {
        Objects.requireNonNull(mode);
        if (sideModes[side.ordinal()] != mode) {
            sideModes[side.ordinal()] = mode;
            topologyChanged();
        }
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideMode(direction) != SideMode.DISABLED;
    }

    @Override
    public boolean canConnect(Direction side, PhysicalNetworkNode other) {
        return other instanceof FluidPipeModule && super.canConnect(side, other);
    }

    @Override
    public void beforeNetworkTick(PhysicalNetworkManager manager) {
        if (!automationEnabled() || !(host().level() instanceof ServerLevel level)) {
            return;
        }
        Direction[] directions = Direction.values();
        boolean moved = false;
        for (int offset = 0; offset < directions.length; offset++) {
            Direction direction = directions[(ioCursor + offset) % directions.length];
            if (!isSideEnabled(direction)
                    || manager.node(NetworkDomain.FLUID, position().relative(direction)).isPresent()) {
                continue;
            }
            if (sideMode(direction) == SideMode.PASSIVE) {
                moved |= pullFrom(level, direction);
            } else if (sideMode(direction) == SideMode.ACTIVE) {
                moved |= pushTo(level, direction);
            }
        }
        if (moved) {
            ioCursor = (ioCursor + 1) % directions.length;
        }
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        // Legacy iron pipes expose their member tanks as one concatenated view;
        // fluid is not redistributed merely because a network edge ticks.
    }

    @Override
    protected void loadNetworkData(CompoundTag tag, HolderLookup.Provider registries) {
        node.set(tag.getString(FLUID_KEY_TAG), tag.getInt(AMOUNT_TAG));
        for (Direction direction : Direction.values()) {
            sideModes[direction.ordinal()] = SideMode.PASSIVE;
        }
        int[] modes = tag.getIntArray(SIDE_MODES_TAG);
        for (Direction direction : Direction.values()) {
            if (direction.ordinal() < modes.length) {
                sideModes[direction.ordinal()] = SideMode.byOrdinal(modes[direction.ordinal()]);
            }
        }
        ioCursor = Math.floorMod(tag.getInt(IO_CURSOR_TAG), Direction.values().length);
    }

    @Override
    protected void saveNetworkData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString(FLUID_KEY_TAG, node.fluidKey());
        tag.putInt(AMOUNT_TAG, node.amount());
        int[] modes = new int[Direction.values().length];
        for (Direction direction : Direction.values()) {
            modes[direction.ordinal()] = sideMode(direction).ordinal();
        }
        tag.putIntArray(SIDE_MODES_TAG, modes);
        tag.putInt(IO_CURSOR_TAG, ioCursor);
    }

    /** Fluid-handler view exposed for the given side, or {@code null} if this module hides fluid access from it. */
    @Nullable
    public IFluidHandler view(Direction side) {
        return isSideEnabled(side) && sideMode(side) == SideMode.PASSIVE
                ? sideHandlers[side.ordinal()]
                : null;
    }

    private boolean pullFrom(ServerLevel level, Direction direction) {
        IFluidHandler handler = adjacentHandler(level, direction);
        ComponentView component = componentView();
        if (handler == null || component.isEmpty()) {
            return false;
        }
        FluidStack offered = handler.drain(maxRate, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return false;
        }
        int accepted = component.fill(fluidKey(offered), offered.getAmount(), true);
        if (accepted <= 0) {
            return false;
        }
        FluidStack requested = offered.copy();
        requested.setAmount(accepted);
        FluidStack drained = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
        int filled = component.fill(fluidKey(drained), drained.getAmount(), false);
        if (filled < drained.getAmount()) {
            FluidStack restore = drained.copy();
            restore.setAmount(drained.getAmount() - filled);
            handler.fill(restore, IFluidHandler.FluidAction.EXECUTE);
        }
        return filled > 0;
    }

    private boolean pushTo(ServerLevel level, Direction direction) {
        IFluidHandler handler = adjacentHandler(level, direction);
        ComponentView component = componentView();
        FluidComponentStorage.Drain available = component.drainAny(maxRate, true);
        FluidStack offered = fluidStack(available.fluidKey(), available.amount());
        if (handler == null || offered.isEmpty()) {
            return false;
        }
        int accepted = Math.min(
                available.amount(),
                handler.fill(offered, IFluidHandler.FluidAction.SIMULATE)
        );
        if (accepted <= 0) {
            return false;
        }
        int drained = component.drain(available.fluidKey(), accepted, false);
        FluidStack executing = offered.copy();
        executing.setAmount(drained);
        int filled = handler.fill(executing, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained) {
            component.fill(available.fluidKey(), drained - filled, false);
        }
        return filled > 0;
    }

    @Nullable
    private IFluidHandler adjacentHandler(ServerLevel level, Direction direction) {
        BlockPos target = position().relative(direction);
        var targetChunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
        if (targetChunk == null) {
            return null;
        }
        BlockEntity blockEntity = targetChunk.getBlockEntity(target);
        if (blockEntity == null) {
            return null;
        }
        return Capabilities.FluidHandler.BLOCK.getCapability(
                level,
                target,
                blockEntity.getBlockState(),
                blockEntity,
                direction.getOpposite()
        );
    }

    private static FluidStack fluidStack(String fluidKey, int amount) {
        if (fluidKey.isBlank() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(fluidKey);
        if (id == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private static String fluidKey(FluidStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return id == null ? "" : id.toString();
    }

    private ComponentView componentView() {
        PhysicalNetworkManager current = manager();
        if (current == null
                || current.node(NetworkDomain.FLUID, position()).orElse(null) != this) {
            return new ComponentView(List.of());
        }
        List<Long> positions = new ArrayList<>(current.component(NetworkDomain.FLUID, position()));
        positions.sort(Comparator.naturalOrder());
        List<FluidPipeModule> members = positions.stream()
                .map(key -> current.node(NetworkDomain.FLUID, BlockPos.of(key)).orElse(null))
                .filter(FluidPipeModule.class::isInstance)
                .map(FluidPipeModule.class::cast)
                .toList();
        return new ComponentView(members);
    }

    public enum SideMode {
        PASSIVE,
        ACTIVE,
        DISABLED;

        public SideMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        private static SideMode byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : PASSIVE;
        }
    }

    private final class SideFluidHandler implements IFluidHandler {
        private final Direction side;

        private SideFluidHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return available() ? componentView().storage.tankCount() : 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (!available()) {
                return FluidStack.EMPTY;
            }
            FluidComponentStorage storage = componentView().storage;
            return tank >= 0 && tank < storage.tankCount()
                    ? fluidStack(storage.fluidKey(tank), storage.amount(tank))
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (!available()) {
                return 0;
            }
            FluidComponentStorage storage = componentView().storage;
            return tank >= 0 && tank < storage.tankCount() ? storage.capacity(tank) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (!available() || stack.isEmpty()) {
                return false;
            }
            FluidComponentStorage storage = componentView().storage;
            return tank >= 0
                    && tank < storage.tankCount()
                    && storage.isFluidValid(tank, fluidKey(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!available() || resource.isEmpty()) {
                return 0;
            }
            return componentView().fill(fluidKey(resource), resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!available() || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            String key = fluidKey(resource);
            int drained = componentView().drain(key, resource.getAmount(), action.simulate());
            return fluidStack(key, drained);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!available()) {
                return FluidStack.EMPTY;
            }
            FluidComponentStorage.Drain drained = componentView().drainAny(maxDrain, action.simulate());
            return fluidStack(drained.fluidKey(), drained.amount());
        }

        private boolean available() {
            return sideMode(side) == SideMode.PASSIVE && isSideEnabled(side);
        }
    }

    private final class ComponentView {
        private final List<FluidPipeModule> members;
        private final FluidComponentStorage storage;

        private ComponentView(List<FluidPipeModule> members) {
            this.members = List.copyOf(members);
            storage = new FluidComponentStorage(this.members.stream().map(FluidPipeModule::node).toList());
        }

        private boolean isEmpty() {
            return members.isEmpty();
        }

        private int fill(String key, int amount, boolean simulate) {
            int[] before = snapshot();
            int filled = storage.fill(key, amount, simulate);
            if (!simulate && filled > 0) {
                markChanges(before);
            }
            return filled;
        }

        private int drain(String key, int amount, boolean simulate) {
            int[] before = snapshot();
            int drained = storage.drain(key, amount, simulate);
            if (!simulate && drained > 0) {
                markChanges(before);
            }
            return drained;
        }

        private FluidComponentStorage.Drain drainAny(int amount, boolean simulate) {
            int[] before = snapshot();
            FluidComponentStorage.Drain drained = storage.drainAny(amount, simulate);
            if (!simulate && drained.amount() > 0) {
                markChanges(before);
            }
            return drained;
        }

        private int[] snapshot() {
            int[] amounts = new int[members.size()];
            for (int index = 0; index < members.size(); index++) {
                amounts[index] = members.get(index).node.amount();
            }
            return amounts;
        }

        private void markChanges(int[] before) {
            for (int index = 0; index < members.size(); index++) {
                FluidPipeModule member = members.get(index);
                if (member.node.amount() != before[index]) {
                    member.markStateChanged();
                }
            }
        }
    }
}
