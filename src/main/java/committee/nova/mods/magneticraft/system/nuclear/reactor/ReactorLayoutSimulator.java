package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearFuelDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Local-neighbour/ray precomputation; no runtime diffusion or volume iteration is required. */
public final class ReactorLayoutSimulator {
    public static final double COOLANT_HEAT_CAPACITY_JOULES_PER_MILLIBUCKET = 12_000.0D;
    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private ReactorLayoutSimulator() {
    }

    public static ReactorLayoutEstimate estimate(
            NuclearReactorSnapshot snapshot,
            Map<ResourceLocation, NuclearFuelDefinition> fuels
    ) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(fuels);
        Map<ReactorColumnCoordinate, RawEstimate> raw = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        double maximumPower = 0.0D;
        int fuelCount = 0;

        for (Map.Entry<ReactorColumnCoordinate, NuclearReactorColumnType> entry : snapshot.columns().entrySet()) {
            NuclearReactorColumnType type = entry.getValue();
            if (!type.isFuel()) {
                continue;
            }
            NuclearFuelGrade grade = Objects.requireNonNull(type.fuelGrade());
            NuclearFuelDefinition fuel = fuels.get(grade.definitionId());
            if (fuel == null) {
                warnings.add("missing_fuel:" + grade.definitionId());
                continue;
            }
            RawEstimate estimate = estimateFuel(entry.getKey(), type, snapshot.columns(), fuel);
            raw.put(entry.getKey(), estimate);
            maximumPower = Math.max(maximumPower, estimate.power());
            fuelCount++;
        }
        if (fuelCount == 0) {
            return ReactorLayoutEstimate.empty(warnings.isEmpty() ? "no_fuel" : warnings.get(0));
        }

        Map<ReactorColumnCoordinate, ReactorColumnEstimate> columns = new LinkedHashMap<>();
        double totalPower = 0.0D;
        double totalEnergy = 0.0D;
        double totalSafety = 0.0D;
        double totalFollowing = 0.0D;
        double totalShutdown = 0.0D;
        for (Map.Entry<ReactorColumnCoordinate, RawEstimate> entry : raw.entrySet()) {
            RawEstimate value = entry.getValue();
            double heat = maximumPower <= 0.0D ? 0.0D : value.power() / maximumPower;
            columns.put(entry.getKey(), new ReactorColumnEstimate(
                    value.power(), heat, value.coupling(), value.moderation(), value.reflection(),
                    value.cooling(), value.shutdown(), value.instrumentation(), value.hotspot()
            ));
            totalPower += value.power();
            totalEnergy += value.energy();
            totalSafety += value.safety();
            totalFollowing += value.following();
            totalShutdown += value.shutdownMargin();
        }
        int activeCells = snapshot.activeWidth() * snapshot.activeLength();
        return new ReactorLayoutEstimate(
                columns,
                totalPower / Math.max(1, activeCells),
                totalEnergy / fuelCount,
                totalSafety / fuelCount,
                totalFollowing / fuelCount,
                totalShutdown / fuelCount,
                totalPower / COOLANT_HEAT_CAPACITY_JOULES_PER_MILLIBUCKET,
                warnings
        );
    }

    private static RawEstimate estimateFuel(
            ReactorColumnCoordinate coordinate,
            NuclearReactorColumnType type,
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> layout,
            NuclearFuelDefinition fuel
    ) {
        double coupling = 0.0D;
        double moderation = 0.0D;
        double cooling = 0.0D;
        double shutdown = 0.0D;
        double instrumentation = 0.0D;
        double reflection = 0.0D;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                NuclearReactorColumnType neighbour = layout.get(offset(coordinate, dx, dz));
                if (neighbour == null) {
                    continue;
                }
                double diagonalFactor = dx != 0 && dz != 0 ? 0.5D : 1.0D;
                if (neighbour.isFuel()) {
                    coupling += 0.11D * diagonalFactor;
                } else if (neighbour == NuclearReactorColumnType.COOLANT_CHANNEL) {
                    moderation += 0.09D * diagonalFactor;
                    cooling += 0.28D * diagonalFactor;
                } else if (neighbour.isControlRod()) {
                    shutdown += 0.24D * diagonalFactor;
                } else if (neighbour == NuclearReactorColumnType.INSTRUMENTATION) {
                    instrumentation += 0.18D * diagonalFactor;
                } else if (neighbour == NuclearReactorColumnType.REFLECTOR) {
                    reflection += 0.07D * diagonalFactor;
                }
            }
        }

        for (int[] direction : ORTHOGONAL) {
            for (int distance = 2; distance <= 4; distance++) {
                NuclearReactorColumnType seen = layout.get(offset(
                        coordinate, direction[0] * distance, direction[1] * distance));
                if (seen == null) {
                    break;
                }
                if (seen == NuclearReactorColumnType.REFLECTOR) {
                    reflection += 0.12D / distance;
                    break;
                }
                if (seen.isControlRod()) {
                    shutdown += 0.16D / distance;
                } else if (seen == NuclearReactorColumnType.COOLANT_CHANNEL) {
                    cooling += 0.10D / distance;
                } else if (seen == NuclearReactorColumnType.INSTRUMENTATION) {
                    instrumentation += 0.08D / distance;
                } else if (seen.isFuel()) {
                    coupling += 0.06D / distance;
                }
            }
        }

        double reactivity = fuel.initialReactivity();
        double hotspot = reactivity * (1.0D + coupling + moderation + reflection)
                / (1.0D + cooling * 0.55D);
        double power = fuel.thermalPowerJoulesPerTick()
                * (1.0D + coupling + moderation + reflection)
                * (0.82D + 0.18D * Math.min(1.5D, cooling));
        double utilizationFactor = clamp(0.75D + reflection * 0.45D + moderation * 0.28D
                - Math.max(0.0D, hotspot - 1.25D) * 0.12D, 0.55D, 1.35D);
        double energy = fuel.totalEnergyJoules() * utilizationFactor;
        double safety = clamp(38.0D + cooling * 34.0D + shutdown * 30.0D
                + instrumentation * 18.0D + reflection * 8.0D
                - Math.max(0.0D, hotspot - 1.0D) * 38.0D, 0.0D, 100.0D);
        double following = clamp(24.0D + shutdown * 42.0D + instrumentation * 36.0D
                + cooling * 12.0D - Math.max(0.0D, reactivity - 1.0D) * 28.0D,
                0.0D, 100.0D);
        double shutdownMargin = clamp(shutdown * 95.0D / Math.max(0.25D, reactivity + coupling),
                0.0D, 100.0D);
        return new RawEstimate(power, energy, coupling, moderation, reflection, cooling,
                shutdown, instrumentation, hotspot, safety, following, shutdownMargin);
    }

    private static ReactorColumnCoordinate offset(ReactorColumnCoordinate coordinate, int dx, int dz) {
        int x = coordinate.x() + dx;
        int z = coordinate.z() + dz;
        return x < 0 || z < 0 ? new ReactorColumnCoordinate(Integer.MAX_VALUE, Integer.MAX_VALUE)
                : new ReactorColumnCoordinate(x, z);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record RawEstimate(
            double power,
            double energy,
            double coupling,
            double moderation,
            double reflection,
            double cooling,
            double shutdown,
            double instrumentation,
            double hotspot,
            double safety,
            double following,
            double shutdownMargin
    ) {
    }
}
