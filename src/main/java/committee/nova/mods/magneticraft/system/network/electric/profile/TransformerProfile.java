package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Immutable one-way transformer coupling contract.
 */
public record TransformerProfile(
        ResourceLocation id,
        ResourceLocation inputTierId,
        ResourceLocation outputTierId,
        double maximumTransferJoulesPerTick,
        double efficiency
) {
    public static final int SCHEMA_VERSION = 1;

    public TransformerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(inputTierId, "inputTierId");
        Objects.requireNonNull(outputTierId, "outputTierId");
        if (inputTierId.equals(outputTierId)) {
            throw new IllegalArgumentException("input_tier and output_tier must differ");
        }
        if (!Double.isFinite(maximumTransferJoulesPerTick) || maximumTransferJoulesPerTick <= 0.0D) {
            throw new IllegalArgumentException("maximum_transfer_joules_per_tick must be finite and positive");
        }
        if (!Double.isFinite(efficiency) || efficiency <= 0.0D || efficiency > 1.0D) {
            throw new IllegalArgumentException("efficiency must be finite and in (0, 1]");
        }
    }
}
