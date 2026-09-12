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
        double controlWorthA,
        double controlWorthB,
        double controlWorthC,
        double controlWorthD,
        double instrumentationCoverage,
        double hotspotFactor
) {
    public double controlWorth(committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup group) {
        return switch (group) {
            case A -> controlWorthA;
            case B -> controlWorthB;
            case C -> controlWorthC;
            case D -> controlWorthD;
        };
    }
}
