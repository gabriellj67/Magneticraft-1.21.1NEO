package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;

import java.util.Set;

/** Loaded adapter for a block entity that owns one or more wire terminals. */
public interface LongDistanceEndpointHost extends MachineModuleHost {
    ElectricalNetworkModule electricity();

    Set<LongDistancePort> longDistancePorts();
}
