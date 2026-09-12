package committee.nova.mods.magneticraft.system.network.electric.profile;

/** Implemented by loaded electrical nodes that must re-resolve profile ids after a successful reload. */
public interface ElectricalProfileBinding {
    void rebindElectricalProfile(ElectricalDataSnapshot snapshot);
}
