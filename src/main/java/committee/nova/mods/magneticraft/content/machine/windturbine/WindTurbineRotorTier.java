package committee.nova.mods.magneticraft.content.machine.windturbine;

/** Legacy rotor families, normalized so the medium rotor keeps today's visual size. */
public enum WindTurbineRotorTier {
    SMALL("small_wind_turbine_rotor", 3, 0.33D, 0.56D),
    MEDIUM("wind_turbine_rotor", 5, 1.0D, 1.0D),
    LARGE("large_wind_turbine_rotor", 7, 2.0D, 1.48D);

    private final String id;
    private final int bladeRadius;
    private final double potency;
    private final double renderScale;

    WindTurbineRotorTier(String id, int bladeRadius, double potency, double renderScale) {
        this.id = id;
        this.bladeRadius = bladeRadius;
        this.potency = potency;
        this.renderScale = renderScale;
    }

    public String id() {
        return id;
    }

    public int bladeRadius() {
        return bladeRadius;
    }

    public double potency() {
        return potency;
    }

    public double renderScale() {
        return renderScale;
    }
}
