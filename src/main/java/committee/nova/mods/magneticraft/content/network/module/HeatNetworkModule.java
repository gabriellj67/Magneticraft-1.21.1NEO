package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.heat.HeatLink;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Persisted lossless thermal node.
 */
public final class HeatNetworkModule extends AbstractPhysicalNetworkModule {
    private static final String INTERNAL_ENERGY_TAG = "internal_energy_joules";

    private final HeatNode node;
    private final double maxTransferWatts;
    private final Predicate<Direction> sideFilter;

    public HeatNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            HeatNode node,
            double maxTransferWatts,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.HEAT);
        if (maxTransferWatts < 0.0) {
            throw new IllegalArgumentException("Heat transfer limit must be non-negative");
        }
        this.node = Objects.requireNonNull(node);
        this.maxTransferWatts = maxTransferWatts;
        this.sideFilter = Objects.requireNonNull(sideFilter);
    }

    public HeatNode node() {
        return node;
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other instanceof HeatNetworkModule heat)) {
            return;
        }
        HeatLink.Transfer transfer = HeatLink.transfer(
                node,
                heat.node,
                1.0,
                Math.min(maxTransferWatts, heat.maxTransferWatts)
        );
        if (transfer.moved()) {
            markStateChanged();
            heat.markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        node.setInternalEnergyJoules(tag.getDouble(INTERNAL_ENERGY_TAG));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putDouble(INTERNAL_ENERGY_TAG, node.internalEnergyJoules());
    }
}
