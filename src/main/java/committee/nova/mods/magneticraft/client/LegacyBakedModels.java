package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;

import java.util.List;
import java.util.stream.IntStream;

/** Client-only identifiers for offline-converted historical moving parts. */
final class LegacyBakedModels {
    static final int ANIMATION_FRAME_COUNT = 8;

    static final ResourceLocation WIND_TURBINE_ROTOR = model("wind_turbine_rotor");
    static final ResourceLocation ELECTRIC_POLE = model("electric_pole");
    static final ResourceLocation ELECTRIC_POLE_TRANSFORMER = model("electric_pole_transformer");
    static final ResourceLocation MINING_ROBOT_PROPELLERS = model("mining_robot_propellers");
    static final ResourceLocation MINING_ROBOT_DRILL = model("mining_robot_drill");
    static final ResourceLocation COMPUTER_SCREEN = model("computer_screen");
    static final ResourceLocation SOLAR_PANEL_TRACKING = model("solar_panel_tracking");
    static final ResourceLocation BIG_COMBUSTION_CHAMBER_FIRE = model("big_combustion_chamber_fire");
    static final ResourceLocation STEAM_TURBINE_BLADE = model("steam_turbine_blades");

    static final List<ResourceLocation> ELECTRIC_ENGINE = frames("electric_engine_moving");
    static final List<ResourceLocation> INSERTER = frames("inserter_moving");
    static final List<ResourceLocation> GRINDER = frames("grinder_moving");
    static final List<ResourceLocation> HYDRAULIC_PRESS = frames("hydraulic_press_moving");
    static final List<ResourceLocation> SIEVE = frames("sieve_moving");
    static final List<ResourceLocation> STEAM_ENGINE = frames("steam_engine_moving");

    /** Full historical shells baked for asset validation and visual comparison. */
    static final List<ResourceLocation> REFERENCE_MODELS = List.of(
            model("solar_mirror"),
            model("solar_panel"),
            model("solar_tower"),
            model("sluice_box"),
            model("oil_heater"),
            model("pumpjack"),
            model("refinery"),
            model("shelving_unit"),
            model("shipping_container"),
            model("big_combustion_chamber_body"),
            model("big_electric_furnace"),
            model("big_steam_boiler"),
            model("grinder_body"),
            model("hydraulic_press_body"),
            model("sieve_body"),
            model("steam_engine_body"),
            model("steam_turbine_body")
    );

    private static final List<ResourceLocation> ALL = List.of(
            WIND_TURBINE_ROTOR,
            ELECTRIC_POLE,
            ELECTRIC_POLE_TRANSFORMER,
            MINING_ROBOT_PROPELLERS,
            MINING_ROBOT_DRILL,
            COMPUTER_SCREEN,
            SOLAR_PANEL_TRACKING,
            BIG_COMBUSTION_CHAMBER_FIRE,
            STEAM_TURBINE_BLADE
    );

    private LegacyBakedModels() {
    }

    static void register(ModelEvent.RegisterAdditional event) {
        ALL.forEach(event::register);
        ELECTRIC_ENGINE.forEach(event::register);
        INSERTER.forEach(event::register);
        GRINDER.forEach(event::register);
        HYDRAULIC_PRESS.forEach(event::register);
        SIEVE.forEach(event::register);
        STEAM_ENGINE.forEach(event::register);
        REFERENCE_MODELS.forEach(event::register);
    }

    static ResourceLocation frame(
            List<ResourceLocation> frames,
            long gameTick,
            float partialTick,
            float ticksPerFrame,
            boolean moving
    ) {
        int index = AnimationMath.bakedFrameIndex(
                gameTick,
                partialTick,
                frames.size(),
                ticksPerFrame,
                moving
        );
        return frames.get(index);
    }

    private static List<ResourceLocation> frames(String prefix) {
        return IntStream.range(0, ANIMATION_FRAME_COUNT)
                .mapToObj(index -> model(prefix + "_frame_" + index))
                .toList();
    }

    private static ResourceLocation model(String path) {
        return Magneticraft.id("legacy/" + path);
    }
}
