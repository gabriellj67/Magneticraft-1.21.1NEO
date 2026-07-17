package committee.nova.mods.magneticraft.content.fluid;

/**
 * Immutable fluid properties transcribed from Nova 1.12's fluid registry.
 */
public enum FluidDefinition {
    COLD_REACTOR_COOLANT(
            "cold_reactor_coolant", "Cold Reactor Coolant", "冷态反应堆冷却剂",
            563, 740, 120, false, "steam", 0xFF4E9ED4
    ),
    HOT_REACTOR_COOLANT(
            "hot_reactor_coolant", "Hot Reactor Coolant", "高温反应堆冷却剂",
            588, 690, 100, false, "steam", 0xFFFF7A3D
    ),
    LOW_PRESSURE_EXHAUST_STEAM(
            "low_pressure_exhaust_steam", "Low-Pressure Exhaust Steam", "低压乏汽",
            373, 1, 10, true, "steam", 0xFFD5DCE5
    ),
    STEAM("steam", "Steam", "蒸汽", 373, 1, 10, true),
    OIL("crude_oil", "Crude Oil", "原油", 298, 1100, 2000, false),
    HOT_CRUDE("heated_crude_oil", "Heated Crude Oil", "加热原油", 873, 10, 20, true),
    LPG("lpg", "Liquid Petroleum Gas", "液化石油气", 298, 300, 1000, false),
    LIGHT_OIL("light_oil", "Light Oil", "轻油", 298, 700, 1000, false),
    HEAVY_OIL("heavy_oil", "Heavy Oil", "重油", 298, 800, 1000, false),
    NATURAL_GAS("natural_gas", "Natural Gas", "天然气", 298, 1, 10, true),
    NAPHTHA("naphtha", "Naphtha", "石脑油", 298, 800, 1000, false),
    PLASTIC("liquid_plastic", "Liquid Plastic", "液化塑料", 298, 900, 1000, false),
    GASOLINE("gasoline", "Gasoline", "汽油", 298, 500, 1000, false),
    KEROSENE("kerosene", "Kerosene", "煤油", 298, 600, 1000, false),
    DIESEL("diesel", "Diesel", "柴油", 298, 700, 1000, false),
    LUBRICANT("lubricant", "Lubricant", "润滑油", 298, 600, 1000, false),
    FUEL("fuel_oil", "Fuel Oil", "燃料油", 298, 700, 1000, false),
    OIL_RESIDUE("oil_residue", "Oil Residue", "油渣", 298, 800, 2000, false),
    WOOD_GAS("wood_gas", "Wood Gas", "木煤气", 373, 1, 10, true);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final int temperatureKelvin;
    private final int density;
    private final int viscosity;
    private final boolean gaseous;
    private final String textureId;
    private final int tintColor;

    FluidDefinition(
            String id,
            String englishName,
            String chineseName,
            int temperatureKelvin,
            int density,
            int viscosity,
            boolean gaseous
    ) {
        this(id, englishName, chineseName, temperatureKelvin, density, viscosity, gaseous, id, 0xFFFFFFFF);
    }

    FluidDefinition(
            String id,
            String englishName,
            String chineseName,
            int temperatureKelvin,
            int density,
            int viscosity,
            boolean gaseous,
            String textureId,
            int tintColor
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.temperatureKelvin = temperatureKelvin;
        this.density = density;
        this.viscosity = viscosity;
        this.gaseous = gaseous;
        this.textureId = textureId;
        this.tintColor = tintColor;
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public int temperatureKelvin() {
        return temperatureKelvin;
    }

    public int density() {
        return density;
    }

    public int viscosity() {
        return viscosity;
    }

    public boolean isGaseous() {
        return gaseous;
    }

    public String textureId() {
        return textureId;
    }

    public int tintColor() {
        return tintColor;
    }

    public static FluidDefinition byId(String id) {
        for (FluidDefinition definition : values()) {
            if (definition.id.equals(id)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Unknown Magneticraft fluid id: " + id);
    }

    public String translationKey() {
        return "fluid_type.magneticraft." + id;
    }
}
