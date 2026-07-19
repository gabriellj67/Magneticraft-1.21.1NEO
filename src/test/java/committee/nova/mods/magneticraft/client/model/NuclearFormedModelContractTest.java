package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearFormedModelContractTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/assets/magneticraft/models/block/gltf"
    );

    @Test
    void reactorSceneIsACompleteNormalizedAssembledModel() throws Exception {
        ModelScene scene = parse("pressurized_water_reactor_formed");
        assertNormalized(scene);
        assertEquals(ModelScene.AlphaMode.OPAQUE, scene.alphaMode());
        assertContainsNodes(scene, Set.of(
                "containment_body",
                "operator_console",
                "coolant_input_manifold",
                "coolant_output_manifold",
                "actuator_b2"
        ));
    }

    @Test
    void spentFuelPoolSceneKeepsAnOpenBasinAndVisibleServiceParts() throws Exception {
        ModelScene scene = parse("spent_fuel_pool_formed");
        assertNormalized(scene);
        assertEquals(ModelScene.AlphaMode.OPAQUE, scene.alphaMode());
        assertContainsNodes(scene, Set.of(
                "pool_floor",
                "front_wall",
                "rear_wall",
                "pool_controller",
                "cooling_port",
                "ladder_step_3"
        ));
    }

    private static void assertNormalized(ModelScene scene) {
        ModelSceneBounds bounds = ModelSceneBounds.calculate(
                scene, ModelSceneSelection.ALL.select(scene)
        );
        assertTrue(bounds.minX() >= -0.05F && bounds.minY() >= -0.05F && bounds.minZ() >= -0.05F);
        assertTrue(bounds.maxX() <= 1.05F && bounds.maxY() <= 1.05F && bounds.maxZ() <= 1.05F);
        assertTrue(bounds.maxX() - bounds.minX() >= 0.95F);
        assertTrue(bounds.maxY() - bounds.minY() >= 0.90F);
        assertTrue(bounds.maxZ() - bounds.minZ() >= 0.95F);
    }

    private static void assertContainsNodes(ModelScene scene, Set<String> expected) {
        Set<String> names = scene.nodes().stream()
                .map(ModelScene.Node::name)
                .collect(Collectors.toSet());
        assertTrue(names.containsAll(expected), () -> "Missing nodes: " + expected.stream()
                .filter(name -> !names.contains(name)).toList());
    }

    private static ModelScene parse(String name) throws Exception {
        Path source = ROOT.resolve(name + ".gltf");
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            return GltfModelParser.parse(
                    source.toString(), reader, uri -> Files.newInputStream(source.getParent().resolve(uri))
            );
        }
    }
}
