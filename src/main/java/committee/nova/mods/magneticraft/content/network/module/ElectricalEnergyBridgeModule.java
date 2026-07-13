package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Explicit 1J:1FE boundary between Magneticraft voltage and an existing FE buffer.
 */
public final class ElectricalEnergyBridgeModule implements MachineModule {
    private static final double CHARGE_RAMP_VOLTS = 10.0D;

    private final ResourceLocation id;
    private final ElectricalNetworkModule electricity;
    private final EnergyStorageModule energy;
    private final double chargeThresholdVolts;
    private final double dischargeThresholdVolts;
    private final int maxTransfer;

    public ElectricalEnergyBridgeModule(
            ResourceLocation id,
            ElectricalNetworkModule electricity,
            EnergyStorageModule energy,
            double chargeThresholdVolts,
            double dischargeThresholdVolts,
            int maxTransfer
    ) {
        if (!Double.isFinite(chargeThresholdVolts)
                || !Double.isFinite(dischargeThresholdVolts)
                || chargeThresholdVolts < dischargeThresholdVolts
                || dischargeThresholdVolts < 0.0
                || maxTransfer < 0) {
            throw new IllegalArgumentException("Invalid electrical bridge limits");
        }
        this.id = Objects.requireNonNull(id);
        this.electricity = Objects.requireNonNull(electricity);
        this.energy = Objects.requireNonNull(energy);
        this.chargeThresholdVolts = chargeThresholdVolts;
        this.dischargeThresholdVolts = dischargeThresholdVolts;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void serverTick() {
        double voltage = electricity.node().voltage();
        if (voltage > chargeThresholdVolts) {
            chargeFeBuffer();
        } else if (voltage < dischargeThresholdVolts) {
            dischargeFeBuffer();
        }
    }

    private void chargeFeBuffer() {
        double ramp = Math.min(
                1.0D,
                (electricity.node().voltage() - chargeThresholdVolts) / CHARGE_RAMP_VOLTS
        );
        int rate = (int) Math.floor(ramp * maxTransfer);
        int room = energy.receiveEnergy(rate, true);
        int transferred = wholeJoules(electricity.node().removeEnergy(room, true));
        if (transferred <= 0) {
            return;
        }
        electricity.node().removeEnergy(transferred, false);
        energy.receiveEnergy(transferred, false);
    }

    private void dischargeFeBuffer() {
        int available = energy.extractEnergy(maxTransfer, true);
        int transferred = wholeJoules(electricity.node().addEnergy(available, true));
        if (transferred <= 0) {
            return;
        }
        energy.extractEnergy(transferred, false);
        electricity.node().addEnergy(transferred, false);
    }

    private static int wholeJoules(double joules) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(Math.max(0.0D, joules)));
    }
}
