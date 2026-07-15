package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Bounded, non-authoritative projection safe to synchronize to clients.
 */
public record VoltageTierDisplay(
        ResourceLocation id,
        String translationKey,
        double minimumOperatingVoltage,
        double nominalVoltage,
        double maximumVoltage,
        int colorRgb
) {
    public VoltageTierDisplay {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        if (id.toString().length() > VoltageTier.MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("tier id exceeds " + VoltageTier.MAX_TEXT_LENGTH + " characters");
        }
        if (translationKey.isBlank() || translationKey.length() > VoltageTier.MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("invalid tier translation key");
        }
        if (!Double.isFinite(minimumOperatingVoltage)
                || !Double.isFinite(nominalVoltage)
                || !Double.isFinite(maximumVoltage)
                || minimumOperatingVoltage <= 0.0D
                || minimumOperatingVoltage >= nominalVoltage
                || nominalVoltage >= maximumVoltage) {
            throw new IllegalArgumentException("invalid tier display voltages");
        }
        if (colorRgb < 0 || colorRgb > 0xFF_FFFF) {
            throw new IllegalArgumentException("invalid tier display color");
        }
    }

    public static VoltageTierDisplay from(VoltageTier tier) {
        return new VoltageTierDisplay(
                tier.id(),
                tier.translationKey(),
                tier.minimumOperatingVoltage(),
                tier.nominalVoltage(),
                tier.maximumVoltage(),
                tier.colorRgb()
        );
    }
}
