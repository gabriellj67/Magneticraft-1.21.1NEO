package committee.nova.mods.magneticraft.system.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;

import java.util.List;
import java.util.Map;

/** Four independent design metrics plus supporting engineering values; intentionally no aggregate score. */
public record ReactorLayoutEstimate(
        Map<ReactorColumnCoordinate, ReactorColumnEstimate> columns,
        double powerDensityJoulesPerTick,
        double fuelEnergyJoulesPerColumn,
        double safetyMarginPercent,
        double loadFollowingPercent,
        double shutdownMarginPercent,
        double requiredCoolantFlowMilliBucketsPerTick,
        List<String> warnings
) {
    public ReactorLayoutEstimate {
        columns = Map.copyOf(columns);
        warnings = List.copyOf(warnings);
    }

    public static ReactorLayoutEstimate empty(String warning) {
        return new ReactorLayoutEstimate(Map.of(), 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, List.of(warning));
    }
}
