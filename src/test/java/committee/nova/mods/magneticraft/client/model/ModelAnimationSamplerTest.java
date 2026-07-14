package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelAnimationSamplerTest {
    @Test
    void samplesLinearTranslationContinuouslyAndLoopsByDuration() {
        ModelScene.Channel channel = new ModelScene.Channel(
                0,
                ModelScene.Path.TRANSLATION,
                ModelScene.Interpolation.LINEAR,
                new float[]{0.0F, 2.0F},
                new float[]{0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 0.0F},
                3
        );
        ModelScene scene = scene(channel, 2.0F);

        Map<Integer, ModelTransform> sampled = ModelAnimationSampler.sample(scene, scene.animations().get(0), 2.5D);

        assertEquals(1.0F, sampled.get(0).translationX(), 0.0001F);
        assertEquals(0.5F, sampled.get(0).translationY(), 0.0001F);
    }

    @Test
    void stepInterpolationHoldsPreviousValue() {
        ModelScene.Channel channel = new ModelScene.Channel(
                0,
                ModelScene.Path.SCALE,
                ModelScene.Interpolation.STEP,
                new float[]{0.0F, 1.0F},
                new float[]{1.0F, 1.0F, 1.0F, 2.0F, 2.0F, 2.0F},
                3
        );
        ModelScene scene = scene(channel, 1.0F);

        ModelTransform sampled = ModelAnimationSampler.sample(scene, scene.animations().get(0), 0.75D).get(0);

        assertEquals(1.0F, sampled.scaleX());
        assertEquals(1.0F, sampled.scaleY());
        assertEquals(1.0F, sampled.scaleZ());
    }

    private static ModelScene scene(ModelScene.Channel channel, float duration) {
        return new ModelScene(
                ModelScene.Format.GLTF,
                true,
                true,
                "minecraft:missingno",
                List.of(0),
                List.of(new ModelScene.Node(0, "animated", ModelTransform.IDENTITY, List.of(), List.of())),
                List.of(new ModelScene.Animation("animation", duration, List.of(channel)))
        );
    }
}
