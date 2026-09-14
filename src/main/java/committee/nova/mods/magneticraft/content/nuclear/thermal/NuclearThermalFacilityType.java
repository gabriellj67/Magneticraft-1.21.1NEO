package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureDescriptor;

/** Supported variable-volume heat-chain structures and their public size contracts. */
public enum NuclearThermalFacilityType {
    STEAM_GENERATOR(
            "nuclear_steam_generator",
            new VariableNuclearStructureDescriptor(
                    Magneticraft.id("nuclear_steam_generator"), range(5, 9), range(5, 9), range(5, 11))
    ),
    CONDENSER(
            "nuclear_condenser",
            new VariableNuclearStructureDescriptor(
                    Magneticraft.id("nuclear_condenser"), range(5, 9), range(5, 11), range(5, 9))
    ),
    COOLING_TOWER(
            "nuclear_cooling_tower",
            new VariableNuclearStructureDescriptor(
                    Magneticraft.id("nuclear_cooling_tower"), range(5, 13), range(5, 13), range(7, 21))
    );

    private final String id;
    private final VariableNuclearStructureDescriptor descriptor;

    NuclearThermalFacilityType(String id, VariableNuclearStructureDescriptor descriptor) {
        this.id = id;
        this.descriptor = descriptor;
    }

    public String id() {
        return id;
    }

    public VariableNuclearStructureDescriptor descriptor() {
        return descriptor;
    }

    public int tankCapacity(int width, int length, int height) {
        return Math.max(32_000, width * length * height * 250);
    }

    private static VariableNuclearStructureDescriptor.IntRange range(int minimum, int maximum) {
        return new VariableNuclearStructureDescriptor.IntRange(minimum, maximum);
    }
}
