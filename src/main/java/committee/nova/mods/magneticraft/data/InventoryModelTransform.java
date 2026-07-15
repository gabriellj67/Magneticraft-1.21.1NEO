package committee.nova.mods.magneticraft.data;

import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Computes bounded item-display transforms from a parsed model scene's world-space bounds. */
record InventoryModelTransform(float centerX, float centerY, float centerZ, float largestSpan) {
    private static final float MAX_ITEM_SCALE = 4.0F;
    private static final float MAX_ITEM_TRANSLATION = 80.0F;
    private static final float ZERO_EPSILON = 0.000001F;

    static InventoryModelTransform fromBounds(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double largestSpan = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        if (!Double.isFinite(largestSpan) || largestSpan <= 0.0D) {
            throw new IllegalArgumentException("Model has no finite extent");
        }

        double centerX = (minX + maxX) * 0.5D;
        double centerY = (minY + maxY) * 0.5D;
        double centerZ = (minZ + maxZ) * 0.5D;
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("Model has no finite center");
        }
        float encodedCenterX = (float) centerX;
        float encodedCenterY = (float) centerY;
        float encodedCenterZ = (float) centerZ;
        float encodedSpan = (float) largestSpan;
        if (!Float.isFinite(encodedCenterX)
                || !Float.isFinite(encodedCenterY)
                || !Float.isFinite(encodedCenterZ)
                || !Float.isFinite(encodedSpan)) {
            throw new IllegalArgumentException("Model bounds exceed the supported item transform range");
        }
        return new InventoryModelTransform(
                encodedCenterX,
                encodedCenterY,
                encodedCenterZ,
                encodedSpan
        );
    }

    /**
     * Mirrors ModelLoader 2.0.0's BLOCK_DEFAULT camera transforms, then fits the selected scene
     * into the unit-block volume expected by those transforms.
     */
    DisplayTransform forContext(ItemDisplayContext context) {
        return switch (context) {
            case THIRD_PERSON_LEFT_HAND -> fit(75.0F, 225.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F);
            case THIRD_PERSON_RIGHT_HAND -> fit(75.0F, 45.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F);
            case FIRST_PERSON_LEFT_HAND -> fit(0.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
            case FIRST_PERSON_RIGHT_HAND -> fit(0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
            case HEAD -> fit(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            case GUI -> fit(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
            case GROUND -> fit(0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.25F);
            case FIXED -> fit(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
            case NONE -> throw new IllegalArgumentException("NONE has no item display transform");
        };
    }

    private DisplayTransform fit(
            float rotationX,
            float rotationY,
            float rotationZ,
            float baseTranslationX,
            float baseTranslationY,
            float baseTranslationZ,
            float baseScale
    ) {
        float scale = Math.min(MAX_ITEM_SCALE, baseScale / largestSpan);
        Vector3f centerOffset = new Vector3f(
                centerX - 0.5F,
                centerY - 0.5F,
                centerZ - 0.5F
        ).mul(scale);
        new Quaternionf().rotationXYZ(
                (float) Math.toRadians(rotationX),
                (float) Math.toRadians(rotationY),
                (float) Math.toRadians(rotationZ)
        ).transform(centerOffset);

        float translationX = normalizeZero(baseTranslationX - centerOffset.x() * 16.0F);
        float translationY = normalizeZero(baseTranslationY - centerOffset.y() * 16.0F);
        float translationZ = normalizeZero(baseTranslationZ - centerOffset.z() * 16.0F);
        if (Math.abs(translationX) > MAX_ITEM_TRANSLATION
                || Math.abs(translationY) > MAX_ITEM_TRANSLATION
                || Math.abs(translationZ) > MAX_ITEM_TRANSLATION) {
            throw new IllegalArgumentException("Model requires an unsupported item translation");
        }
        return new DisplayTransform(
                rotationX,
                rotationY,
                rotationZ,
                translationX,
                translationY,
                translationZ,
                scale
        );
    }

    private static float normalizeZero(float value) {
        return Math.abs(value) < ZERO_EPSILON ? 0.0F : value;
    }

    record DisplayTransform(
            float rotationX,
            float rotationY,
            float rotationZ,
            float translationX,
            float translationY,
            float translationZ,
            float scale
    ) {
    }
}
