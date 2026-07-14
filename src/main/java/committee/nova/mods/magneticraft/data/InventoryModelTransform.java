package committee.nova.mods.magneticraft.data;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Computes a bounded GUI transform from a parsed model scene's world-space bounds. */
record InventoryModelTransform(float translationX, float translationY, float translationZ, float scale) {
    static final float GUI_ROTATION_X = 30.0F;
    static final float GUI_ROTATION_Y = 225.0F;
    private static final float VANILLA_BLOCK_GUI_SCALE = 0.625F;
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

        float scale = (float) Math.min(MAX_ITEM_SCALE, VANILLA_BLOCK_GUI_SCALE / largestSpan);
        Vector3f centerOffset = new Vector3f(
                (float) ((minX + maxX) * 0.5D - 0.5D),
                (float) ((minY + maxY) * 0.5D - 0.5D),
                (float) ((minZ + maxZ) * 0.5D - 0.5D)
        ).mul(scale);
        new Quaternionf().rotationXYZ(
                (float) Math.toRadians(GUI_ROTATION_X),
                (float) Math.toRadians(GUI_ROTATION_Y),
                0.0F
        ).transform(centerOffset);

        float translationX = normalizeZero(-centerOffset.x() * 16.0F);
        float translationY = normalizeZero(-centerOffset.y() * 16.0F);
        float translationZ = normalizeZero(-centerOffset.z() * 16.0F);
        if (Math.abs(translationX) > MAX_ITEM_TRANSLATION
                || Math.abs(translationY) > MAX_ITEM_TRANSLATION
                || Math.abs(translationZ) > MAX_ITEM_TRANSLATION) {
            throw new IllegalArgumentException("Model requires an unsupported item translation");
        }
        return new InventoryModelTransform(translationX, translationY, translationZ, scale);
    }

    private static float normalizeZero(float value) {
        return Math.abs(value) < ZERO_EPSILON ? 0.0F : value;
    }
}
