package committee.nova.mods.magneticraft.api.nuclear.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/** Public validation boundary for controller-anchored variable-volume nuclear facilities. */
public interface VariableNuclearStructureValidator<S, P> {
    VariableNuclearStructureDescriptor descriptor();

    ValidationResult<S> validate(BlockPos controller, Direction facing, PartLookup<P> lookup);

    @FunctionalInterface
    interface PartLookup<P> {
        @Nullable
        P partAt(BlockPos position);

        default boolean isLoaded(BlockPos position) {
            return true;
        }
    }

    record ValidationResult<S>(Optional<S> snapshot, String reason, BlockPos position) {
        public ValidationResult {
            snapshot = Objects.requireNonNull(snapshot);
            reason = Objects.requireNonNull(reason);
            position = position.immutable();
        }
    }
}
