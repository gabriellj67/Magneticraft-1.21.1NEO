package committee.nova.mods.magneticraft.system.network.electric.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Complete parse outcome; invalid candidates never expose a partial snapshot. */
public record ElectricalDataLoadResult(
        Optional<ElectricalDataSnapshot> snapshot,
        List<ElectricalDataValidationError> errors
) {
    public ElectricalDataLoadResult {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        if (errors.isEmpty() == snapshot.isEmpty()) {
            throw new IllegalArgumentException("a load result must contain either one snapshot or one-or-more errors");
        }
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
