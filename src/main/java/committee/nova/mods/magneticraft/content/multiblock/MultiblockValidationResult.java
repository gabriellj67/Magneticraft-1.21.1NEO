package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Stable server-side result for structure formation and periodic validation.
 */
public record MultiblockValidationResult(
        Status status,
        @Nullable BlockPos position,
        @Nullable MultiblockRule expected
) {
    public boolean valid() {
        return status == Status.VALID;
    }

    public static MultiblockValidationResult validResult() {
        return new MultiblockValidationResult(Status.VALID, null, null);
    }

    public static MultiblockValidationResult unloaded(BlockPos position, MultiblockRule expected) {
        return new MultiblockValidationResult(Status.UNLOADED, position.immutable(), expected);
    }

    public static MultiblockValidationResult mismatch(BlockPos position, MultiblockRule expected) {
        return new MultiblockValidationResult(Status.MISMATCH, position.immutable(), expected);
    }

    public enum Status {
        VALID,
        UNLOADED,
        MISMATCH
    }
}
