package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.pressure.PressureLink;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Independent pressure node hosted by pneumatic components.
 */
public final class PressureNetworkModule extends AbstractPhysicalNetworkModule {
    private static final String GAS_TAG = "gas_kpa_liters";

    private final PressureNode node;
    private final double conductance;
    private final double maxGasPerTick;
    private final double leakFraction;
    private final Predicate<Direction> sideFilter;

    public PressureNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            PressureNode node,
            double conductance,
            double maxGasPerTick,
            double leakFraction,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.PRESSURE);
        this.node = Objects.requireNonNull(node);
        this.conductance = conductance;
        this.maxGasPerTick = maxGasPerTick;
        this.leakFraction = leakFraction;
        this.sideFilter = Objects.requireNonNull(sideFilter);
    }

    public PressureNode node() {
        return node;
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other instanceof PressureNetworkModule pressure)) {
            return;
        }
        PressureLink.Transfer transfer = PressureLink.transfer(
                node,
                pressure.node,
                Math.min(conductance, pressure.conductance),
                Math.min(maxGasPerTick, pressure.maxGasPerTick),
                Math.max(leakFraction, pressure.leakFraction)
        );
        if (transfer.moved()) {
            markStateChanged();
            pressure.markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        node.setGasKpaLiters(tag.getDouble(GAS_TAG));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putDouble(GAS_TAG, node.gasKpaLiters());
    }
}
