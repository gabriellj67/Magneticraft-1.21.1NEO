package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.system.network.pressure.PressureGasStack;
import committee.nova.mods.magneticraft.system.network.pressure.PressureFluidConversion;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Objects;

/**
 * Whole-millibucket NeoForge fluid view over the authoritative pressure node.
 */
public final class PressureFluidHandler implements IFluidHandler {
    private final PressureNode node;
    private final Runnable onChanged;

    public PressureFluidHandler(PressureNode node, Runnable onChanged) {
        this.node = Objects.requireNonNull(node);
        this.onChanged = Objects.requireNonNull(onChanged);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation gasId = node.gasId().orElse(null);
        Fluid fluid = gasId == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(gasId);
        int amount = PressureFluidConversion.wholeMillibuckets(node.gasKpaLiters());
        return fluid == null || fluid == Fluids.EMPTY || amount <= 0
                ? FluidStack.EMPTY
                : new FluidStack(fluid, amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? PressureFluidConversion.wholeMillibuckets(node.capacityKpaLiters()) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && !stack.isEmpty() && stack.getFluid().is(Tags.Fluids.GASEOUS);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) {
            return 0;
        }
        ResourceLocation gasId = BuiltInRegistries.FLUID.getKey(resource.getFluid());
        if (gasId == null || node.gasId().filter(id -> !id.equals(gasId)).isPresent()) {
            return 0;
        }
        double room = node.capacityKpaLiters() - node.gasKpaLiters();
        int acceptedMillibuckets = Math.min(resource.getAmount(), PressureFluidConversion.wholeMillibuckets(room));
        if (acceptedMillibuckets <= 0) {
            return 0;
        }
        if (action.execute()) {
            double accepted = node.insert(new PressureGasStack(
                    gasId,
                    PressureFluidConversion.toGasKpaLiters(acceptedMillibuckets)
            ), false);
            if (accepted > 0.0D) {
                onChanged.run();
            }
        }
        return acceptedMillibuckets;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        ResourceLocation requested = BuiltInRegistries.FLUID.getKey(resource.getFluid());
        if (requested == null || node.gasId().filter(requested::equals).isEmpty()) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation gasId = node.gasId().orElse(null);
        Fluid fluid = gasId == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(gasId);
        int drainedMillibuckets = Math.min(
                maxDrain,
                PressureFluidConversion.wholeMillibuckets(node.gasKpaLiters())
        );
        if (fluid == null || fluid == Fluids.EMPTY || drainedMillibuckets <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            node.extract(gasId, PressureFluidConversion.toGasKpaLiters(drainedMillibuckets), false);
            onChanged.run();
        }
        return new FluidStack(fluid, drainedMillibuckets);
    }
}
