package committee.nova.mods.magneticraft.system.network.electric.profile;

/**
 * Defines the only active exchange behavior an electrical machine may own.
 */
public enum ElectricalRole {
    CONSUMER,
    GENERATOR,
    STORAGE,
    CONVERTER,
    PASSIVE
}
