package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;

/** Loaded receiver visible to nearby Tesla towers without chunk scanning. */
public interface WirelessEnergyReceiverHost extends MachineModuleHost {
    ElectricalNetworkModule electricity();
}
