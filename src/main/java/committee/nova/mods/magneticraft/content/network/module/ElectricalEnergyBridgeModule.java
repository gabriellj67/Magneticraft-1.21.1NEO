package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Explicit 1J:1FE boundary between Magneticraft voltage and an existing FE buffer.
 */
public final class ElectricalEnergyBridgeModule implements MachineModule {
    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule electricity;
    private final EnergyStorageModule energy;
    private final double receiveThresholdVolts;
    private final double dischargeThresholdVolts;
    private final int maxTransfer;
    private final boolean canDischarge;

    public ElectricalEnergyBridgeModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNetworkModule electricity,
            EnergyStorageModule energy,
            double receiveThresholdVolts,
            double dischargeThresholdVolts,
            int maxTransfer,
            boolean canDischarge
    ) {
        if (receiveThresholdVolts < 0.0 || dischargeThresholdVolts < 0.0 || maxTransfer < 0) {
            throw new IllegalArgumentException("Invalid electrical bridge limits");
        }
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.electricity = Objects.requireNonNull(electricity);
        this.energy = Objects.requireNonNull(energy);
        this.receiveThresholdVolts = receiveThresholdVolts;
        this.dischargeThresholdVolts = dischargeThresholdVolts;
        this.maxTransfer = maxTransfer;
        this.canDischarge = canDischarge;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void serverTick() {
        if (!electricity.hasConnections() && electricity.node().energyJoules() > 0.0D) {
            chargeFeBuffer();
        } else if (electricity.node().voltage() >= receiveThresholdVolts) {
            chargeFeBuffer();
        } else if (canDischarge
                && electricity.hasConnections()
                && electricity.node().voltage() < dischargeThresholdVolts) {
            dischargeFeBuffer();
        }
    }

    private void chargeFeBuffer() {
        int room = energy.receiveEnergy(maxTransfer, true);
        int available = (int) Math.floor(electricity.node().removeEnergy(room, true));
        int accepted = energy.receiveEnergy(available, true);
        if (accepted <= 0) {
            return;
        }
        double removed = electricity.node().removeEnergy(accepted, false);
        int inserted = energy.receiveEnergy((int) Math.floor(removed), false);
        if (inserted > 0) {
            host.markChanged();
        }
    }

    private void dischargeFeBuffer() {
        double targetEnergy = electricity.node().capacitance()
                * dischargeThresholdVolts
                * dischargeThresholdVolts;
        int requested = (int) Math.min(
                maxTransfer,
                Math.ceil(Math.max(0.0, targetEnergy - electricity.node().energyJoules()))
        );
        int available = energy.extractEnergy(requested, true);
        int accepted = (int) Math.floor(electricity.node().addEnergy(available, true));
        if (accepted <= 0) {
            return;
        }
        int extracted = energy.extractEnergy(accepted, false);
        double inserted = electricity.node().addEnergy(extracted, false);
        if (inserted > 0.0) {
            host.markChanged();
        }
    }
}
