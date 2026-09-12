package committee.nova.mods.magneticraft.content.network.module;

import org.jetbrains.annotations.Nullable;

/** A block entity whose tier may be supplied by validated placement item data. */
public interface TieredElectricalHost {
    @Nullable
    ElectricalNetworkModule tieredElectricalModule();
}
