package committee.nova.mods.magneticraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common settings carried forward from the Magneticraft 1.12 behavior contract.
 */
public final class MagneticraftConfig {
    public static final String CRUSHING_TABLE_CAUSES_FIRE_TRANSLATION_KEY =
            "config.magneticraft.crushing_table_causes_fire";
    public static final String WATER_GENERATOR_PER_TICK_WATER_TRANSLATION_KEY =
            "config.magneticraft.water_generator_per_tick_water";

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CRUSHING_TABLE_CAUSES_FIRE;
    public static final ForgeConfigSpec.IntValue WATER_GENERATOR_PER_TICK_WATER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        CRUSHING_TABLE_CAUSES_FIRE = builder
                .comment("Set players on fire when they process blaze rods in the crushing table.")
                .translation(CRUSHING_TABLE_CAUSES_FIRE_TRANSLATION_KEY)
                .define("crushing_table_causes_fire", true);
        WATER_GENERATOR_PER_TICK_WATER = builder
                .comment("Amount of water each side of a water generator may export every tick.")
                .translation(WATER_GENERATOR_PER_TICK_WATER_TRANSLATION_KEY)
                .defineInRange("water_generator_per_tick_water", 20, 0, 32_000);
        builder.pop();
        SPEC = builder.build();
    }

    private MagneticraftConfig() {
    }
}
