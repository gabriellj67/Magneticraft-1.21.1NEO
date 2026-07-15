package committee.nova.mods.magneticraft.content.machine.observation;

import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
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

    public record ElectricalStatus(
            ResourceLocation tierId,
            ResourceLocation terminalId,
            double voltageVolts,
            double chargeCoulombsPerTick,
            double currentAmps,
            double joulesPerTick,
            double powerWatts,
            double storedJoules,
            double capacityJoules,
            double loadRatio,
            double thermalStress,
            ElectricalDiagnosticSource.FlowDirection flowDirection,
            ElectricalDiagnosticSource.FaultKind faultKind
    ) {
        public ElectricalStatus {
            tierId = Objects.requireNonNull(tierId);
            terminalId = Objects.requireNonNull(terminalId);
            voltageVolts = finiteNonNegativeOrZero(voltageVolts);
            chargeCoulombsPerTick = finiteNonNegativeOrZero(chargeCoulombsPerTick);
            currentAmps = finiteNonNegativeOrZero(currentAmps);
            joulesPerTick = finiteNonNegativeOrZero(joulesPerTick);
            powerWatts = finiteNonNegativeOrZero(powerWatts);
            storedJoules = finiteNonNegativeOrZero(storedJoules);
            capacityJoules = finiteNonNegativeOrZero(capacityJoules);
            loadRatio = finiteNonNegativeOrZero(loadRatio);
            thermalStress = Math.min(1.0D, finiteNonNegativeOrZero(thermalStress));
            flowDirection = Objects.requireNonNull(flowDirection);
            faultKind = Objects.requireNonNull(faultKind);
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

    private static double finiteNonNegativeOrZero(double value) {
        return Double.isFinite(value) && value >= 0.0D ? value : 0.0D;
    }
}
