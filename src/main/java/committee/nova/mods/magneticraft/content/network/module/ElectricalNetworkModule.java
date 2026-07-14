package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Persisted Magneticraft electrical node; Forge Energy is intentionally absent.
 */
public final class ElectricalNetworkModule extends AbstractPhysicalNetworkModule implements ElectricalDiagnosticSource {
    private static final String ENERGY_TAG = "energy_joules";
    private static final double ADJACENT_DISTANCE = 1.0D;

    private final ElectricalNode node;
    private final Predicate<Direction> sideFilter;

    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNode node,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.ELECTRICITY);
        this.node = Objects.requireNonNull(node);
        this.sideFilter = Objects.requireNonNull(sideFilter);
    }

    public ElectricalNode node() {
        return node;
    }

    @Override
    public Optional<ElectricalReading> electricalReading(Direction side) {
        if (!isSideEnabled(side)) {
            return Optional.empty();
        }
        double voltage = node.voltage();
        double current = node.lastCompletedTickCurrentAmps();
        return Optional.of(new ElectricalReading(voltage, current, voltage * current));
    }

    @Override
    public void beforeNetworkTick(PhysicalNetworkManager manager) {
        node.beginNetworkTick();
    }

    @Override
    public void afterNetworkTick(PhysicalNetworkManager manager) {
        node.completeNetworkTick();
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other.transferNode() instanceof ElectricalNetworkModule electrical)) {
            return;
        }
        exchangeLongDistance(electrical, ADJACENT_DISTANCE);
    }

    public void exchangeLongDistance(ElectricalNetworkModule other, double distance) {
        Objects.requireNonNull(other);
        ElectricalLink.Transfer transfer = ElectricalLink.transfer(node, other.node, distance);
        if (transfer.moved()) {
            markStateChanged();
            other.markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        node.setEnergyJoules(tag.getDouble(ENERGY_TAG));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putDouble(ENERGY_TAG, node.energyJoules());
    }
}
