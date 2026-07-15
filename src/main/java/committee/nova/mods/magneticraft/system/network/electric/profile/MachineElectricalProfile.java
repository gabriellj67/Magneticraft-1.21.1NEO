package committee.nova.mods.magneticraft.system.network.electric.profile;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Server-authoritative electrical behavior assigned to a machine registry id.
 *
 * <p>The legacy JSON key {@code buffer_capacity_joules} now defines the rated energy of the
 * machine's authoritative electrical node. It is retained as a data-pack compatibility key;
 * no second machine buffer is implied.</p>
 */
public record MachineElectricalProfile(
        ResourceLocation id,
        ResourceLocation tierId,
        ElectricalRole role,
        double bufferCapacityJoules,
        double maximumTransferJoulesPerTick,
        double terminalRatedChargePerTick
) {
    public static final int SCHEMA_VERSION = 1;

    public MachineElectricalProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tierId, "tierId");
        Objects.requireNonNull(role, "role");
        positiveFinite("buffer_capacity_joules", bufferCapacityJoules);
        positiveFinite("maximum_transfer_joules_per_tick", maximumTransferJoulesPerTick);
        positiveFinite("terminal_rated_charge_per_tick", terminalRatedChargePerTick);
    }

    /** Capacitance whose stored energy at the tier maximum voltage equals this profile capacity. */
    public double nodeCapacitanceFarads(VoltageTier tier) {
        return capacitanceForRatedEnergy(bufferCapacityJoules, tier);
    }

    public static double capacitanceForRatedEnergy(double capacityJoules, VoltageTier tier) {
        Objects.requireNonNull(tier, "tier");
        positiveFinite("rated_capacity_joules", capacityJoules);
        double maximumVoltage = tier.maximumVoltage();
        double capacitance = 2.0D * capacityJoules / (maximumVoltage * maximumVoltage);
        positiveFinite("node_capacitance_farads", capacitance);
        return capacitance;
    }

    private static void positiveFinite(String field, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(field + " must be finite and positive");
        }
    }
}
