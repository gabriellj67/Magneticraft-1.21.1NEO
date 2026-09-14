package committee.nova.mods.magneticraft.content.nuclear.fuel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Immutable durable state transferred between an item and one reactor column. */
public record FuelAssemblyState(
        ResourceLocation fuelId,
        double burnupFraction,
        double poisonFraction,
        double decayHeatJoules,
        double temperatureKelvin,
        double claddingIntegrity,
        long lastUpdateGameTime
) {
    public static final int SCHEMA_VERSION = 1;
    public static final double AMBIENT_TEMPERATURE_KELVIN = 293.15D;

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String FUEL_ID_KEY = "fuel_id";
    private static final String BURNUP_KEY = "burnup_fraction";
    private static final String POISON_KEY = "poison_fraction";
    private static final String DECAY_HEAT_KEY = "decay_heat_joules";
    private static final String TEMPERATURE_KEY = "temperature_kelvin";
    private static final String CLADDING_KEY = "cladding_integrity";
    private static final String LAST_UPDATE_KEY = "last_update_game_time";

    public FuelAssemblyState {
        if (fuelId == null) {
            throw new IllegalArgumentException("fuelId must not be null");
        }
        requireFraction("burnupFraction", burnupFraction);
        requireFraction("poisonFraction", poisonFraction);
        requireNonNegativeFinite("decayHeatJoules", decayHeatJoules);
        requireNonNegativeFinite("temperatureKelvin", temperatureKelvin);
        requireFraction("claddingIntegrity", claddingIntegrity);
    }

    public static FuelAssemblyState fresh(NuclearFuelGrade grade, long gameTime) {
        return new FuelAssemblyState(
                grade.definitionId(),
                0.0D,
                0.0D,
                0.0D,
                AMBIENT_TEMPERATURE_KELVIN,
                1.0D,
                Math.max(0L, gameTime)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        tag.putString(FUEL_ID_KEY, fuelId.toString());
        tag.putDouble(BURNUP_KEY, burnupFraction);
        tag.putDouble(POISON_KEY, poisonFraction);
        tag.putDouble(DECAY_HEAT_KEY, decayHeatJoules);
        tag.putDouble(TEMPERATURE_KEY, temperatureKelvin);
        tag.putDouble(CLADDING_KEY, claddingIntegrity);
        tag.putLong(LAST_UPDATE_KEY, lastUpdateGameTime);
        return tag;
    }

    public static Optional<FuelAssemblyState> load(CompoundTag tag) {
        if (tag == null
                || !tag.contains(SCHEMA_VERSION_KEY, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_KEY) != SCHEMA_VERSION
                || !tag.contains(FUEL_ID_KEY, Tag.TAG_STRING)
                || !tag.contains(BURNUP_KEY, Tag.TAG_DOUBLE)
                || !tag.contains(POISON_KEY, Tag.TAG_DOUBLE)
                || !tag.contains(DECAY_HEAT_KEY, Tag.TAG_DOUBLE)
                || !tag.contains(TEMPERATURE_KEY, Tag.TAG_DOUBLE)
                || !tag.contains(CLADDING_KEY, Tag.TAG_DOUBLE)
                || !tag.contains(LAST_UPDATE_KEY, Tag.TAG_LONG)) {
            return Optional.empty();
        }
        ResourceLocation fuelId = ResourceLocation.tryParse(tag.getString(FUEL_ID_KEY));
        if (fuelId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new FuelAssemblyState(
                    fuelId,
                    tag.getDouble(BURNUP_KEY),
                    tag.getDouble(POISON_KEY),
                    tag.getDouble(DECAY_HEAT_KEY),
                    tag.getDouble(TEMPERATURE_KEY),
                    tag.getDouble(CLADDING_KEY),
                    Math.max(0L, tag.getLong(LAST_UPDATE_KEY))
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void requireFraction(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(field + " must be finite and in [0, 1]");
        }
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(field + " must be finite and non-negative");
        }
    }
}
