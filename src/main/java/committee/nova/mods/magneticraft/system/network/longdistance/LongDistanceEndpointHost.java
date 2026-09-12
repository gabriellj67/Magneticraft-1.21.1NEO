package committee.nova.mods.magneticraft.system.network.longdistance;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import net.minecraft.core.Direction;

import java.util.Optional;

import java.util.Set;

/** Loaded adapter for a block entity that owns one or more wire terminals. */
public interface LongDistanceEndpointHost extends MachineModuleHost, TieredElectricalHost {
    ElectricalNetworkModule electricity();

    @Override
    default ElectricalNetworkModule tieredElectricalModule() {
        return electricity();
    }

    Set<LongDistancePort> longDistancePorts();

    default ElectricalNetworkModule electricity(LongDistancePort port) {
        if (!longDistancePorts().contains(port)) {
            throw new IllegalArgumentException("Unsupported long-distance port " + port);
        }
        return electricity();
    }

    default Optional<LongDistanceEndpoint> longDistanceEndpoint(LongDistancePort port) {
        if (!longDistancePorts().contains(port)) {
            return Optional.empty();
        }
        ElectricalNetworkModule module = electricity(port);
        return Optional.of(new LongDistanceEndpoint(position(), module.terminalId(), port, module.tierId()));
    }

    default Optional<LongDistanceEndpoint> longDistanceEndpointForInteraction(Direction clickedFace) {
        return longDistancePorts().size() == 1
                ? longDistanceEndpoint(longDistancePorts().iterator().next())
                : Optional.empty();
    }
}
