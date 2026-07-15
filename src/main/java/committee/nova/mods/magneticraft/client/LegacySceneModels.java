package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.model.LegacySceneRenderer;
import committee.nova.mods.magneticraft.client.model.ModelSceneSelection;
import committee.nova.mods.magneticraft.client.model.ModelTransform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Set;

/** Fixed original-format model parts used by block-entity renderers. */
final class LegacySceneModels {
    private static final String[] SOLAR_PANEL_PARTS = {
            "Panel1-1", "Panel1-2", "Panel1-3",
            "Panel2-1", "Panel2-2", "Panel2-3",
            "Panel3-1", "Panel3-2", "Panel3-3",
            "Panel4-1", "Panel4-2", "Panel4-3",
            "Panel5-1", "Panel5-2", "Panel5-3",
            "Panel6-1", "Panel6-2", "Panel6-3"
    };
    private static final String[] STEAM_ENGINE_LID_PARTS = {
            "gearbox_lid_side", "gear_box_lid_top", "gearbox_lid_lock"
    };

    static final Part COMPUTER_SCREEN = mcx("computer", includeNodes("screen"));
    static final Part MINING_ROBOT_PROPELLERS = mcx("mining_robot", includeNodes("prop1", "prop2"));
    static final Part MINING_ROBOT_DRILL = mcx(
            "mining_robot",
            includeNodes("drill1", "drill2", "drill3", "drill4", "drill5")
    );
    static final Part WIND_TURBINE_ROTOR = new Part(
            source("mcx/wind_turbine.mcx"),
            new ModelSceneSelection(Set.of(), Set.of(), Set.of("Shape2"), Set.of()),
            ModelTransform.IDENTITY.withTranslation(0.0F, -5.0F, 0.0F)
    );
    static final Part ELECTRIC_POLE = mcx("electric_pole", ModelSceneSelection.ALL);
    static final Part ELECTRIC_POLE_TRANSFORMER = mcx("electric_pole_transformer", ModelSceneSelection.ALL);
    static final Part BOX_TRANSFORMER_FORWARD = gltf("box_transformer", includeSubtrees("arrow_forward"));
    static final Part BOX_TRANSFORMER_REVERSE = gltf("box_transformer", includeSubtrees("arrow_reverse"));
    static final Part FUSE_INTACT = gltf("fuse_box", includeNodes("fuse_intact"));
    static final Part FUSE_BLOWN = gltf("fuse_box", includeNodes("fuse_blown"));
    static final Part BREAKER_CLOSED = gltf("circuit_breaker", includeNodes("switch_closed"));
    static final Part BREAKER_OPEN = gltf("circuit_breaker", includeNodes("switch_open"));
    static final Part SLUICE_BOX_BODY = mcx("sluice_box", excludeNodes("gravel"));
    static final Part SLUICE_BOX_GRAVEL = mcx("sluice_box", includeNodes("gravel"));
    static final Part SLUICE_BOX_WATER = mcx("sluice_box_water", ModelSceneSelection.ALL);
    static final Part FEEDING_TROUGH = mcx("feeding_trough", ModelSceneSelection.ALL);
    static final Part SMALL_TANK_SHELL = mcx("small_tank", includeNodes("top"));
    static final Part SMALL_TANK_BASE = mcx("small_tank", includeNodes("base"));
    static final Part COMBUSTION_CHAMBER_BODY = mcx("combustion_chamber", excludeNodes("Door"));
    static final Part COMBUSTION_CHAMBER_DOOR = mcx("combustion_chamber", includeNodes("Door"));
    static final Part STEAM_BOILER = mcx("steam_boiler", ModelSceneSelection.ALL);
    static final Part GASIFICATION_UNIT = mcx("gasification_unit", ModelSceneSelection.ALL);
    static final Part INSERTER = gltf("inserter", excludeSubtrees("item"));
    static final Part ELECTRIC_ENGINE = gltf("electric_engine", ModelSceneSelection.ALL);
    static final Part INSERTER_MOVING = gltf("inserter", includeSubtrees("level1"));
    static final Part ELECTRIC_ENGINE_MOVING = gltf(
            "electric_engine",
            includeSubtrees("Group 18", "piston")
    );
    static final Part BIG_COMBUSTION_CHAMBER_BODY = gltf(
            "big_combustion_chamber",
            excludeSubtrees("fire_on", "fire_off")
    );
    static final Part BIG_COMBUSTION_CHAMBER_FIRE_ON = gltf(
            "big_combustion_chamber",
            includeSubtrees("fire_on")
    );
    static final Part BIG_COMBUSTION_CHAMBER_FIRE_OFF = gltf(
            "big_combustion_chamber",
            includeSubtrees("fire_off")
    );
    static final Part BIG_ELECTRIC_FURNACE = gltf("big_electric_furnace", ModelSceneSelection.ALL);
    static final Part BIG_STEAM_BOILER = gltf("big_steam_boiler", ModelSceneSelection.ALL);
    static final Part SHIPPING_CONTAINER = mcx("container", ModelSceneSelection.ALL);
    static final Part GRINDER = gltf("grinder", ModelSceneSelection.ALL);
    static final Part HYDRAULIC_PRESS = gltf("hydraulic_press", ModelSceneSelection.ALL);
    static final Part OIL_HEATER = mcx("oil_heater", ModelSceneSelection.ALL);
    static final Part PUMPJACK = mcx("pumpjack", ModelSceneSelection.ALL);
    static final Part REFINERY = mcx("refinery", ModelSceneSelection.ALL);
    static final Part SHELVING_UNIT_BODY = mcx("shelving_unit", excludeNodes(crateNames()));
    static final Part SIEVE = gltf("sieve", ModelSceneSelection.ALL);
    static final Part SOLAR_MIRROR_BODY = mcx(
            "solar_mirror",
            excludeNodes("mirror1", "mirror2", "mirror_rod")
    );
    static final Part SOLAR_MIRROR_MOVING = mcx(
            "solar_mirror",
            includeNodes("mirror1", "mirror2", "mirror_rod")
    );
    static final Part SOLAR_PANEL_BODY = mcx("solar_panel", excludeNodes(SOLAR_PANEL_PARTS));
    static final Part SOLAR_PANEL_X_LEFT = solarPanelParts(1, 4);
    static final Part SOLAR_PANEL_X_MIDDLE = solarPanelParts(2, 5);
    static final Part SOLAR_PANEL_X_RIGHT = solarPanelParts(3, 6);
    static final Part SOLAR_PANEL_Z_FRONT = solarPanelParts(1, 2, 3);
    static final Part SOLAR_PANEL_Z_BACK = solarPanelParts(4, 5, 6);
    static final Part SOLAR_TOWER = mcx("solar_tower", ModelSceneSelection.ALL);
    static final Part STEAM_ENGINE_BODY = gltf(
            "steam_engine",
            excludeSubtrees(STEAM_ENGINE_LID_PARTS)
    );
    static final Part STEAM_ENGINE_LID = gltf(
            "steam_engine",
            includeSubtrees(STEAM_ENGINE_LID_PARTS)
    );
    static final Part STEAM_TURBINE_BODY = gltf("steam_turbine", excludeSubtrees("blades"));
    static final Part STEAM_TURBINE_BLADE = gltf("steam_turbine", includeSubtrees("blades"));
    private static final Part[] SHELVING_CRATES = shelvingCrates();

    private LegacySceneModels() {
    }

    static void render(
            Part part,
            String animationName,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        LegacySceneRenderer.render(
                part.source,
                part.selection,
                part.sourceTransform,
                animationName,
                animationSeconds,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
    }

    @Nullable
    static Matrix4f nodeTransform(
            Part part,
            String animationName,
            double animationSeconds,
            String nodeName
    ) {
        return LegacySceneRenderer.nodeTransform(
                part.source,
                part.sourceTransform,
                animationName,
                animationSeconds,
                nodeName
        );
    }

    static void render(
            Part part,
            String animationName,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            LegacySceneRenderer.RenderStyle style
    ) {
        LegacySceneRenderer.render(
                part.source,
                part.selection,
                part.sourceTransform,
                animationName,
                animationSeconds,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                style
        );
    }

    private static Part mcx(String name, ModelSceneSelection selection) {
        return new Part(source("mcx/" + name + ".mcx"), selection, ModelTransform.IDENTITY);
    }

    private static Part gltf(String name, ModelSceneSelection selection) {
        return new Part(source("gltf/" + name + ".gltf"), selection, ModelTransform.IDENTITY);
    }

    private static ResourceLocation source(String path) {
        return Magneticraft.id("models/block/" + path);
    }

    private static ModelSceneSelection includeNodes(String... names) {
        return new ModelSceneSelection(Set.of(names), Set.of(), Set.of(), Set.of());
    }

    private static ModelSceneSelection includeSubtrees(String... names) {
        return new ModelSceneSelection(Set.of(), Set.of(names), Set.of(), Set.of());
    }

    private static ModelSceneSelection excludeNodes(String... names) {
        return new ModelSceneSelection(Set.of(), Set.of(), Set.of(names), Set.of());
    }

    private static ModelSceneSelection excludeSubtrees(String... names) {
        return new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of(names));
    }

    static Part solarPanelParts(int... panels) {
        String[] names = new String[panels.length * 3];
        int index = 0;
        for (int panel : panels) {
            for (int segment = 1; segment <= 3; segment++) {
                names[index++] = "Panel" + panel + "-" + segment;
            }
        }
        return mcx("solar_panel", includeNodes(names));
    }

    static Part shelvingCrate(int crate) {
        if (crate < 1 || crate > 24) {
            throw new IllegalArgumentException("Shelving crate index must be in [1, 24]");
        }
        return SHELVING_CRATES[crate - 1];
    }

    private static Part[] shelvingCrates() {
        Part[] crates = new Part[24];
        for (int index = 0; index < crates.length; index++) {
            crates[index] = mcx("shelving_unit", includeNodes("Crate" + (index + 1)));
        }
        return crates;
    }

    private static String[] crateNames() {
        String[] names = new String[24];
        for (int index = 0; index < names.length; index++) {
            names[index] = "Crate" + (index + 1);
        }
        return names;
    }

    record Part(ResourceLocation source, ModelSceneSelection selection, ModelTransform sourceTransform) {
    }
}
