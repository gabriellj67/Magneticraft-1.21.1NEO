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
}
