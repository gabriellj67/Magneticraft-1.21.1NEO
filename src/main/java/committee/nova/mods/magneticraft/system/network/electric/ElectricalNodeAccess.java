package committee.nova.mods.magneticraft.system.network.electric;

import net.minecraft.resources.ResourceLocation;

/** Narrow runtime view used by the topology manager to exchange native electricity. */
public interface ElectricalNodeAccess {
    ElectricalNode electricalNode();

    ResourceLocation electricalTierId();

    boolean electricalProfileBound();

    void markElectricalStateChanged();
}
