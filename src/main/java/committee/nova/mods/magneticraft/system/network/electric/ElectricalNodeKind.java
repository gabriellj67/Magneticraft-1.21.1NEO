package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;

/** Selects the voltage-tier capacitance contract used by a physical node. */
public enum ElectricalNodeKind {
    MACHINE {
        @Override
        public double capacitance(VoltageTier tier) {
            return tier.machineCapacitanceFarads();
        }
    },
    CONDUCTOR {
        @Override
        public double capacitance(VoltageTier tier) {
            return tier.conductorCapacitanceFarads();
        }
    };

    public abstract double capacitance(VoltageTier tier);
}
