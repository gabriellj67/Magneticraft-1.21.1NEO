package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorStructure;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorLayoutSimulatorTest {
    @Test
    void preservesFourIndependentDesignTradeoffs() {
        ReactorLayoutEstimate robust = estimate(layout(
                "RCR",
                "AFI",
                "RCR"
        ));
        ReactorLayoutEstimate compact = estimate(layout(
                "FFF",
                "FAF",
                "CIR"
        ));
        ReactorLayoutEstimate fast = estimate(layout(
                "ACI",
                "CFA",
                "ICR"
        ));

        assertTrue(compact.powerDensityJoulesPerTick() > robust.powerDensityJoulesPerTick());
        assertTrue(robust.safetyMarginPercent() > compact.safetyMarginPercent());
        assertTrue(fast.loadFollowingPercent() > compact.loadFollowingPercent());
        assertNotEquals(robust.fuelEnergyJoulesPerColumn(), compact.fuelEnergyJoulesPerColumn());
        assertNotEquals(compact.shutdownMarginPercent(), fast.shutdownMarginPercent());
        assertEquals(compact.powerDensityJoulesPerTick()
                        * 9.0D / ReactorLayoutSimulator.COOLANT_HEAT_CAPACITY_JOULES_PER_MILLIBUCKET,
                compact.requiredCoolantFlowMilliBucketsPerTick(), 1.0E-9D);
    }

    @Test
    void aLocalEditOnlyChangesNeighbourOrBoundedRayFuelPrecomputation() {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> before = new HashMap<>();
        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 9; x++) {
                before.put(new ReactorColumnCoordinate(x, z), NuclearReactorColumnType.FUEL_STANDARD);
            }
        }
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> after = new HashMap<>(before);
        ReactorColumnCoordinate changed = new ReactorColumnCoordinate(4, 4);
        after.put(changed, NuclearReactorColumnType.COOLANT_CHANNEL);
        ReactorLayoutEstimate baseline = estimate(snapshot(13, 13, before));
        ReactorLayoutEstimate modified = estimate(snapshot(13, 13, after));

        for (var entry : modified.columns().entrySet()) {
            ReactorColumnCoordinate coordinate = entry.getKey();
            if (coordinate.equals(changed)) {
                continue;
            }
            ReactorColumnEstimate original = baseline.columns().get(coordinate);
            if (sameStaticContribution(entry.getValue(), original)) {
                continue;
            }
            int dx = Math.abs(coordinate.x() - changed.x());
            int dz = Math.abs(coordinate.z() - changed.z());
            boolean localNeighbour = dx <= 1 && dz <= 1;
            boolean boundedRay = (dx == 0 && dz <= 4) || (dz == 0 && dx <= 4);
            assertTrue(localNeighbour || boundedRay, () -> "Unexpected global recompute effect at " + coordinate);
        }
    }

    private static boolean sameStaticContribution(ReactorColumnEstimate first, ReactorColumnEstimate second) {
        return first.thermalPowerJoulesPerTick() == second.thermalPowerJoulesPerTick()
                && first.fuelCoupling() == second.fuelCoupling()
                && first.moderation() == second.moderation()
                && first.reflection() == second.reflection()
                && first.cooling() == second.cooling()
                && first.shutdownWorth() == second.shutdownWorth()
                && first.instrumentationCoverage() == second.instrumentationCoverage()
                && first.hotspotFactor() == second.hotspotFactor();
    }

    private static ReactorLayoutEstimate estimate(Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns) {
        return estimate(snapshot(7, 7, columns));
    }

    private static ReactorLayoutEstimate estimate(NuclearReactorSnapshot snapshot) {
        return ReactorLayoutSimulator.estimate(snapshot, definitions());
    }

    private static NuclearReactorSnapshot snapshot(
            int width, int length, Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns
    ) {
        BlockPos origin = BlockPos.ZERO;
        return new NuclearReactorSnapshot(
                NuclearReactorStructure.DESCRIPTOR, origin, Direction.NORTH, width, length, 7,
                origin, new BlockPos(width - 1, 6, length - 1), columns, Map.of(), List.of(origin));
    }

    private static Map<ReactorColumnCoordinate, NuclearReactorColumnType> layout(String... rows) {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> result = new HashMap<>();
        for (int z = 0; z < rows.length; z++) {
            for (int x = 0; x < rows[z].length(); x++) {
                result.put(new ReactorColumnCoordinate(x, z), switch (rows[z].charAt(x)) {
                    case 'F' -> NuclearReactorColumnType.FUEL_STANDARD;
                    case 'A' -> NuclearReactorColumnType.CONTROL_ROD_A;
                    case 'C' -> NuclearReactorColumnType.COOLANT_CHANNEL;
                    case 'I' -> NuclearReactorColumnType.INSTRUMENTATION;
                    case 'R' -> NuclearReactorColumnType.REFLECTOR;
                    default -> throw new IllegalArgumentException("Unknown layout symbol");
                });
            }
        }
        return result;
    }

    private static Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> definitions() {
        Map<net.minecraft.resources.ResourceLocation, NuclearFuelDefinition> result = new HashMap<>();
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            result.put(grade.definitionId(), new NuclearFuelDefinition(
                    grade.definitionId(), grade.enrichmentPercent(), grade.thermalPowerJoulesPerTick(),
                    grade.designLifeTicks(), grade.initialReactivity(), grade.temperatureCoefficientPerKelvin(),
                    grade.voidCoefficient(), grade.decayHeatFraction(), grade.claddingFailureTemperatureKelvin(),
                    grade.loadFollowRatePerTick()));
        }
        return result;
    }
}
