package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDynamicSceneContractTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/magneticraft/models/block");

    @Test
    void mcxDynamicPartNamesMatchTheReleasedModels() throws Exception {
        assertMcxParts("computer", Set.of("screen"));
        assertMcxParts("mining_robot", Set.of("prop1", "prop2"));
        assertMcxParts("mining_robot", Set.of("drill1", "drill2", "drill3", "drill4", "drill5"));

        ModelScene turbine = parseMcx("wind_turbine");
        Set<Integer> rotor = new ModelSceneSelection(
                Set.of(),
                Set.of(),
                Set.of("Shape2"),
                Set.of()
        ).select(turbine);
        assertFalse(rotor.isEmpty());
        assertEquals(turbine.nodes().size() - 1, rotor.size());
    }

    @Test
    void gltfDynamicSubtreesAndAnimationNamesMatchTheReleasedModels() throws Exception {
        ModelScene inserter = parseGltf("inserter");
        assertTrue(inserter.animation("animation0").isPresent());
        assertEquals(10, inserter.animations().size());
        assertSelectionHasGeometry(
                inserter,
                new ModelSceneSelection(Set.of(), Set.of("level1"), Set.of(), Set.of())
        );

        ModelScene engine = parseGltf("electric_engine");
        assertTrue(engine.animation("animation").isPresent());
        assertSelectionHasGeometry(
                engine,
                new ModelSceneSelection(Set.of(), Set.of("Group 18", "piston"), Set.of(), Set.of())
        );

        for (String name : Set.of("grinder", "hydraulic_press", "sieve", "steam_engine")) {
            assertTrue(parseGltf(name).animation("animation").isPresent(), name);
        }
    }

    @Test
    void everyAdvancedMultiblockUsesItsReleasedRawScene() throws Exception {
        Map<String, String> mcx = Map.of(
                "shipping_container", "container",
                "oil_heater", "oil_heater",
                "pumpjack", "pumpjack",
                "refinery", "refinery",
                "shelving_unit", "shelving_unit",
                "solar_mirror", "solar_mirror",
                "solar_panel", "solar_panel",
                "solar_tower", "solar_tower"
        );
        Map<String, String> gltf = Map.of(
                "industrial_combustion_chamber", "big_combustion_chamber",
                "industrial_electric_furnace", "big_electric_furnace",
                "industrial_steam_boiler", "big_steam_boiler",
                "grinder", "grinder",
                "hydraulic_press", "hydraulic_press",
                "sieve", "sieve",
                "steam_engine", "steam_engine",
                "steam_turbine", "steam_turbine"
        );
        assertEquals(16, mcx.size() + gltf.size());
        for (Map.Entry<String, String> entry : mcx.entrySet()) {
            assertSceneHasGeometry(entry.getKey(), parseMcx(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : gltf.entrySet()) {
            assertSceneHasGeometry(entry.getKey(), parseGltf(entry.getValue()));
        }
    }

    @Test
    void releasedNamedPartsPartitionBodiesWithoutDuplicateGeometry() throws Exception {
        assertPartition(
                parseGltf("big_combustion_chamber"),
                new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of("fire_on", "fire_off")),
                new ModelSceneSelection(Set.of(), Set.of("fire_on", "fire_off"), Set.of(), Set.of())
        );
        assertPartition(
                parseGltf("steam_turbine"),
                new ModelSceneSelection(Set.of(), Set.of(), Set.of(), Set.of("blades")),
                new ModelSceneSelection(Set.of(), Set.of("blades"), Set.of(), Set.of())
        );
        Set<String> panelParts = new HashSet<>();
        for (int panel = 1; panel <= 6; panel++) {
            for (int segment = 1; segment <= 3; segment++) {
                panelParts.add("Panel" + panel + "-" + segment);
            }
        }
        assertPartition(
                parseMcx("solar_panel"),
                new ModelSceneSelection(Set.of(), Set.of(), panelParts, Set.of()),
                new ModelSceneSelection(panelParts, Set.of(), Set.of(), Set.of())
        );
    }

    private static void assertMcxParts(String name, Set<String> parts) throws Exception {
        ModelScene scene = parseMcx(name);
        Set<Integer> selected = new ModelSceneSelection(parts, Set.of(), Set.of(), Set.of()).select(scene);
        assertEquals(parts.size(), selected.size());
        assertTrue(selected.stream().allMatch(index -> !scene.node(index).primitives().isEmpty()));
    }

    private static void assertSelectionHasGeometry(ModelScene scene, ModelSceneSelection selection) {
        Set<Integer> selected = selection.select(scene);
        assertFalse(selected.isEmpty());
        assertTrue(selected.stream().anyMatch(index -> !scene.node(index).primitives().isEmpty()));
    }

    private static void assertSceneHasGeometry(String name, ModelScene scene) {
        assertTrue(
                scene.nodes().stream().anyMatch(node -> !node.primitives().isEmpty()),
                name
        );
    }

    private static void assertPartition(
            ModelScene scene,
            ModelSceneSelection body,
            ModelSceneSelection moving
    ) {
        Set<Integer> bodyNodes = body.select(scene);
        Set<Integer> movingNodes = moving.select(scene);
        assertTrue(bodyNodes.stream().noneMatch(movingNodes::contains));
        Set<Integer> combined = new HashSet<>(bodyNodes);
        combined.addAll(movingNodes);
        assertEquals(ModelSceneSelection.ALL.select(scene), combined);
        assertSelectionHasGeometry(scene, body);
        assertSelectionHasGeometry(scene, moving);
    }

    private static ModelScene parseMcx(String name) throws Exception {
        Path source = ROOT.resolve("mcx/" + name + ".mcx");
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return McxModelParser.parse(source.toString(), reader);
        }
    }

    private static ModelScene parseGltf(String name) throws Exception {
        Path source = ROOT.resolve("gltf/" + name + ".gltf");
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return GltfModelParser.parse(
                    source.toString(),
                    reader,
                    uri -> Files.newInputStream(source.getParent().resolve(uri))
            );
        }
    }
}
