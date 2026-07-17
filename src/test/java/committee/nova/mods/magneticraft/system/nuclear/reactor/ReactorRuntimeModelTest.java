package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorStructure;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorRuntimeModelTest {
    private static final ReactorColumnCoordinate FUEL = new ReactorColumnCoordinate(0, 0);

    @Test
    void eachRodGroupOnlyAppliesItsPrecomputedStaticWorth() {
        Fixture fixture = fixture();
        EnumMap<ReactorRodGroup, Double> withdrawn = rods(0.0D);
        ReactorRuntimeResult baseline = fixture.step(fresh(), withdrawn, true, 1L);

        EnumMap<ReactorRodGroup, Double> groupAInserted = rods(0.0D);
        groupAInserted.put(ReactorRodGroup.A, 1.0D);
        ReactorRuntimeResult suppressed = fixture.step(fresh(), groupAInserted, true, 1L);

        EnumMap<ReactorRodGroup, Double> unrelatedGroupInserted = rods(0.0D);
        unrelatedGroupInserted.put(ReactorRodGroup.B, 1.0D);
        ReactorRuntimeResult unrelated = fixture.step(fresh(), unrelatedGroupInserted, true, 1L);

        assertTrue(suppressed.fissionPowerJoulesPerTick() < baseline.fissionPowerJoulesPerTick());
        assertEquals(baseline.fissionPowerJoulesPerTick(), unrelated.fissionPowerJoulesPerTick(), 1.0E-9D);
    }

    @Test
    void runningUpdatesOnlyDurablePerColumnStateAndShutdownKeepsDecayHeat() {
        Fixture fixture = fixture();
        ReactorRuntimeResult running = fixture.step(fresh(), rods(0.0D), true, 20L);
        FuelAssemblyState hot = running.fuelStates().get(FUEL);

        assertTrue(hot.burnupFraction() > 0.0D);
        assertTrue(hot.poisonFraction() > 0.0D);
        assertTrue(hot.decayHeatJoules() > 0.0D);
        assertTrue(hot.temperatureKelvin() > FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN);

        ReactorRuntimeResult stopped = fixture.step(hot, rods(1.0D), false, 21L);
        FuelAssemblyState cooling = stopped.fuelStates().get(FUEL);
        assertEquals(hot.burnupFraction(), cooling.burnupFraction(), 0.0D);
        assertTrue(cooling.decayHeatJoules() > 0.0D);
        assertTrue(cooling.decayHeatJoules() < hot.decayHeatJoules());
        assertTrue(cooling.poisonFraction() < hot.poisonFraction());
    }

    @Test
    void offlineCatchupIsDeterministicAndHardCapped() {
        Fixture fixture = fixture();
        FuelAssemblyState hot = new FuelAssemblyState(
                NuclearFuelGrade.STANDARD_ENRICHMENT.definitionId(),
                0.3D, 0.4D, 8_000.0D, 1_000.0D, 0.9D, 100L);
        long gameTime = 900_000L;

        ReactorRuntimeResult exactlyCapped = ReactorRuntimeModel.catchUpDecay(
                Map.of(FUEL, hot), fixture.fuels(), ReactorParameters.DEFAULT.maximumOfflineCatchupTicks(),
                gameTime, ReactorParameters.DEFAULT);
        ReactorRuntimeResult excessivelyLong = ReactorRuntimeModel.catchUpDecay(
                Map.of(FUEL, hot), fixture.fuels(), Long.MAX_VALUE,
                gameTime, ReactorParameters.DEFAULT);

        assertEquals(exactlyCapped, excessivelyLong);
        FuelAssemblyState caughtUp = exactlyCapped.fuelStates().get(FUEL);
        assertEquals(hot.burnupFraction(), caughtUp.burnupFraction(), 0.0D);
        assertEquals(gameTime, caughtUp.lastUpdateGameTime());
        assertTrue(caughtUp.decayHeatJoules() < hot.decayHeatJoules());
    }

    private static FuelAssemblyState fresh() {
        return FuelAssemblyState.fresh(NuclearFuelGrade.STANDARD_ENRICHMENT, 0L);
    }

    private static EnumMap<ReactorRodGroup, Double> rods(double insertion) {
        EnumMap<ReactorRodGroup, Double> result = new EnumMap<>(ReactorRodGroup.class);
        for (ReactorRodGroup group : ReactorRodGroup.values()) {
            result.put(group, insertion);
        }
        return result;
    }

    private static Fixture fixture() {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = new LinkedHashMap<>();
        columns.put(FUEL, NuclearReactorColumnType.FUEL_STANDARD);
        columns.put(new ReactorColumnCoordinate(1, 0), NuclearReactorColumnType.CONTROL_ROD_A);
        columns.put(new ReactorColumnCoordinate(0, 1), NuclearReactorColumnType.COOLANT_CHANNEL);
        columns.put(new ReactorColumnCoordinate(1, 1), NuclearReactorColumnType.INSTRUMENTATION);
        NuclearReactorSnapshot snapshot = new NuclearReactorSnapshot(
                NuclearReactorStructure.DESCRIPTOR, BlockPos.ZERO, Direction.NORTH, 7, 7, 7,
                BlockPos.ZERO, new BlockPos(6, 6, 6), columns, Map.of(), List.of(BlockPos.ZERO));
        Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> fuels = definitions();
        return new Fixture(snapshot, ReactorLayoutSimulator.estimate(snapshot, fuels), fuels);
    }

    private static Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> definitions() {
        Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> result = new LinkedHashMap<>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            result.put(grade.definitionId(), new NuclearFuelDefinition(
                    grade.definitionId(), grade.enrichmentPercent(), grade.thermalPowerJoulesPerTick(),
                    grade.designLifeTicks(), grade.initialReactivity(), grade.temperatureCoefficientPerKelvin(),
                    grade.voidCoefficient(), grade.decayHeatFraction(), grade.claddingFailureTemperatureKelvin(),
                    grade.loadFollowRatePerTick()));
        }
        return result;
    }

    private record Fixture(
            NuclearReactorSnapshot snapshot,
            ReactorLayoutEstimate layout,
            Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> fuels
    ) {
        ReactorRuntimeResult step(
                FuelAssemblyState state,
                Map<ReactorRodGroup, Double> rodInsertion,
                boolean fission,
                long gameTime
        ) {
            return ReactorRuntimeModel.step(
                    snapshot, layout, Map.of(FUEL, state), fuels, rodInsertion,
                    1.0D, layout.requiredCoolantFlowMilliBucketsPerTick(), fission,
                    gameTime, ReactorParameters.DEFAULT);
        }
    }
}
