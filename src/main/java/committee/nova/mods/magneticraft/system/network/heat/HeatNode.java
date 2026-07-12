package committee.nova.mods.magneticraft.system.network.heat;

/**
 * Iron-like thermal mass compatible with Magneticraft's legacy heat semantics.
 */
public final class HeatNode {
    public static final double IDEAL_GAS_CONSTANT = 8.3144598;
    public static final double AMBIENT_TEMPERATURE_KELVIN = 293.15;
    public static final double IRON_MOLAR_MASS_GRAMS = 55.845;

    private final double massKilograms;
    private final double conductivity;
    private final double molarMassGrams;
    private double internalEnergyJoules;

    public HeatNode(double massKilograms, double conductivity) {
        this(massKilograms, conductivity, IRON_MOLAR_MASS_GRAMS);
    }

    public HeatNode(double massKilograms, double conductivity, double molarMassGrams) {
        if (!(massKilograms > 0.0) || conductivity < 0.0 || !(molarMassGrams > 0.0)) {
            throw new IllegalArgumentException("Invalid heat node material properties");
        }
        this.massKilograms = massKilograms;
        this.conductivity = conductivity;
        this.molarMassGrams = molarMassGrams;
        setTemperature(AMBIENT_TEMPERATURE_KELVIN);
    }

    public double temperatureKelvin() {
        return internalEnergyJoules / heatCapacityJoulesPerKelvin();
    }

    public double internalEnergyJoules() {
        return internalEnergyJoules;
    }

    public double massKilograms() {
        return massKilograms;
    }

    public double conductivity() {
        return conductivity;
    }

    public double molarMassGrams() {
        return molarMassGrams;
    }

    public double heatCapacityJoulesPerKelvin() {
        double moles = massKilograms * 1_000.0 / molarMassGrams;
        return 1.5 * moles * IDEAL_GAS_CONSTANT;
    }

    public double addHeat(double joules, boolean simulate) {
        if (!Double.isFinite(joules)) {
            return 0.0;
        }
        if (joules < 0.0) {
            return -removeHeat(-joules, simulate);
        }
        if (!simulate) {
            internalEnergyJoules += joules;
        }
        return joules;
    }

    public double removeHeat(double joules, boolean simulate) {
        double removed = Math.min(Math.max(0.0, finite(joules)), internalEnergyJoules);
        if (!simulate) {
            internalEnergyJoules -= removed;
        }
        return removed;
    }

    public void setInternalEnergyJoules(double joules) {
        internalEnergyJoules = Math.max(0.0, finite(joules));
    }

    public void setTemperature(double kelvin) {
        setInternalEnergyJoules(Math.max(0.0, finite(kelvin)) * heatCapacityJoulesPerKelvin());
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
