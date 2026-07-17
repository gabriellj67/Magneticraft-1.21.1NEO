package committee.nova.mods.magneticraft.integration.jade;

import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorAccidentStage;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/** Small versioned whitelist for nuclear state sent to Jade clients. */
final class NuclearJadeData {
    static final String ROOT_KEY = "magneticraft_nuclear";
    private static final int SCHEMA_VERSION = 1;

    private NuclearJadeData() {
    }

    static void write(CompoundTag target, BlockEntity entity) {
        if (!(entity instanceof RadiationSource source)) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putDouble("dose_rate", finiteNonNegative(source.doseRateMillisievertsPerHour()));
        tag.putBoolean("contamination_source", source.contaminationSource());
        if (entity instanceof NuclearReactorControllerBlockEntity reactor) {
            tag.putString("kind", "reactor");
            tag.putBoolean("formed", reactor.formed());
            tag.putInt("accident_stage", reactor.accidentStage().ordinal());
            tag.putDouble("pressure_mpa", finiteNonNegative(reactor.corePressureMegapascals()));
            tag.putDouble("vessel_integrity", fraction(reactor.vesselIntegrity()));
            tag.putDouble("containment_integrity", fraction(reactor.containmentIntegrity()));
        } else if (entity instanceof SpentFuelPoolControllerBlockEntity pool) {
            tag.putString("kind", "spent_fuel_pool");
            tag.putBoolean("formed", pool.formed());
        } else {
            tag.putString("kind", "source");
        }
        target.put(ROOT_KEY, tag);
    }

    static Optional<Reading> read(CompoundTag target) {
        if (!target.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = target.getCompound(ROOT_KEY);
        if (tag.getInt("schema_version") != SCHEMA_VERSION) {
            return Optional.empty();
        }
        String kind = tag.getString("kind");
        if (!kind.equals("reactor") && !kind.equals("spent_fuel_pool") && !kind.equals("source")) {
            return Optional.empty();
        }
        ReactorAccidentStage stage = null;
        if (kind.equals("reactor")) {
            int ordinal = tag.getInt("accident_stage");
            ReactorAccidentStage[] stages = ReactorAccidentStage.values();
            stage = ordinal >= 0 && ordinal < stages.length ? stages[ordinal] : ReactorAccidentStage.NORMAL;
        }
        return Optional.of(new Reading(
                kind,
                finiteNonNegative(tag.getDouble("dose_rate")),
                tag.getBoolean("contamination_source"),
                kind.equals("reactor") || kind.equals("spent_fuel_pool") ? tag.getBoolean("formed") : null,
                stage,
                finiteNonNegative(tag.getDouble("pressure_mpa")),
                fraction(tag.getDouble("vessel_integrity")),
                fraction(tag.getDouble("containment_integrity"))
        ));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0D ? value : 0.0D;
    }

    private static double fraction(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    record Reading(String kind, double doseRate, boolean contaminationSource, Boolean formed,
                   ReactorAccidentStage accidentStage, double pressureMegapascals,
                   double vesselIntegrity, double containmentIntegrity) {
    }
}
