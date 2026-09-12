package committee.nova.mods.magneticraft.system.nuclear.safety;

import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorAccidentStage;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;

/** Pure deterministic transition model. It never rolls random failures or skips warning stages. */
public final class ReactorAccidentModel {
    private ReactorAccidentModel() {
    }

    public static Result step(Input input, ReactorParameters parameters) {
        double coolant = clamp(input.coolantFraction(), 0.0D, 2.0D);
        double temperature = Math.max(0.0D, input.hottestTemperatureKelvin());
        double pressure = Math.max(0.1D, input.pressureMegapascals());
        double vessel = clamp(input.vesselIntegrity(), 0.0D, 1.0D);
        double containment = clamp(input.containmentIntegrity(), 0.0D, 1.0D);
        double energy = Math.max(0.0D, input.accidentEnergyJoules());
        boolean heatActive = input.heatJoulesPerTick() > 1.0D;
        boolean coolingShortage = heatActive && coolant < parameters.minimumCoolantFraction();

        double temperatureExcess = Math.max(0.0D,
                temperature - parameters.primaryBoilingTemperatureKelvin());
        if (coolingShortage) {
            pressure += 0.002D + temperatureExcess * parameters.pressureRiseMegapascalsPerKelvinTick();
        } else {
            pressure = Math.max(0.1D, pressure - parameters.pressureReliefMegapascalsPerTick());
        }

        ReactorAccidentStage candidate = ReactorAccidentStage.NORMAL;
        String reason = "stable";
        if (coolingShortage) {
            candidate = ReactorAccidentStage.COOLING_SHORTAGE;
            reason = "cooling_shortage";
        }
        if (heatActive && temperature >= parameters.primaryBoilingTemperatureKelvin()) {
            candidate = ReactorAccidentStage.LOCAL_BOILING;
            reason = "local_boiling";
        }
        if (input.minimumCladdingIntegrity() <= parameters.claddingAccidentThreshold()) {
            candidate = ReactorAccidentStage.CLADDING_DAMAGE;
            reason = "cladding_damage";
        }
        if (temperature >= parameters.fuelMeltingTemperatureKelvin()
                || input.previousStage().ordinal() >= ReactorAccidentStage.FUEL_MELT.ordinal()) {
            candidate = ReactorAccidentStage.FUEL_MELT;
            reason = "fuel_melt";
            energy += input.heatJoulesPerTick();
        }
        if (pressure >= parameters.pressureAlarmMegapascals()
                || input.previousStage().ordinal() >= ReactorAccidentStage.PRESSURE_RISE.ordinal()) {
            candidate = ReactorAccidentStage.PRESSURE_RISE;
            reason = "pressure_rise";
        }
        if (pressure > parameters.vesselDesignPressureMegapascals()) {
            vessel = Math.max(0.0D, vessel - (pressure - parameters.vesselDesignPressureMegapascals())
                    * parameters.vesselDamagePerMegapascalTick());
        }
        if (pressure >= parameters.vesselBurstPressureMegapascals() || vessel <= 0.0D
                || input.previousStage().ordinal() >= ReactorAccidentStage.VESSEL_BREACH.ordinal()) {
            candidate = ReactorAccidentStage.VESSEL_BREACH;
            reason = "vessel_breach";
            vessel = 0.0D;
            containment = Math.max(0.0D, containment - input.heatJoulesPerTick() / 1_000_000.0D
                    * parameters.containmentDamagePerMegajoule());
        }
        if (containment <= 0.0D
                || input.previousStage() == ReactorAccidentStage.CONTAINMENT_BREACH) {
            candidate = ReactorAccidentStage.CONTAINMENT_BREACH;
            reason = "containment_breach";
            containment = 0.0D;
        }

        ReactorAccidentStage stage = irreversibleMaximum(input.previousStage(), candidate);
        if (stage != candidate) {
            reason = persistentReason(stage);
        }
        return new Result(stage, reason, pressure, vessel, containment, energy);
    }

    private static ReactorAccidentStage irreversibleMaximum(
            ReactorAccidentStage previous, ReactorAccidentStage candidate
    ) {
        if (previous.ordinal() < ReactorAccidentStage.CLADDING_DAMAGE.ordinal()) {
            return candidate;
        }
        return previous.ordinal() > candidate.ordinal() ? previous : candidate;
    }

    private static String persistentReason(ReactorAccidentStage stage) {
        return stage.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Input(
            ReactorAccidentStage previousStage,
            double heatJoulesPerTick,
            double coolantFraction,
            double hottestTemperatureKelvin,
            double minimumCladdingIntegrity,
            double pressureMegapascals,
            double vesselIntegrity,
            double containmentIntegrity,
            double accidentEnergyJoules
    ) {
        public Input {
            if (previousStage == null) {
                throw new IllegalArgumentException("previousStage must not be null");
            }
        }
    }

    public record Result(
            ReactorAccidentStage stage,
            String reason,
            double pressureMegapascals,
            double vesselIntegrity,
            double containmentIntegrity,
            double accidentEnergyJoules
    ) {
    }
}
