package committee.nova.mods.magneticraft.client.model;

import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/** Continuous glTF TRS animation sampler. Input time is measured in seconds. */
public final class ModelAnimationSampler {
    private ModelAnimationSampler() {
    }

    public static Map<Integer, ModelTransform> sample(
            ModelScene scene,
            ModelScene.Animation animation,
            double seconds
    ) {
        if (animation == null) {
            return Map.of();
        }
        float localTime = localTime(animation.durationSeconds(), seconds);
        Map<Integer, ModelTransform> result = new HashMap<>();
        for (ModelScene.Channel channel : animation.channels()) {
            ModelTransform base = result.getOrDefault(channel.node(), scene.node(channel.node()).transform());
            float[] value = sample(channel, localTime);
            ModelTransform transformed = switch (channel.path()) {
                case TRANSLATION -> base.withTranslation(value[0], value[1], value[2]);
                case ROTATION -> base.withRotation(value[0], value[1], value[2], value[3]);
                case SCALE -> base.withScale(value[0], value[1], value[2]);
            };
            result.put(channel.node(), transformed);
        }
        return Map.copyOf(result);
    }

    private static float localTime(float duration, double seconds) {
        if (duration <= 0.0F || !Double.isFinite(seconds)) {
            return 0.0F;
        }
        double wrapped = seconds % duration;
        if (wrapped < 0.0D) {
            wrapped += duration;
        }
        return (float) wrapped;
    }

    static float[] sample(ModelScene.Channel channel, float time) {
        float[] times = channel.timesView();
        float[] values = channel.valuesView();
        int width = channel.valueWidth();
        if (times.length == 1 || time <= times[0]) {
            return key(values, width, 0);
        }
        int last = times.length - 1;
        if (time >= times[last]) {
            return key(values, width, last);
        }
        int next = 1;
        while (next < times.length && times[next] <= time) {
            next++;
        }
        int previous = next - 1;
        if (channel.interpolation() == ModelScene.Interpolation.STEP) {
            return key(values, width, previous);
        }
        float progress = (time - times[previous]) / (times[next] - times[previous]);
        if (channel.path() == ModelScene.Path.ROTATION) {
            Quaternionf from = quaternion(values, previous * width);
            Quaternionf to = quaternion(values, next * width);
            Quaternionf result = from.slerp(to, progress, new Quaternionf()).normalize();
            return new float[]{result.x, result.y, result.z, result.w};
        }
        float[] result = new float[width];
        int previousOffset = previous * width;
        int nextOffset = next * width;
        for (int component = 0; component < width; component++) {
            result[component] = values[previousOffset + component]
                    + (values[nextOffset + component] - values[previousOffset + component]) * progress;
        }
        return result;
    }

    private static Quaternionf quaternion(float[] values, int offset) {
        return new Quaternionf(values[offset], values[offset + 1], values[offset + 2], values[offset + 3]).normalize();
    }

    private static float[] key(float[] values, int width, int index) {
        float[] result = new float[width];
        System.arraycopy(values, index * width, result, 0, width);
        return result;
    }
}
