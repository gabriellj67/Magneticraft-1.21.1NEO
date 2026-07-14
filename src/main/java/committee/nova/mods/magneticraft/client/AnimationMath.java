package committee.nova.mods.magneticraft.client;

/** Pure interpolation helpers shared by block-entity renderers. */
public final class AnimationMath {
    private AnimationMath() {
    }

    public static float boundedSnapshotAge(
            long currentGameTick,
            long snapshotGameTick,
            float partialTick,
            float maximumTicks,
            boolean moving
    ) {
        if (!moving || snapshotGameTick == Long.MIN_VALUE || maximumTicks <= 0.0F) {
            return 0.0F;
        }
        long elapsedTicks = Math.max(0L, currentGameTick - snapshotGameTick);
        float boundedPartialTick = Math.max(0.0F, Math.min(1.0F, partialTick));
        return Math.min(maximumTicks, elapsedTicks + boundedPartialTick);
    }

    public static int bakedFrameIndex(
            long currentGameTick,
            float partialTick,
            int frameCount,
            float ticksPerFrame,
            boolean moving
    ) {
        if (!moving || frameCount <= 1 || !Float.isFinite(ticksPerFrame) || ticksPerFrame <= 0.0F) {
            return 0;
        }
        double boundedPartialTick = Math.max(0.0D, Math.min(1.0D, partialTick));
        long absoluteFrame = (long) Math.floor((currentGameTick + boundedPartialTick) / ticksPerFrame);
        return Math.floorMod(absoluteFrame, frameCount);
    }
}
