package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelSelectionTest {
    @Test
    void branchFilterIncludesEntireMatchingSubtreeLikeOriginalModelCacheFactory() {
        ModelScene scene = scene();

        assertEquals(
                Set.of(0, 1, 2),
                ModelSelection.nodes(scene, ModelSelection.exact("moving", ModelSelection.Target.BRANCH))
        );
        assertEquals(
                Set.of(0, 1),
                ModelSelection.nodes(scene, ModelSelection.exact("piston"))
        );
    }

    @Test
    void animationFilterUsesAnimationTargetOnly() {
        ModelScene scene = scene();

        assertEquals(
                "working",
                ModelSelection.animation(
                        scene,
                        ModelSelection.regex("working", ModelSelection.Target.ANIMATION)
                ).name()
        );
        assertNull(ModelSelection.animation(scene, ModelSelection.ignoreAnimation()));
    }

    private static ModelScene scene() {
        ModelScene.Primitive primitive = new ModelScene.Primitive(
                "magneticraft:test",
                null,
                new float[12],
                new float[8],
                new float[12]
        );
        return new ModelScene(
                ModelScene.Format.GLTF,
                true,
                true,
                "magneticraft:test",
                List.of(0),
                List.of(
                        new ModelScene.Node(0, "moving", ModelTransform.IDENTITY, List.of(1, 2), List.of()),
                        new ModelScene.Node(1, "piston", ModelTransform.IDENTITY, List.of(), List.of(primitive)),
                        new ModelScene.Node(2, "rod", ModelTransform.IDENTITY, List.of(), List.of(primitive))
                ),
                List.of(new ModelScene.Animation("working", 1.0F, List.of()))
        );
    }
}
