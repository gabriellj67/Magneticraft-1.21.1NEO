package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Persisted and lifecycle-safe directional fluid capability module.
 */
public final class FluidTankModule implements MachineModule {
    private final ResourceLocation id;
    private final Predicate<Direction> exposedSides;
    private final FluidTank tank;
    private LazyOptional<IFluidHandler> capability = LazyOptional.empty();

    public FluidTankModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            Predicate<FluidStack> validator,
            Predicate<Direction> exposedSides
    ) {
        this.id = Objects.requireNonNull(id);
        Objects.requireNonNull(host);
        this.exposedSides = Objects.requireNonNull(exposedSides);
        this.tank = new FluidTank(capacity, Objects.requireNonNull(validator)) {
            @Override
            protected void onContentsChanged() {
                host.markChanged();
            }
        };
        reviveCapabilities();
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
    public void invalidateCapabilities() {
        capability.invalidate();
        capability = LazyOptional.empty();
    }

    @Override
    public void reviveCapabilities() {
        capability = LazyOptional.of(() -> tank);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> requested, @Nullable Direction side) {
        if (requested == ForgeCapabilities.FLUID_HANDLER && exposedSides.test(side)) {
            return capability.cast();
        }
        return LazyOptional.empty();
    }

    public FluidTank tank() {
        return tank;
    }
}
