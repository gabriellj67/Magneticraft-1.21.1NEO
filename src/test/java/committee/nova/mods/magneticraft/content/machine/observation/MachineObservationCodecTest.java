package committee.nova.mods.magneticraft.content.machine.observation;

import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineObservationCodecTest {
    @Test
    void roundTripsOnlyThePublicMachineStatusWhitelist() {
        MachineObservation observation = new MachineObservation(
                Optional.of(new MachineObservation.ProcessStatus(20, 100, true)),
                Optional.of(new MachineObservation.EnergyStatus(400, 1_000)),
                Optional.of(new MachineObservation.ElectricalStatus(
                        ResourceLocation.fromNamespaceAndPath("magneticraft", "low_voltage"),
                        ResourceLocation.fromNamespaceAndPath("magneticraft", "main"),
                        60.0D,
                        0.125D,
                        2.5D,
                        7.5D,
                        150.0D,
                        720.0D,
                        14_400.0D,
                        0.25D,
                        0.5D,
                        ElectricalDiagnosticSource.FlowDirection.INPUT,
                        ElectricalDiagnosticSource.FaultKind.NONE
                )),
                Optional.of(new MachineObservation.ThermalStatus(525.0D)),
                List.of(new MachineObservation.TankStatus(
                        Optional.of(ResourceLocation.fromNamespaceAndPath("magneticraft", "heavy_oil")),
                        750,
                        4_000
                )),
                Optional.of(new MachineObservation.StructureStatus(true, false))
        );

        CompoundTag data = new CompoundTag();
        MachineObservationCodec.write(data, observation);

        assertEquals(Optional.of(observation), MachineObservationCodec.read(data));
        CompoundTag root = data.getCompound(MachineObservationCodec.ROOT_KEY);
        assertEquals(
                Set.of("schema_version", "process", "energy", "electrical", "thermal", "tanks", "structure"),
                root.getAllKeys()
        );
        String encoded = root.toString();
        for (String forbidden : List.of("program", "source", "inventory", "owner", "session", "output")) {
            assertFalse(encoded.contains(forbidden), forbidden);
        }
    }

    @Test
    void rejectsUnknownSchemaAndBoundsTankCount() {
        MachineObservation observation = new MachineObservation(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                java.util.stream.IntStream.range(0, MachineObservation.MAX_TANKS + 3)
                        .mapToObj(index -> new MachineObservation.TankStatus(Optional.empty(), index, 100))
                        .toList(),
                Optional.empty()
        );
        assertEquals(MachineObservation.MAX_TANKS, observation.tanks().size());

        CompoundTag data = new CompoundTag();
        MachineObservationCodec.write(data, observation);
        assertEquals(
                MachineObservation.MAX_TANKS,
                data.getCompound(MachineObservationCodec.ROOT_KEY).getList("tanks", Tag.TAG_COMPOUND).size()
        );

        data.getCompound(MachineObservationCodec.ROOT_KEY).putInt("schema_version", 99);
        assertTrue(MachineObservationCodec.read(data).isEmpty());
    }

    @Test
    void rejectsUnboundedElectricalIdsAndSafelyDefaultsUnknownEnums() {
        MachineObservation observation = new MachineObservation(
                Optional.empty(),
                Optional.of(new MachineObservation.EnergyStatus(1, 2)),
                Optional.of(new MachineObservation.ElectricalStatus(
                        ResourceLocation.fromNamespaceAndPath("magneticraft", "low_voltage"),
                        ResourceLocation.fromNamespaceAndPath("magneticraft", "main"),
                        120.0D,
                        1.0D,
                        20.0D,
                        100.0D,
                        2_000.0D,
                        100.0D,
                        14_400.0D,
                        0.5D,
                        0.25D,
                        ElectricalDiagnosticSource.FlowDirection.OUTPUT,
                        ElectricalDiagnosticSource.FaultKind.NONE
                )),
                Optional.empty(),
                List.of(),
                Optional.empty()
        );
        CompoundTag data = new CompoundTag();
        MachineObservationCodec.write(data, observation);
        CompoundTag electrical = data.getCompound(MachineObservationCodec.ROOT_KEY).getCompound("electrical");
        electrical.putString("flow_direction", "FUTURE_DIRECTION");
        electrical.putString("fault_kind", "FUTURE_FAULT");
        MachineObservation.ElectricalStatus decoded = MachineObservationCodec.read(data)
                .orElseThrow()
                .electrical()
                .orElseThrow();
        assertEquals(ElectricalDiagnosticSource.FlowDirection.IDLE, decoded.flowDirection());
        assertEquals(ElectricalDiagnosticSource.FaultKind.NONE, decoded.faultKind());

        electrical.putString("tier_id", "x".repeat(257));
        assertTrue(MachineObservationCodec.read(data).orElseThrow().electrical().isEmpty());
    }
}
