package committee.nova.mods.magneticraft.client.model;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Immutable translation/rotation/scale transform used by legacy model scenes. */
public record ModelTransform(
        float translationX,
        float translationY,
        float translationZ,
        float rotationX,
        float rotationY,
        float rotationZ,
        float rotationW,
        float scaleX,
        float scaleY,
        float scaleZ
) {
    public static final ModelTransform IDENTITY = new ModelTransform(
            0.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 0.0F, 1.0F,
            1.0F, 1.0F, 1.0F
    );

    public ModelTransform {
        float lengthSquared = rotationX * rotationX + rotationY * rotationY
                + rotationZ * rotationZ + rotationW * rotationW;
        if (!Float.isFinite(lengthSquared) || lengthSquared == 0.0F) {
            throw new IllegalArgumentException("Rotation quaternion must be finite and non-zero");
        }
        float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
        rotationX *= inverseLength;
        rotationY *= inverseLength;
        rotationZ *= inverseLength;
        rotationW *= inverseLength;
        if (!allFinite(translationX, translationY, translationZ, scaleX, scaleY, scaleZ)) {
            throw new IllegalArgumentException("Transform components must be finite");
        }
    }

    public static ModelTransform of(float[] translation, float[] rotation, float[] scale) {
        return new ModelTransform(
                translation[0], translation[1], translation[2],
                rotation[0], rotation[1], rotation[2], rotation[3],
                scale[0], scale[1], scale[2]
        );
    }

    public static ModelTransform fromMatrix(float[] values) {
        if (values.length != 16) {
            throw new IllegalArgumentException("A glTF node matrix must contain 16 values");
        }
        Matrix4f matrix = new Matrix4f().set(values);
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f scale = matrix.getScale(new Vector3f());
        return new ModelTransform(
                translation.x, translation.y, translation.z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scale.x, scale.y, scale.z
        );
    }

    public Matrix4f matrix() {
        return new Matrix4f().translationRotateScale(
                translationX,
                translationY,
                translationZ,
                rotationX,
                rotationY,
                rotationZ,
                rotationW,
                scaleX,
                scaleY,
                scaleZ
        );
    }

    public ModelTransform withTranslation(float x, float y, float z) {
        return new ModelTransform(
                x, y, z,
                rotationX, rotationY, rotationZ, rotationW,
                scaleX, scaleY, scaleZ
        );
    }

    public ModelTransform withRotation(float x, float y, float z, float w) {
        Quaternionf rotation = new Quaternionf(x, y, z, w).normalize();
        return new ModelTransform(
                translationX, translationY, translationZ,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scaleX, scaleY, scaleZ
        );
    }

    public ModelTransform withScale(float x, float y, float z) {
        return new ModelTransform(
                translationX, translationY, translationZ,
                rotationX, rotationY, rotationZ, rotationW,
                x, y, z
        );
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
