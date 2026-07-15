package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;

/** Applies a validated placement identity to every terminal owned by a compound electrical device. */
public interface TieredElectricalPlacementHost {
    boolean applyElectricalItemData(TieredElectricalItemData data);
}
