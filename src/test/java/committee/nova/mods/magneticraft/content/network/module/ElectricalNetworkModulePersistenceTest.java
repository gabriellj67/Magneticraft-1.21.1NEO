package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleContainer;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalNetworkModulePersistenceTest {
    private static final double EPSILON = 1.0E-7D;
    private static final ResourceLocation LOW = id("low_voltage");
    private static final ResourceLocation MEDIUM = id("medium_voltage");
    private static final ResourceLocation INPUT = id("input");

    @Test
    void currentSchemaRoundTripsTierTerminalAndJoulesThenRebindsWithoutEnergyChange() {
        ElectricalNetworkModule source = module(LOW, INPUT);
        source.node().setVoltage(120.0D);
        double energy = source.node().energyJoules();
        MachineModuleContainer sourceModules = new MachineModuleContainer();
        sourceModules.add(source);
        CompoundTag saved = sourceModules.save();

        ElectricalNetworkModule restored = module(MEDIUM, INPUT);
        MachineModuleContainer restoredModules = new MachineModuleContainer();
        restoredModules.add(restored);
        restoredModules.load(saved);
        restored.rebindElectricalProfile(snapshot());

        assertEquals(LOW, restored.tierId());
        assertEquals(INPUT, restored.terminalId());
        assertEquals(energy, restored.node().energyJoules(), EPSILON);
        assertEquals(0.5D, restored.node().capacitance(), EPSILON);
        assertEquals(125.0D, restored.node().maxVoltage(), EPSILON);
        assertTrue(restored.electricalProfileBound());
    }

    @Test
    void legacyUnknownAndTerminalMismatchPayloadsResetSafely() {
        ElectricalNetworkModule legacy = module(LOW, INPUT);
        CompoundTag oldPayload = new CompoundTag();
        oldPayload.putDouble("energy_joules", 10_000.0D);
        legacy.load(oldPayload);
        assertEquals(0.0D, legacy.node().energyJoules(), EPSILON);

        ElectricalNetworkModule source = module(LOW, INPUT);
        source.node().setVoltage(120.0D);
        CompoundTag saved = new CompoundTag();
        source.save(saved);

        ElectricalNetworkModule wrongTerminal = module(LOW, id("output"));
        wrongTerminal.load(saved);
        assertEquals(0.0D, wrongTerminal.node().energyJoules(), EPSILON);

        saved.putString("tier_id", id("missing").toString());
        ElectricalNetworkModule missing = module(LOW, INPUT);
        missing.load(saved);
        missing.rebindElectricalProfile(snapshot());
        assertFalse(missing.electricalProfileBound());
        assertEquals(id("missing"), missing.tierId());
    }

    @Test
    void boundedClientTelemetryRoundTripsAndMalformedNumbersFailSafe() {
        ElectricalNetworkModule source = module(LOW, INPUT);
        source.rebindElectricalProfile(snapshot());
        source.node().setVoltage(120.0D);
        source.node().beginNetworkTick();
        source.node().removeEnergy(40.0D, false);
        source.node().completeNetworkTick();
        CompoundTag clientTag = new CompoundTag();
        source.saveClientData(clientTag);

        ElectricalNetworkModule restored = module(MEDIUM, INPUT);
        restored.loadClientData(clientTag);
        var reading = restored.syncedClientReading().orElseThrow();
        assertEquals(LOW, reading.tierId());
        assertEquals(INPUT, reading.terminalId());
        assertTrue(reading.voltageVolts() > 0.0D);
        assertEquals(40.0D, reading.joulesPerTick(), EPSILON);
        assertEquals(800.0D, reading.powerWatts(), EPSILON);

        clientTag.putDouble("voltage", Double.NaN);
        clientTag.putDouble("stress", Double.POSITIVE_INFINITY);
        clientTag.putString("flow", "future_flow");
        clientTag.putString("fault", "future_fault");
        restored.loadClientData(clientTag);
        reading = restored.syncedClientReading().orElseThrow();
        assertEquals(0.0D, reading.voltageVolts(), EPSILON);
        assertEquals(0.0D, reading.thermalStress(), EPSILON);
        assertEquals(
                committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FlowDirection.IDLE,
                reading.flowDirection()
        );
        assertEquals(
                committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind.MISSING_PROFILE,
                reading.faultKind()
        );
    }

    @Test
    void clientTierChangeRequestsExactlyOneStaticModelRefresh() {
        TestHost host = new TestHost();
        ElectricalNetworkModule restored = module(LOW, INPUT, host);
        ElectricalNetworkModule source = module(MEDIUM, INPUT);
        CompoundTag clientTag = new CompoundTag();
        source.saveClientData(clientTag);

        restored.loadClientData(clientTag);
        restored.loadClientData(clientTag);

        assertEquals(MEDIUM, restored.tierId());
        assertEquals(1, host.modelRefreshRequests);
    }

    @Test
    void placementTierChangeSynchronizesImmediatelyAndUnchangedIdentityDoesNotResend() {
        TestHost host = new TestHost();
        ElectricalNetworkModule module = module(LOW, INPUT, host);

        module.applyTierFromPlacementData(MEDIUM);
        module.applyTierFromPlacementData(MEDIUM);

        assertEquals(MEDIUM, module.tierId());
        assertEquals(1, host.syncRequests);
    }

    private static ElectricalNetworkModule module(ResourceLocation tier, ResourceLocation terminal) {
        return module(tier, terminal, new TestHost());
    }

    private static ElectricalNetworkModule module(
            ResourceLocation tier,
            ResourceLocation terminal,
            MachineModuleHost host
    ) {
        return new ElectricalNetworkModule(
                id("electricity"),
                host,
                tier,
                terminal,
                ElectricalNodeKind.CONDUCTOR,
                side -> true
        );
    }

    private static ElectricalDataSnapshot snapshot() {
        VoltageTier low = tier(LOW, 60.0D, 120.0D, 125.0D, 2.0D, 0.5D);
        VoltageTier medium = tier(MEDIUM, 240.0D, 480.0D, 500.0D, 0.5D, 0.125D);
        TransformerProfile transformer = new TransformerProfile(
                id("lv_to_mv"), LOW, MEDIUM, 800.0D, 0.96D
        );
        MachineElectricalProfile machine = new MachineElectricalProfile(
                id("test_machine"), LOW, ElectricalRole.PASSIVE, 1_000.0D, 100.0D, 8.0D
        );
        return new ElectricalDataSnapshot(
                1L,
                Map.of(LOW, low, MEDIUM, medium),
                Map.of(transformer.id(), transformer),
                Map.of(machine.id(), machine)
        );
    }

    private static VoltageTier tier(
            ResourceLocation id,
            double minimum,
            double nominal,
            double maximum,
            double machineCapacitance,
            double conductorCapacitance
    ) {
        return new VoltageTier(
                id,
                "tier." + id.getPath(),
                minimum,
                nominal,
                maximum,
                maximum,
                machineCapacitance,
                conductorCapacitance,
                0.005D,
                8.0D,
                16.0D,
                8.0D,
                16.0D,
                400.0D,
                0.0025D,
                800.0D,
                0.00125D,
                8,
                16,
                1_000_000L,
                640.0D,
                0xD98245
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private static final class TestHost implements MachineModuleHost {
        private int modelRefreshRequests;
        private int syncRequests;

        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
            syncRequests++;
        }

        @Override
        public void requestModelRefresh() {
            modelRefreshRequests++;
        }

        @Override
        public Level level() {
            return null;
        }

        @Override
        public BlockPos position() {
            return BlockPos.ZERO;
        }
    }
}
