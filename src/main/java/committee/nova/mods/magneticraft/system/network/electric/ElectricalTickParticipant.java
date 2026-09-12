package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;

/** Optional deterministic source/sink hooks around passive edge exchange. */
public interface ElectricalTickParticipant {
    default void injectElectricalEnergy(PhysicalNetworkManager manager) {
    }

    default void extractElectricalEnergy(PhysicalNetworkManager manager) {
    }

    default void commitElectricalState(PhysicalNetworkManager manager) {
    }
}
