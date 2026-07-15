package committee.nova.mods.magneticraft.content.machine.observation;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Versioned whitelist codec for the state sent to optional overlay clients.
 */
public final class MachineObservationCodec {
    public static final String ROOT_KEY = Magneticraft.MOD_ID;
    public static final int SCHEMA_VERSION = 3;

    private static final String SCHEMA_VERSION_KEY = "schema_version";
    private static final String PROCESS_KEY = "process";
    private static final String ENERGY_KEY = "energy";
    private static final String ELECTRICAL_KEY = "electrical";
    private static final String THERMAL_KEY = "thermal";
    private static final String TANKS_KEY = "tanks";
    private static final String STRUCTURE_KEY = "structure";

    private MachineObservationCodec() {
    }

    public static void write(CompoundTag target, MachineObservation observation) {
        if (observation.isEmpty()) {
            return;
        }

        CompoundTag root = new CompoundTag();
        root.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        observation.process().ifPresent(process -> root.put(PROCESS_KEY, writeProcess(process)));
        observation.energy().ifPresent(energy -> root.put(ENERGY_KEY, writeEnergy(energy)));
        observation.electrical().ifPresent(electrical -> root.put(ELECTRICAL_KEY, writeElectrical(electrical)));
        observation.thermal().ifPresent(thermal -> root.put(THERMAL_KEY, writeThermal(thermal)));
        if (!observation.tanks().isEmpty()) {
            ListTag tanks = new ListTag();
            observation.tanks().forEach(tank -> tanks.add(writeTank(tank)));
            root.put(TANKS_KEY, tanks);
        }
        observation.structure().ifPresent(structure -> root.put(STRUCTURE_KEY, writeStructure(structure)));
        target.put(ROOT_KEY, root);
    }

    public static Optional<MachineObservation> read(CompoundTag target) {
        if (!target.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag root = target.getCompound(ROOT_KEY);
        if (root.getInt(SCHEMA_VERSION_KEY) != SCHEMA_VERSION) {
            return Optional.empty();
        }

        MachineObservation observation = new MachineObservation(
                readProcess(root),
                readEnergy(root),
                readElectrical(root),
                readThermal(root),
                readTanks(root),
                readStructure(root)
        );
        return observation.isEmpty() ? Optional.empty() : Optional.of(observation);
    }

    private static CompoundTag writeProcess(MachineObservation.ProcessStatus process) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("progress", process.progress());
        tag.putInt("total", process.total());
        tag.putBoolean("working", process.working());
        return tag;
    }

    private static CompoundTag writeEnergy(MachineObservation.EnergyStatus energy) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stored", energy.stored());
        tag.putInt("capacity", energy.capacity());
        tag.putString("unit", energy.unit().name());
        return tag;
    }

    private static CompoundTag writeElectrical(MachineObservation.ElectricalStatus electrical) {
        CompoundTag tag = new CompoundTag();
        tag.putString("tier_id", electrical.tierId().toString());
        tag.putString("terminal_id", electrical.terminalId().toString());
        tag.putDouble("voltage_volts", electrical.voltageVolts());
        tag.putDouble("charge_coulombs_per_tick", electrical.chargeCoulombsPerTick());
        tag.putDouble("current_amps", electrical.currentAmps());
        tag.putDouble("joules_per_tick", electrical.joulesPerTick());
        tag.putDouble("power_watts", electrical.powerWatts());
        tag.putDouble("stored_joules", electrical.storedJoules());
        tag.putDouble("capacity_joules", electrical.capacityJoules());
        tag.putDouble("load_ratio", electrical.loadRatio());
        tag.putDouble("thermal_stress", electrical.thermalStress());
        tag.putString("flow_direction", electrical.flowDirection().name());
        tag.putString("fault_kind", electrical.faultKind().name());
        return tag;
    }

    private static CompoundTag writeThermal(MachineObservation.ThermalStatus thermal) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("temperature_kelvin", thermal.temperatureKelvin());
        return tag;
    }

    private static CompoundTag writeTank(MachineObservation.TankStatus tank) {
        CompoundTag tag = new CompoundTag();
        tank.fluidId().ifPresent(id -> tag.putString("fluid_id", id.toString()));
        tag.putInt("amount", tank.amount());
        tag.putInt("capacity", tank.capacity());
        return tag;
    }

    private static CompoundTag writeStructure(MachineObservation.StructureStatus structure) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", structure.formed());
        tag.putBoolean("operational", structure.operational());
        return tag;
    }

    private static Optional<MachineObservation.ProcessStatus> readProcess(CompoundTag root) {
        if (!root.contains(PROCESS_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = root.getCompound(PROCESS_KEY);
        return Optional.of(new MachineObservation.ProcessStatus(
                tag.getInt("progress"),
                tag.getInt("total"),
                tag.getBoolean("working")
        ));
    }

    private static Optional<MachineObservation.EnergyStatus> readEnergy(CompoundTag root) {
        if (!root.contains(ENERGY_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = root.getCompound(ENERGY_KEY);
        return Optional.of(new MachineObservation.EnergyStatus(
                tag.getInt("stored"),
                tag.getInt("capacity"),
                enumValue(
                        MachineObservation.EnergyUnit.class,
                        tag.getString("unit"),
                        MachineObservation.EnergyUnit.FORGE_ENERGY
                )
        ));
    }

    private static Optional<MachineObservation.ElectricalStatus> readElectrical(CompoundTag root) {
        if (!root.contains(ELECTRICAL_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = root.getCompound(ELECTRICAL_KEY);
        ResourceLocation tierId = boundedId(tag, "tier_id");
        ResourceLocation terminalId = boundedId(tag, "terminal_id");
        if (tierId == null || terminalId == null) {
            return Optional.empty();
        }
        return Optional.of(new MachineObservation.ElectricalStatus(
                tierId,
                terminalId,
                tag.getDouble("voltage_volts"),
                tag.getDouble("charge_coulombs_per_tick"),
                tag.getDouble("current_amps"),
                tag.getDouble("joules_per_tick"),
                tag.getDouble("power_watts"),
                tag.getDouble("stored_joules"),
                tag.getDouble("capacity_joules"),
                tag.getDouble("load_ratio"),
                tag.getDouble("thermal_stress"),
                enumValue(
                        ElectricalDiagnosticSource.FlowDirection.class,
                        tag.getString("flow_direction"),
                        ElectricalDiagnosticSource.FlowDirection.IDLE
                ),
                enumValue(
                        ElectricalDiagnosticSource.FaultKind.class,
                        tag.getString("fault_kind"),
                        ElectricalDiagnosticSource.FaultKind.NONE
                )
        ));
    }

    private static Optional<MachineObservation.ThermalStatus> readThermal(CompoundTag root) {
        if (!root.contains(THERMAL_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(new MachineObservation.ThermalStatus(
                root.getCompound(THERMAL_KEY).getDouble("temperature_kelvin")
        ));
    }

    private static List<MachineObservation.TankStatus> readTanks(CompoundTag root) {
        if (!root.contains(TANKS_KEY, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag tags = root.getList(TANKS_KEY, Tag.TAG_COMPOUND);
        List<MachineObservation.TankStatus> tanks = new ArrayList<>(Math.min(tags.size(), MachineObservation.MAX_TANKS));
        for (int index = 0; index < Math.min(tags.size(), MachineObservation.MAX_TANKS); index++) {
            CompoundTag tag = tags.getCompound(index);
            Optional<ResourceLocation> fluidId = Optional.empty();
            if (tag.contains("fluid_id", Tag.TAG_STRING) && tag.getString("fluid_id").length() <= 256) {
                fluidId = Optional.ofNullable(ResourceLocation.tryParse(tag.getString("fluid_id")));
            }
            tanks.add(new MachineObservation.TankStatus(
                    fluidId,
                    tag.getInt("amount"),
                    tag.getInt("capacity")
            ));
        }
        return List.copyOf(tanks);
    }

    private static Optional<MachineObservation.StructureStatus> readStructure(CompoundTag root) {
        if (!root.contains(STRUCTURE_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag tag = root.getCompound(STRUCTURE_KEY);
        return Optional.of(new MachineObservation.StructureStatus(
                tag.getBoolean("formed"),
                tag.getBoolean("operational")
        ));
    }

    private static ResourceLocation boundedId(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return null;
        }
        String value = tag.getString(key);
        return value.length() <= 256 ? ResourceLocation.tryParse(value) : null;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String name, T fallback) {
        if (name == null || name.length() > 64) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
