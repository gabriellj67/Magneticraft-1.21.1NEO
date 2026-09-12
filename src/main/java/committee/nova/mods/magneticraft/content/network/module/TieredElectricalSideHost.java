package committee.nova.mods.magneticraft.content.network.module;

import net.minecraft.core.Direction;

import java.util.Optional;

/** Selects the exact tiered terminal exposed on one side of a multi-terminal device. */
public interface TieredElectricalSideHost {
    Optional<ElectricalNetworkModule> electricalTerminal(Direction side);
}
