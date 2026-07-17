package committee.nova.mods.magneticraft.system.nuclear.reactor;

/** Static contribution of one column; heatFraction is normalized across this layout only. */
public record ReactorColumnEstimate(
        double thermalPowerJoulesPerTick,
        double heatFraction,
        double fuelCoupling,
        double moderation,
        double reflection,
        double cooling,
        double shutdownWorth,
        double instrumentationCoverage,
        double hotspotFactor
) {
}
