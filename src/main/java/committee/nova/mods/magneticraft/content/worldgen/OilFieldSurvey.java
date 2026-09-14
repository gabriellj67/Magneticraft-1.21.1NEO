package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.core.BlockPos;

/** Immutable aggregate for one already-generated oil field. */
public record OilFieldSurvey(
        BlockPos origin,
        int minimumY,
        int maximumY,
        long remainingMillibuckets,
        long knownCapacityMillibuckets,
        int depositCount
) {
    public OilFieldSurvey {
        origin = origin.immutable();
        if (minimumY > maximumY
                || remainingMillibuckets < 0L
                || knownCapacityMillibuckets < remainingMillibuckets
                || depositCount <= 0) {
            throw new IllegalArgumentException("Invalid oil-field survey");
        }
    }

    public int remainingPercent() {
        if (knownCapacityMillibuckets <= 0L) {
            return 0;
        }
        return (int) Math.round(remainingMillibuckets * 100.0D / knownCapacityMillibuckets);
    }

    public boolean depleted() {
        return remainingMillibuckets == 0L;
    }
}
