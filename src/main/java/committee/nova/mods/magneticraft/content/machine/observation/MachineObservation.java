package committee.nova.mods.magneticraft.content.machine.observation;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded, read-only machine state that is safe to expose to optional HUD integrations.
 */
public record MachineObservation(
        Optional<ProcessStatus> process,
        Optional<EnergyStatus> energy,
        Optional<ElectricalStatus> electrical,
        Optional<ThermalStatus> thermal,
        List<TankStatus> tanks,
        Optional<StructureStatus> structure
) {
    public static final int MAX_TANKS = 5;

    public MachineObservation {
        process = Objects.requireNonNull(process);
        energy = Objects.requireNonNull(energy);
        electrical = Objects.requireNonNull(electrical);
        thermal = Objects.requireNonNull(thermal);
        tanks = List.copyOf(Objects.requireNonNull(tanks).stream().limit(MAX_TANKS).toList());
        structure = Objects.requireNonNull(structure);
    }

    public boolean isEmpty() {
        return process.isEmpty()
                && energy.isEmpty()
                && electrical.isEmpty()
                && thermal.isEmpty()
                && tanks.isEmpty()
                && structure.isEmpty();
    }

    public record ProcessStatus(int progress, int total, boolean working) {
        public ProcessStatus {
            total = Math.max(0, total);
            progress = Math.max(0, total == 0 ? progress : Math.min(progress, total));
        }
    }

    public record EnergyStatus(int stored, int capacity) {
        public EnergyStatus {
            capacity = Math.max(0, capacity);
            stored = Math.max(0, Math.min(stored, capacity));
        }
    }

    public record ElectricalStatus(double voltageVolts, double currentAmps, double powerWatts) {
        public ElectricalStatus {
            voltageVolts = finiteOrZero(voltageVolts);
            currentAmps = finiteOrZero(currentAmps);
            powerWatts = finiteOrZero(powerWatts);
        }
    }

    public record ThermalStatus(double temperatureKelvin) {
        public ThermalStatus {
            temperatureKelvin = finiteOrZero(temperatureKelvin);
        }
    }

    public record TankStatus(Optional<ResourceLocation> fluidId, int amount, int capacity) {
        public TankStatus {
            fluidId = Objects.requireNonNull(fluidId);
            capacity = Math.max(0, capacity);
            amount = Math.max(0, Math.min(amount, capacity));
        }
    }

    public record StructureStatus(boolean formed, boolean operational) {
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
