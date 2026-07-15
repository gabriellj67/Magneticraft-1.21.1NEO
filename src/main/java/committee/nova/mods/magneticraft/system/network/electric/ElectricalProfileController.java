package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;

/**
 * Internal attachment that binds one electrical device to the active data-pack snapshot.
 * A missing or incompatible profile makes every attached terminal fail closed.
 */
public interface ElectricalProfileController extends ElectricalProfileBinding {
    boolean electricalControllerBound();
}
