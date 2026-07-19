package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.kinetic.KineticLink;
import committee.nova.mods.magneticraft.system.network.kinetic.KineticNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Predicate;

/** Persisted native rotary-energy node. This module never exposes Forge Energy. */
public final class KineticNetworkModule extends AbstractPhysicalNetworkModule {
    private static final String ENERGY_TAG = "energy_joules";
    private static final String CLIENT_ENERGY_TAG = "kinetic_energy_joules";

    private final KineticNode node;
    private final double maximumTransferJoulesPerTick;
    private final double frictionJoulesPerTick;
    private final Predicate<Direction> sideFilter;

    public KineticNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            KineticNode node,
            double maximumTransferJoulesPerTick,
            double frictionJoulesPerTick,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.KINETIC);
        if (!Double.isFinite(maximumTransferJoulesPerTick) || maximumTransferJoulesPerTick < 0.0D) {
            throw new IllegalArgumentException("Kinetic transfer limit must be finite and non-negative");
        }
        if (!Double.isFinite(frictionJoulesPerTick) || frictionJoulesPerTick < 0.0D) {
            throw new IllegalArgumentException("Kinetic friction must be finite and non-negative");
        }
        this.node = Objects.requireNonNull(node);
        this.maximumTransferJoulesPerTick = maximumTransferJoulesPerTick;
        this.frictionJoulesPerTick = frictionJoulesPerTick;
        this.sideFilter = Objects.requireNonNull(sideFilter);
    }

    public KineticNode node() {
        return node;
    }

    public double insertJoules(double joules, boolean simulate) {
        double accepted = node.insertJoules(joules, simulate);
        if (!simulate && accepted > 0.0D) {
            markStateChanged();
        }
        return accepted;
    }

    public double extractJoules(double joules, boolean simulate) {
        double extracted = node.extractJoules(joules, simulate);
        if (!simulate && extracted > 0.0D) {
            markStateChanged();
        }
        return extracted;
    }

    public int clientStateHash() {
        return Long.hashCode(Double.doubleToLongBits(node.energyJoules()));
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other.transferNode() instanceof KineticNetworkModule kinetic)) {
            return;
        }
        KineticLink.Transfer transfer = KineticLink.transfer(
                node,
                kinetic.node,
                Math.min(maximumTransferJoulesPerTick, kinetic.maximumTransferJoulesPerTick)
        );
        if (transfer.moved()) {
            markStateChanged();
            kinetic.markStateChanged();
        }
    }

    @Override
    public void afterNetworkTick(PhysicalNetworkManager manager) {
        if (frictionJoulesPerTick > 0.0D && node.extractJoules(frictionJoulesPerTick, false) > 0.0D) {
            markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        node.setEnergyJoules(tag.contains(ENERGY_TAG, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(ENERGY_TAG) : 0.0D);
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putDouble(ENERGY_TAG, node.energyJoules());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        node.setEnergyJoules(tag.getDouble(CLIENT_ENERGY_TAG));
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        tag.putDouble(CLIENT_ENERGY_TAG, node.energyJoules());
    }
}
