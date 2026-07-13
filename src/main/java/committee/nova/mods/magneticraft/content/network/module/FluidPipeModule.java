package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.fluid.FluidLink;
import committee.nova.mods.magneticraft.system.network.fluid.FluidNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A 160mB pipe cell with explicit input/output/disabled side adapters.
 */
public final class FluidPipeModule extends AbstractPhysicalNetworkModule {
    private static final String FLUID_KEY_TAG = "fluid_key";
    private static final String AMOUNT_TAG = "amount";
    private static final String SIDE_MODES_TAG = "side_modes";
    private static final String IO_CURSOR_TAG = "io_cursor";

    private final FluidNode node;
    private final int maxRate;
    private final SideMode[] sideModes = new SideMode[Direction.values().length];
    private final Map<Direction, LazyOptional<IFluidHandler>> capabilities = new EnumMap<>(Direction.class);
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
        }
        reviveCapabilities();
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
        if (!(other instanceof FluidPipeModule pipe)) {
            return;
        }
        FluidLink.Transfer transfer = FluidLink.transfer(node, pipe.node, Math.min(maxRate, pipe.maxRate));
        if (transfer.moved()) {
            markStateChanged();
            pipe.markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
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
    protected void saveNetworkData(CompoundTag tag) {
        tag.putString(FLUID_KEY_TAG, node.fluidKey());
        tag.putInt(AMOUNT_TAG, node.amount());
        int[] modes = new int[Direction.values().length];
        for (Direction direction : Direction.values()) {
            modes[direction.ordinal()] = sideMode(direction).ordinal();
        }
        tag.putIntArray(SIDE_MODES_TAG, modes);
        tag.putInt(IO_CURSOR_TAG, ioCursor);
    }

    @Override
    public void invalidateCapabilities() {
        capabilities.values().forEach(LazyOptional::invalidate);
        capabilities.clear();
    }

    @Override
    public void reviveCapabilities() {
        for (Direction direction : Direction.values()) {
            capabilities.put(direction, LazyOptional.of(() -> new SideFluidHandler(direction)));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER
                && side != null
                && isSideEnabled(side)
                && sideMode(side) != SideMode.DISABLED) {
            LazyOptional<IFluidHandler> result = capabilities.get(side);
            return result == null ? LazyOptional.empty() : result.cast();
        }
        return LazyOptional.empty();
    }

    private boolean pullFrom(ServerLevel level, Direction direction) {
        IFluidHandler handler = adjacentHandler(level, direction);
        if (handler == null || node.amount() >= node.capacity()) {
            return false;
        }
        FluidStack offered = handler.drain(maxRate, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return false;
        }
        int accepted = node.fill(fluidKey(offered), offered.getAmount(), true);
        if (accepted <= 0) {
            return false;
        }
        FluidStack requested = offered.copy();
        requested.setAmount(accepted);
        FluidStack drained = handler.drain(requested, IFluidHandler.FluidAction.EXECUTE);
        int filled = node.fill(fluidKey(drained), drained.getAmount(), false);
        if (filled < drained.getAmount()) {
            FluidStack restore = drained.copy();
            restore.setAmount(drained.getAmount() - filled);
            handler.fill(restore, IFluidHandler.FluidAction.EXECUTE);
        }
        if (filled > 0) {
            markStateChanged();
        }
        return filled > 0;
    }

    private boolean pushTo(ServerLevel level, Direction direction) {
        IFluidHandler handler = adjacentHandler(level, direction);
        FluidStack available = fluidStack(Math.min(maxRate, node.amount()));
        if (handler == null || available.isEmpty()) {
            return false;
        }
        int accepted = handler.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }
        int drained = node.drain(node.fluidKey(), accepted, false);
        FluidStack executing = available.copy();
        executing.setAmount(drained);
        int filled = handler.fill(executing, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained) {
            node.fill(fluidKey(executing), drained - filled, false);
        }
        if (filled > 0) {
            markStateChanged();
        }
        return filled > 0;
    }

    @Nullable
    private IFluidHandler adjacentHandler(ServerLevel level, Direction direction) {
        BlockPos target = position().relative(direction);
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).orElse(null);
    }

    private FluidStack fluidStack(int amount) {
        if (node.isEmpty() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(node.fluidKey());
        if (id == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private static String fluidKey(FluidStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id == null ? "" : id.toString();
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
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fluidStack(node.amount()) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? node.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && sideMode(side) == SideMode.PASSIVE && isSideEnabled(side);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!automationEnabled()
                    || sideMode(side) != SideMode.PASSIVE
                    || resource.isEmpty()
                    || !isSideEnabled(side)) {
                return 0;
            }
            int accepted = node.fill(fluidKey(resource), resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                markStateChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!automationEnabled()
                    || sideMode(side) != SideMode.ACTIVE
                    || resource.isEmpty()
                    || !isSideEnabled(side)
                    || !fluidKey(resource).equals(node.fluidKey())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!automationEnabled()
                    || sideMode(side) != SideMode.ACTIVE
                    || !isSideEnabled(side)
                    || node.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack result = fluidStack(Math.min(maxDrain, node.amount()));
            int drained = node.drain(node.fluidKey(), result.getAmount(), action.simulate());
            result.setAmount(drained);
            if (drained > 0 && action.execute()) {
                markStateChanged();
            }
            return result;
        }
    }
}
