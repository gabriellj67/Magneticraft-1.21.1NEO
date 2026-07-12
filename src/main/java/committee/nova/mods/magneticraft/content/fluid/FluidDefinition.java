package committee.nova.mods.magneticraft.content.fluid;

/**
 * Immutable fluid properties transcribed from Nova 1.12's fluid registry.
 */
public enum FluidDefinition {
    STEAM("steam", "Steam", "蒸汽", 373, 1, 10, true),
    OIL("oil", "Crude Oil", "原油", 298, 1100, 2000, false),
    HOT_CRUDE("hot_crude", "Hot Crude", "热油", 873, 10, 20, true),
    LPG("lpg", "Liquid Petroleum Gas", "液化石油气", 298, 300, 1000, false),
    LIGHT_OIL("light_oil", "Light Oil", "轻油", 298, 700, 1000, false),
    HEAVY_OIL("heavy_oil", "Heavy Oil", "重油", 298, 800, 1000, false),
    NATURAL_GAS("natural_gas", "Natural Gas", "天然气", 298, 1, 10, true),
    NAPHTHA("naphtha", "Naphtha", "石脑油", 298, 800, 1000, false),
    PLASTIC("plastic", "Liquid Plastic", "液化塑料", 298, 900, 1000, false),
    GASOLINE("gasoline", "Gasoline", "汽油", 298, 500, 1000, false),
    KEROSENE("kerosene", "Kerosene", "煤油", 298, 600, 1000, false),
    DIESEL("diesel", "Diesel", "柴油", 298, 700, 1000, false),
    LUBRICANT("lubricant", "Lubricant", "润滑油", 298, 600, 1000, false),
    FUEL("fuel", "Fuel", "燃油", 298, 700, 1000, false),
    OIL_RESIDUE("oil_residue", "Oil Residue", "油渣", 298, 800, 2000, false),
    WOOD_GAS("wood_gas", "Wood Gas", "木煤气", 373, 1, 10, true);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final int temperatureKelvin;
    private final int density;
    private final int viscosity;
    private final boolean gaseous;

    FluidDefinition(
            String id,
            String englishName,
            String chineseName,
            int temperatureKelvin,
            int density,
            int viscosity,
            boolean gaseous
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.temperatureKelvin = temperatureKelvin;
        this.density = density;
        this.viscosity = viscosity;
        this.gaseous = gaseous;
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

    public String translationKey() {
        return "fluid_type.magneticraft." + id;
    }
}
