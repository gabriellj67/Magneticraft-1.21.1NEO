package committee.nova.mods.magneticraft.api.nuclear.radiation;

import net.minecraft.core.BlockPos;

/** Public contract for a durable, locatable radiation source. */
public interface RadiationSource {
    BlockPos radiationOrigin();

    /** Unshielded dose rate measured one block from the source. */
    double doseRateMillisievertsPerHour();

    /** True only for dispersible material that can contaminate players and terrain. */
    boolean contaminationSource();

    default double maximumRangeBlocks() {
        return 32.0D;
    }
}
