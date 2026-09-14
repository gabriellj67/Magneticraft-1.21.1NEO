package committee.nova.mods.magneticraft.content.nuclear.fuel;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;

/** Three fixed LEU item identities with different performance trade-offs. */
public enum NuclearFuelGrade {
    LOW_ENRICHMENT(
            "low_enrichment_fuel_assembly",
            "Low-Enrichment Fuel Assembly",
            "低富集燃料组件",
            2.5D,
            16_000.0D,
            540_000,
            0.85D,
            -0.00025D,
            -0.30D,
            0.055D,
            1_450.0D,
            0.0025D
    ),
    STANDARD_ENRICHMENT(
            "standard_enrichment_fuel_assembly",
            "Standard-Enrichment Fuel Assembly",
            "标准富集燃料组件",
            3.5D,
            24_000.0D,
            432_000,
            1.0D,
            -0.00030D,
            -0.32D,
            0.060D,
            1_450.0D,
            0.0040D
    ),
    HIGH_ENRICHMENT(
            "high_enrichment_fuel_assembly",
            "High-Enrichment Fuel Assembly",
            "高富集燃料组件",
            4.9D,
            36_000.0D,
            324_000,
            1.20D,
            -0.00035D,
            -0.35D,
            0.065D,
            1_450.0D,
            0.0060D
    );

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final double enrichmentPercent;
    private final double thermalPowerJoulesPerTick;
    private final int designLifeTicks;
    private final double initialReactivity;
    private final double temperatureCoefficientPerKelvin;
    private final double voidCoefficient;
    private final double decayHeatFraction;
    private final double claddingFailureTemperatureKelvin;
    private final double loadFollowRatePerTick;

    NuclearFuelGrade(
            String id,
            String englishName,
            String chineseName,
            double enrichmentPercent,
            double thermalPowerJoulesPerTick,
            int designLifeTicks,
            double initialReactivity,
            double temperatureCoefficientPerKelvin,
            double voidCoefficient,
            double decayHeatFraction,
            double claddingFailureTemperatureKelvin,
            double loadFollowRatePerTick
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.enrichmentPercent = enrichmentPercent;
        this.thermalPowerJoulesPerTick = thermalPowerJoulesPerTick;
        this.designLifeTicks = designLifeTicks;
        this.initialReactivity = initialReactivity;
        this.temperatureCoefficientPerKelvin = temperatureCoefficientPerKelvin;
        this.voidCoefficient = voidCoefficient;
        this.decayHeatFraction = decayHeatFraction;
        this.claddingFailureTemperatureKelvin = claddingFailureTemperatureKelvin;
        this.loadFollowRatePerTick = loadFollowRatePerTick;
    }

    public String id() {
        return id;
    }

    public ResourceLocation definitionId() {
        return Magneticraft.id(id);
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public double enrichmentPercent() {
        return enrichmentPercent;
    }

    public double thermalPowerJoulesPerTick() {
        return thermalPowerJoulesPerTick;
    }

    public int designLifeTicks() {
        return designLifeTicks;
    }

    public double initialReactivity() {
        return initialReactivity;
    }

    public double temperatureCoefficientPerKelvin() {
        return temperatureCoefficientPerKelvin;
    }

    public double voidCoefficient() {
        return voidCoefficient;
    }

    public double decayHeatFraction() {
        return decayHeatFraction;
    }

    public double claddingFailureTemperatureKelvin() {
        return claddingFailureTemperatureKelvin;
    }

    public double loadFollowRatePerTick() {
        return loadFollowRatePerTick;
    }
}
