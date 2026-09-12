package committee.nova.mods.magneticraft.system.nuclear.radiation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** Schema-versioned persistent player exposure; radiation dose and contamination remain separate. */
public record RadiationExposure(
        double cumulativeDoseMillisieverts,
        double contaminationMillisieverts,
        double lastDoseRateMillisievertsPerHour,
        double lastContaminationRateMillisievertsPerHour
) {
    public static final int SCHEMA_VERSION = 1;
    public static final RadiationExposure ZERO = new RadiationExposure(0, 0, 0, 0);

    public RadiationExposure {
        require(cumulativeDoseMillisieverts);
        require(contaminationMillisieverts);
        require(lastDoseRateMillisievertsPerHour);
        require(lastContaminationRateMillisievertsPerHour);
    }

    public RadiationExposure accumulate(RadiationReading reading, double hours, double protection) {
        double factor = Math.max(0.0D, Math.min(1.0D, 1.0D - protection));
        return new RadiationExposure(
                cumulativeDoseMillisieverts + reading.doseRateMillisievertsPerHour() * hours * factor,
                contaminationMillisieverts + reading.contaminationRateMillisievertsPerHour() * hours * factor,
                reading.doseRateMillisievertsPerHour() * factor,
                reading.contaminationRateMillisievertsPerHour() * factor);
    }

    public RadiationExposure decontaminated() {
        return new RadiationExposure(cumulativeDoseMillisieverts, 0.0D,
                lastDoseRateMillisievertsPerHour, 0.0D);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putDouble("cumulative_dose_msv", cumulativeDoseMillisieverts);
        tag.putDouble("contamination_msv", contaminationMillisieverts);
        tag.putDouble("last_dose_rate_msv_h", lastDoseRateMillisievertsPerHour);
        tag.putDouble("last_contamination_rate_msv_h", lastContaminationRateMillisievertsPerHour);
        return tag;
    }

    public static Optional<RadiationExposure> load(CompoundTag tag) {
        if (tag == null || tag.getInt("schema_version") != SCHEMA_VERSION
                || !tag.contains("cumulative_dose_msv", Tag.TAG_DOUBLE)
                || !tag.contains("contamination_msv", Tag.TAG_DOUBLE)
                || !tag.contains("last_dose_rate_msv_h", Tag.TAG_DOUBLE)
                || !tag.contains("last_contamination_rate_msv_h", Tag.TAG_DOUBLE)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RadiationExposure(
                    tag.getDouble("cumulative_dose_msv"), tag.getDouble("contamination_msv"),
                    tag.getDouble("last_dose_rate_msv_h"), tag.getDouble("last_contamination_rate_msv_h")));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void require(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("Radiation exposure values must be finite and non-negative");
        }
    }
}
