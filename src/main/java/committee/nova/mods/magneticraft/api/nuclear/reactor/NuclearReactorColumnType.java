package committee.nova.mods.magneticraft.api.nuclear.reactor;

import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import org.jetbrains.annotations.Nullable;

/** Stable identifiers for one full-height reactor-core column. */
public enum NuclearReactorColumnType {
    FUEL_LOW(NuclearFuelGrade.LOW_ENRICHMENT),
    FUEL_STANDARD(NuclearFuelGrade.STANDARD_ENRICHMENT),
    FUEL_HIGH(NuclearFuelGrade.HIGH_ENRICHMENT),
    CONTROL_ROD_A(null),
    CONTROL_ROD_B(null),
    CONTROL_ROD_C(null),
    CONTROL_ROD_D(null),
    COOLANT_CHANNEL(null),
    INSTRUMENTATION(null),
    REFLECTOR(null);

    @Nullable
    private final NuclearFuelGrade fuelGrade;

    NuclearReactorColumnType(@Nullable NuclearFuelGrade fuelGrade) {
        this.fuelGrade = fuelGrade;
    }

    public boolean isFuel() {
        return fuelGrade != null;
    }

    public boolean isControlRod() {
        return this == CONTROL_ROD_A || this == CONTROL_ROD_B
                || this == CONTROL_ROD_C || this == CONTROL_ROD_D;
    }

    @Nullable
    public NuclearFuelGrade fuelGrade() {
        return fuelGrade;
    }
}
