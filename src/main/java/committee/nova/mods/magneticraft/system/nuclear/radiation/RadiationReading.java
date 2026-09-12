package committee.nova.mods.magneticraft.system.nuclear.radiation;

/** A separated external dose-rate and contamination reading. */
public record RadiationReading(
        double doseRateMillisievertsPerHour,
        double contaminationRateMillisievertsPerHour
) {
    public static final RadiationReading ZERO = new RadiationReading(0.0D, 0.0D);

    public RadiationReading add(RadiationReading other) {
        return new RadiationReading(
                doseRateMillisievertsPerHour + other.doseRateMillisievertsPerHour,
                contaminationRateMillisievertsPerHour + other.contaminationRateMillisievertsPerHour);
    }
}
