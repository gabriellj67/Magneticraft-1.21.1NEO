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

    private final ElectricalNode node;
    private final double edgeResistance;
    private final double maxCurrent;
    private final Predicate<Direction> sideFilter;
    private double netCurrentAmps;

    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNode node,
            double edgeResistance,
            double maxCurrent,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.ELECTRICITY);
        if (edgeResistance < 0.0 || maxCurrent < 0.0) {
            throw new IllegalArgumentException("Electrical module limits must be non-negative");
        }
        this.node = Objects.requireNonNull(node);
        this.edgeResistance = edgeResistance;
        this.maxCurrent = maxCurrent;
        this.sideFilter = Objects.requireNonNull(sideFilter);
    }

    public ElectricalNode node() {
        return node;
    }

    public boolean hasConnections() {
        var manager = manager();
        return manager != null && !manager.neighbors(domain(), position()).isEmpty();
    }

    @Override
    public Optional<ElectricalReading> electricalReading(Direction side) {
        if (!isSideEnabled(side)) {
            return Optional.empty();
        }
        double voltage = node.voltage();
        return Optional.of(new ElectricalReading(voltage, netCurrentAmps, voltage * netCurrentAmps));
    }

    @Override
    public void beforeNetworkTick(PhysicalNetworkManager manager) {
        netCurrentAmps = 0.0;
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other instanceof ElectricalNetworkModule electrical)) {
            return;
        }
        double resistance = Math.max(edgeResistance, electrical.edgeResistance);
        double current = Math.min(maxCurrent, electrical.maxCurrent);
        ElectricalLink.Transfer transfer = ElectricalLink.transfer(node, electrical.node, resistance, current);
        if (transfer.moved()) {
            double signedCurrent = transfer.currentAmps();
            if (transfer.firstWasSource()) {
                netCurrentAmps -= signedCurrent;
                electrical.netCurrentAmps += signedCurrent;
            } else {
                netCurrentAmps += signedCurrent;
                electrical.netCurrentAmps -= signedCurrent;
            }
            markStateChanged();
            electrical.markStateChanged();
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
