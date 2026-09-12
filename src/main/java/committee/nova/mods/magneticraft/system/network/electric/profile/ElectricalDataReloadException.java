package committee.nova.mods.magneticraft.system.network.electric.profile;

import java.util.List;

/** Raised after a complete candidate has been rejected without replacing the live snapshot. */
public final class ElectricalDataReloadException extends RuntimeException {
    private final List<ElectricalDataValidationError> errors;

    public ElectricalDataReloadException(List<ElectricalDataValidationError> errors) {
        super("Rejected electrical data snapshot with " + errors.size() + " validation error(s)");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("reload exceptions require at least one validation error");
        }
        this.errors = List.copyOf(errors);
    }

    public List<ElectricalDataValidationError> errors() {
        return errors;
    }
}
