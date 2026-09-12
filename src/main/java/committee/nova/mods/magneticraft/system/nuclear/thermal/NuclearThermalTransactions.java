package committee.nova.mods.magneticraft.system.nuclear.thermal;

import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;

/**
 * Pure, loader-independent transaction rules for the two-loop nuclear heat chain.
 * Every operation is bounded by all inputs and all output capacity before state is mutated.
 */
public final class NuclearThermalTransactions {
    private NuclearThermalTransactions() {
    }

    public static Transfer stateConversion(int sourceAmount, int destinationSpace, int requested) {
        int moved = minimum(sourceAmount, destinationSpace, requested);
        return new Transfer(moved, moved);
    }

    public static PumpTransfer pump(
            int coolantAvailable,
            int outputSpace,
            int availableJoules,
            double flowSetting,
            ReactorParameters parameters
    ) {
        double setting = clamp(flowSetting, 0.0D, 1.0D);
        int requested = (int) Math.floor(parameters.mainPumpMaximumFlowMilliBucketsPerTick() * setting);
        int energyLimited = (int) Math.floor(availableJoules / parameters.mainPumpJoulesPerMilliBucket());
        int coolant = minimum(coolantAvailable, outputSpace, requested, energyLimited);
        int energy = (int) Math.ceil(coolant * parameters.mainPumpJoulesPerMilliBucket());
        return new PumpTransfer(coolant, energy);
    }

    public static SteamGeneratorTransfer steamGenerator(
            int hotCoolantAvailable,
            int coldCoolantSpace,
            int waterAvailable,
            int steamSpace,
            int requestedHotCoolant,
            ReactorParameters parameters
    ) {
        int steamRatio = parameters.steamGeneratorSteamPerWaterMilliBucket();
        int coolant = minimum(
                hotCoolantAvailable,
                coldCoolantSpace,
                waterAvailable,
                steamSpace / steamRatio,
                requestedHotCoolant
        );
        return new SteamGeneratorTransfer(
                coolant,
                coolant,
                coolant,
                coolant * steamRatio,
                (long) coolant * parameters.primaryCoolantEnthalpyJoulesPerMilliBucket()
        );
    }

    public static TurbineTransfer turbine(
            int steamAvailable,
            int exhaustSpace,
            int acceptedJoules,
            int requestedSteam,
            boolean venting,
            ReactorParameters parameters
    ) {
        double efficiency = venting ? parameters.turbineVentingEfficiency() : 1.0D;
        double joulesPerSteam = parameters.turbineJoulesPerSteamMilliBucket() * efficiency;
        int energyLimited = joulesPerSteam <= 0.0D
                ? 0
                : (int) Math.floor(acceptedJoules / joulesPerSteam);
        int steam = minimum(
                steamAvailable,
                venting ? Integer.MAX_VALUE : exhaustSpace,
                requestedSteam,
                energyLimited
        );
        int generated = (int) Math.floor(steam * joulesPerSteam);
        return new TurbineTransfer(steam, venting ? 0 : steam, generated, venting);
    }

    public static CondenserTransfer condenser(
            int exhaustAvailable,
            int waterSpace,
            long acceptedHeatJoules,
            int requestedExhaust,
            ReactorParameters parameters
    ) {
        int ratio = parameters.condenserSteamPerWaterMilliBucket();
        int heatLimited = (int) Math.min(
                Integer.MAX_VALUE,
                Math.floor(acceptedHeatJoules / parameters.condenserHeatJoulesPerSteamMilliBucket())
        );
        int steam = minimum(exhaustAvailable, waterSpace * ratio, requestedExhaust, heatLimited);
        steam -= steam % ratio;
        return new CondenserTransfer(
                steam,
                steam / ratio,
                (long) Math.ceil(steam * parameters.condenserHeatJoulesPerSteamMilliBucket())
        );
    }

    public static long coolingTowerCapacity(
            int fillBlocks,
            int fans,
            double environmentTemperatureKelvin,
            int makeupWaterAvailable,
            ReactorParameters parameters
    ) {
        if (fillBlocks <= 0 || fans <= 0 || makeupWaterAvailable <= 0) {
            return 0L;
        }
        double ambientFactor = clamp((323.15D - environmentTemperatureKelvin) / 35.0D, 0.2D, 1.25D);
        long fillCapacity = (long) fillBlocks * parameters.coolingTowerHeatJoulesPerFillBlockTick();
        long fanCapacity = (long) fans * parameters.coolingTowerHeatJoulesPerFanTick();
        long makeupCapacity = (long) makeupWaterAvailable * 100L;
        return (long) Math.floor(Math.min(Math.min(fillCapacity, fanCapacity), makeupCapacity) * ambientFactor);
    }

    private static int minimum(int... values) {
        int result = Integer.MAX_VALUE;
        for (int value : values) {
            result = Math.min(result, Math.max(0, value));
        }
        return result;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Transfer(int consumed, int produced) {
    }

    public record PumpTransfer(int coolantMoved, int energyConsumedJoules) {
    }

    public record SteamGeneratorTransfer(
            int hotCoolantConsumed,
            int coldCoolantProduced,
            int waterConsumed,
            int steamProduced,
            long heatTransferredJoules
    ) {
    }

    public record TurbineTransfer(
            int steamConsumed,
            int exhaustProduced,
            int energyGeneratedJoules,
            boolean vented
    ) {
    }

    public record CondenserTransfer(
            int exhaustConsumed,
            int waterProduced,
            long heatRejectedJoules
    ) {
    }
}
