package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Bounded per-column update; static neutronic coupling remains owned by {@link ReactorLayoutSimulator}. */
public final class ReactorRuntimeModel {
    private static final double MINIMUM_COOLING_DENOMINATOR = 0.15D;
    private static final int OFFLINE_CHUNK_TICKS = 200;

    private ReactorRuntimeModel() {
    }

    public static ReactorRuntimeResult step(
            NuclearReactorSnapshot snapshot,
            ReactorLayoutEstimate layout,
            Map<ReactorColumnCoordinate, FuelAssemblyState> states,
            Map<ResourceLocation, NuclearFuelDefinition> fuels,
            Map<ReactorRodGroup, Double> rodInsertion,
            double commandedPowerFraction,
            double actualCoolantFlowMilliBucketsPerTick,
            boolean fissionEnabled,
            long gameTime,
            ReactorParameters parameters
    ) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(layout);
        Objects.requireNonNull(states);
        Objects.requireNonNull(fuels);
        Objects.requireNonNull(rodInsertion);
        Objects.requireNonNull(parameters);
        double command = fissionEnabled ? clamp(commandedPowerFraction, 0.0D, 1.0D) : 0.0D;
        double coolantFraction = layout.requiredCoolantFlowMilliBucketsPerTick() <= 0.0D
                ? 0.0D
                : clamp(actualCoolantFlowMilliBucketsPerTick
                / layout.requiredCoolantFlowMilliBucketsPerTick(), 0.0D, 1.5D);
        Map<ReactorColumnCoordinate, FuelAssemblyState> next = new LinkedHashMap<>();
        Totals totals = new Totals();
        for (Map.Entry<ReactorColumnCoordinate, FuelAssemblyState> entry : states.entrySet()) {
            ReactorColumnCoordinate coordinate = entry.getKey();
            FuelAssemblyState state = entry.getValue();
            NuclearReactorColumnType columnType = snapshot.columns().get(coordinate);
            ReactorColumnEstimate estimate = layout.columns().get(coordinate);
            NuclearFuelDefinition fuel = fuels.get(state.fuelId());
            if (columnType == null || !columnType.isFuel() || estimate == null || fuel == null
                    || !columnType.fuelGrade().definitionId().equals(state.fuelId())) {
                next.put(coordinate, state);
                totals.include(state, 0.0D);
                continue;
            }
            double controlSuppression = 0.0D;
            for (ReactorRodGroup group : ReactorRodGroup.values()) {
                controlSuppression += estimate.controlWorth(group)
                        * clamp(rodInsertion.getOrDefault(group, 1.0D), 0.0D, 1.0D);
            }
            double temperatureFeedback = clamp(
                    1.0D + fuel.temperatureCoefficientPerKelvin()
                            * (state.temperatureKelvin() - 600.0D), 0.05D, 1.25D);
            double availableFuel = Math.max(0.0D, 1.0D - state.burnupFraction());
            double poisonPenalty = Math.max(0.15D, 1.0D - state.poisonFraction() * 0.45D);
            double fissionPower = estimate.thermalPowerJoulesPerTick() * command
                    * Math.max(0.0D, 1.0D - Math.min(0.98D, controlSuppression))
                    * availableFuel * poisonPenalty * temperatureFeedback;
            double normalizedPower = clamp(
                    fissionPower / Math.max(1.0D, fuel.thermalPowerJoulesPerTick()), 0.0D, 2.0D);
            double burnup = clamp(state.burnupFraction()
                    + fissionPower / Math.max(1.0D, fuel.totalEnergyJoules()), 0.0D, 1.0D);
            double poison = clamp(state.poisonFraction()
                    + normalizedPower * parameters.poisonBuildPerTick()
                    - state.poisonFraction() * parameters.poisonDecayPerTick(), 0.0D, 1.0D);
            double targetDecay = fissionPower * fuel.decayHeatFraction();
            double decayHeat = command > 0.0D
                    ? approach(state.decayHeatJoules(), targetDecay, parameters.decayHeatResponsePerTick())
                    : state.decayHeatJoules() * (1.0D - parameters.decayHeatLossPerTick());
            double normalizedHeat = (fissionPower + decayHeat)
                    / Math.max(1.0D, fuel.thermalPowerJoulesPerTick());
            double targetTemperature = FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN
                    + parameters.fuelTemperatureRiseAtFullPowerKelvin() * normalizedHeat
                    / Math.max(MINIMUM_COOLING_DENOMINATOR, coolantFraction);
            double temperature = approach(state.temperatureKelvin(), targetTemperature,
                    parameters.fuelTemperatureResponsePerTick());
            double excessTemperature = Math.max(0.0D,
                    temperature - fuel.claddingFailureTemperatureKelvin());
            double cladding = clamp(state.claddingIntegrity()
                    - excessTemperature * parameters.claddingDamagePerKelvinTick(), 0.0D, 1.0D);
            FuelAssemblyState updated = new FuelAssemblyState(
                    state.fuelId(), burnup, poison, Math.max(0.0D, decayHeat),
                    Math.max(0.0D, temperature), cladding, Math.max(0L, gameTime));
            next.put(coordinate, updated);
            totals.include(updated, fissionPower);
        }
        return totals.result(next);
    }

    /** Deterministic capped catch-up that assumes fission stopped and no active cooling while unloaded. */
    public static ReactorRuntimeResult catchUpDecay(
            Map<ReactorColumnCoordinate, FuelAssemblyState> states,
            Map<ResourceLocation, NuclearFuelDefinition> fuels,
            long elapsedTicks,
            long gameTime,
            ReactorParameters parameters
    ) {
        int remaining = (int) Math.min(
                Math.max(0L, elapsedTicks), parameters.maximumOfflineCatchupTicks());
        Map<ReactorColumnCoordinate, FuelAssemblyState> next = new LinkedHashMap<>(states);
        while (remaining > 0) {
            int ticks = Math.min(OFFLINE_CHUNK_TICKS, remaining);
            Map<ReactorColumnCoordinate, FuelAssemblyState> updated = new LinkedHashMap<>();
            for (Map.Entry<ReactorColumnCoordinate, FuelAssemblyState> entry : next.entrySet()) {
                FuelAssemblyState state = entry.getValue();
                NuclearFuelDefinition fuel = fuels.get(state.fuelId());
                if (fuel == null) {
                    updated.put(entry.getKey(), state);
                    continue;
                }
                double decayMultiplier = Math.pow(1.0D - parameters.decayHeatLossPerTick(), ticks);
                double decayHeat = state.decayHeatJoules() * decayMultiplier;
                double averageDecay = (state.decayHeatJoules() + decayHeat) * 0.5D;
                double normalizedHeat = averageDecay / Math.max(1.0D, fuel.thermalPowerJoulesPerTick());
                double targetTemperature = FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN
                        + parameters.fuelTemperatureRiseAtFullPowerKelvin() * normalizedHeat
                        / MINIMUM_COOLING_DENOMINATOR;
                double response = 1.0D - Math.pow(
                        1.0D - parameters.fuelTemperatureResponsePerTick(), ticks);
                double temperature = approach(state.temperatureKelvin(), targetTemperature, response);
                double poison = state.poisonFraction()
                        * Math.pow(1.0D - parameters.poisonDecayPerTick(), ticks);
                double excessTemperature = Math.max(0.0D,
                        temperature - fuel.claddingFailureTemperatureKelvin());
                double cladding = clamp(state.claddingIntegrity()
                        - excessTemperature * parameters.claddingDamagePerKelvinTick() * ticks,
                        0.0D, 1.0D);
                updated.put(entry.getKey(), new FuelAssemblyState(
                        state.fuelId(), state.burnupFraction(), poison, decayHeat,
                        temperature, cladding, Math.max(0L, gameTime)));
            }
            next = updated;
            remaining -= ticks;
        }
        Totals totals = new Totals();
        next.values().forEach(state -> totals.include(state, 0.0D));
        return totals.result(next);
    }

    public static EnumMap<ReactorRodGroup, Double> fullyInsertedRods() {
        EnumMap<ReactorRodGroup, Double> rods = new EnumMap<>(ReactorRodGroup.class);
        for (ReactorRodGroup group : ReactorRodGroup.values()) {
            rods.put(group, 1.0D);
        }
        return rods;
    }

    private static double approach(double current, double target, double fraction) {
        return current + (target - current) * clamp(fraction, 0.0D, 1.0D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Totals {
        private int count;
        private double fission;
        private double decay;
        private double temperature;
        private double hottest = FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN;
        private double minimumCladding = 1.0D;
        private double burnup;
        private double poison;

        void include(FuelAssemblyState state, double fissionPower) {
            count++;
            fission += fissionPower;
            decay += state.decayHeatJoules();
            temperature += state.temperatureKelvin();
            hottest = Math.max(hottest, state.temperatureKelvin());
            minimumCladding = Math.min(minimumCladding, state.claddingIntegrity());
            burnup += state.burnupFraction();
            poison += state.poisonFraction();
        }

        ReactorRuntimeResult result(Map<ReactorColumnCoordinate, FuelAssemblyState> states) {
            if (count == 0) {
                return ReactorRuntimeResult.empty(states);
            }
            return new ReactorRuntimeResult(states, fission, decay, fission + decay,
                    temperature / count, hottest, minimumCladding, burnup / count, poison / count);
        }
    }
}
