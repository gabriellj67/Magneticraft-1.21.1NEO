package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElectricalEnergyBridgeModuleTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void consumerChargesAtVoltageLimitedRateWithoutBackfeeding() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalEnergyBridgeModule.ExchangePolicy.PROFILE_ROLE, 90.0D, 0);
        double before = fixture.totalEnergy();

        fixture.bridge.injectElectricalEnergy(null);
        fixture.bridge.extractElectricalEnergy(null);

        assertEquals(100, fixture.energy.getEnergyStored());
        assertEquals(100, fixture.bridge.lastChargeTransfer());
        assertEquals(0, fixture.bridge.lastDischargeTransfer());
        assertEquals(before, fixture.totalEnergy(), EPSILON);

        fixture.node.setVoltage(55.0D);
        fixture.energy.setEnergyStored(500);
        double lowVoltageEnergy = fixture.node.energyJoules();
        fixture.bridge.injectElectricalEnergy(null);
        fixture.bridge.extractElectricalEnergy(null);
        assertEquals(500, fixture.energy.getEnergyStored());
        assertEquals(lowVoltageEnergy, fixture.node.energyJoules(), EPSILON);
    }

    @Test
    void consumerAtMinimumVoltagePauses() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalEnergyBridgeModule.ExchangePolicy.PROFILE_ROLE, 60.0D, 0);
        double before = fixture.totalEnergy();

        fixture.bridge.extractElectricalEnergy(null);

        assertEquals(0, fixture.energy.getEnergyStored());
        assertEquals(before, fixture.totalEnergy(), EPSILON);
    }

    @Test
    void generatorInjectsOnlyUntilTierSetpoint() {
        Fixture fixture = fixture(ElectricalRole.GENERATOR, ElectricalEnergyBridgeModule.ExchangePolicy.PROFILE_ROLE, 0.0D, 500);
        double before = fixture.totalEnergy();

        fixture.bridge.injectElectricalEnergy(null);
        fixture.bridge.extractElectricalEnergy(null);

        assertEquals(300, fixture.energy.getEnergyStored());
        assertEquals(200, fixture.bridge.lastDischargeTransfer());
        assertEquals(before, fixture.totalEnergy(), EPSILON);

        fixture.node.setVoltage(125.0D);
        fixture.energy.setEnergyStored(500);
        fixture.bridge.injectElectricalEnergy(null);
        assertEquals(500, fixture.energy.getEnergyStored());
        assertEquals(0, fixture.bridge.lastDischargeTransfer());
    }

    @Test
    void storageUsesSeventyFivePercentTargetWithTwoPercentDeadband() {
        Fixture fixture = fixture(ElectricalRole.STORAGE, ElectricalEnergyBridgeModule.ExchangePolicy.PROFILE_ROLE, 80.0D, 500);
        double before = fixture.totalEnergy();
        fixture.bridge.injectElectricalEnergy(null);
        assertEquals(300, fixture.energy.getEnergyStored());
        assertEquals(before, fixture.totalEnergy(), EPSILON);

        fixture.node.setVoltage(90.0D);
        fixture.energy.setEnergyStored(500);
        double deadbandEnergy = fixture.totalEnergy();
        fixture.bridge.injectElectricalEnergy(null);
        fixture.bridge.extractElectricalEnergy(null);
        assertEquals(500, fixture.energy.getEnergyStored());
        assertEquals(deadbandEnergy, fixture.totalEnergy(), EPSILON);

        fixture.node.setVoltage(100.0D);
        fixture.energy.setEnergyStored(0);
        before = fixture.totalEnergy();
        fixture.bridge.extractElectricalEnergy(null);
        assertEquals(200, fixture.energy.getEnergyStored());
        assertEquals(before, fixture.totalEnergy(), EPSILON);
    }

    @Test
    void converterPolicySelectsExactlyOneDirection() {
        Fixture nativeToFe = fixture(
                ElectricalRole.CONVERTER,
                ElectricalEnergyBridgeModule.ExchangePolicy.NATIVE_TO_FE,
                120.0D,
                0
        );
        nativeToFe.bridge.injectElectricalEnergy(null);
        nativeToFe.bridge.extractElectricalEnergy(null);
        assertEquals(200, nativeToFe.energy.getEnergyStored());
        assertEquals(0, nativeToFe.bridge.lastDischargeTransfer());

        Fixture feToNative = fixture(
                ElectricalRole.CONVERTER,
                ElectricalEnergyBridgeModule.ExchangePolicy.FE_TO_NATIVE,
                0.0D,
                500
        );
        feToNative.bridge.injectElectricalEnergy(null);
        feToNative.bridge.extractElectricalEnergy(null);
        assertEquals(300, feToNative.energy.getEnergyStored());
        assertEquals(0, feToNative.bridge.lastChargeTransfer());
    }

    @Test
    void fractionalNodeEnergyRemainderAlwaysStaysAtSource() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalEnergyBridgeModule.ExchangePolicy.PROFILE_ROLE, 90.0D, 0);
        fixture.node.setEnergyJoules(1_000.75D);
        double before = fixture.totalEnergy();

        fixture.bridge.extractElectricalEnergy(null);

        assertEquals(before, fixture.totalEnergy(), EPSILON);
        assertEquals(0.75D, fixture.node.energyJoules() % 1.0D, EPSILON);
    }

    @Test
    void electricalTransferRateDoesNotThrottleInternalMachineActions() {
        TestHost host = new TestHost();
        ElectricalNode node = new ElectricalNode(1.0D, 125.0D, 0.005D);
        node.setVoltage(120.0D);
        EnergyStorageModule energy = new EnergyStorageModule(
                id("energy"), host, 10_000, 500, 500, side -> false, false, false
        );
        ElectricalNetworkModule electricity = new ElectricalNetworkModule(
                id("electricity"), host, node, side -> true
        );
        ResourceLocation profileId = id("slow_grid_machine");
        ElectricalEnergyBridgeModule bridge = new ElectricalEnergyBridgeModule(
                id("bridge"), profileId, electricity, energy
        );
        bridge.rebindElectricalProfile(snapshot(profileId, ElectricalRole.CONSUMER, 40.0D));
        node.setVoltage(120.0D);

        bridge.extractElectricalEnergy(null);
        assertEquals(40, bridge.lastChargeTransfer());
        assertEquals(40, bridge.maximumTransferJoulesPerTick());

        energy.setEnergyStored(500);
        assertEquals(500, energy.extractEnergy(500, true));
    }

    private static Fixture fixture(
            ElectricalRole role,
            ElectricalEnergyBridgeModule.ExchangePolicy policy,
            double voltage,
            int storedEnergy
    ) {
        TestHost host = new TestHost();
        ElectricalNode node = new ElectricalNode(1.0D, 125.0D, 0.005D);
        node.setVoltage(voltage);
        EnergyStorageModule energy = new EnergyStorageModule(
                id("energy"), host, 10_000, 200, 200, side -> false, false, false
        );
        energy.setEnergyStored(storedEnergy);
        ElectricalNetworkModule electricity = new ElectricalNetworkModule(
                id("electricity"), host, node, side -> true
        );
        ResourceLocation profileId = id("test_machine");
        ElectricalEnergyBridgeModule bridge = new ElectricalEnergyBridgeModule(
                id("bridge"), profileId, electricity, energy, policy, false
        );
        bridge.rebindElectricalProfile(snapshot(profileId, role));
        node.setVoltage(voltage);
        return new Fixture(node, energy, bridge);
    }

    private static ElectricalDataSnapshot snapshot(ResourceLocation profileId, ElectricalRole role) {
        return snapshot(profileId, role, 200.0D);
    }

    private static ElectricalDataSnapshot snapshot(
            ResourceLocation profileId,
            ElectricalRole role,
            double maximumTransferJoulesPerTick
    ) {
        VoltageTier tier = new VoltageTier(
                VoltageTierIds.LOW,
                "voltage_tier.magneticraft.low_voltage",
                60.0D,
                120.0D,
                125.0D,
                125.0D,
                2.0D,
                0.5D,
                0.005D,
                8.0D,
                16.0D,
                8.0D,
                16.0D,
                400.0D,
                0.0025D,
                800.0D,
                0.00125D,
                400.0D,
                8,
                16,
                1_000_000L,
                640.0D,
                0xD98245
        );
        TransformerProfile transformer = new TransformerProfile(
                id("test_transformer"), VoltageTierIds.LOW, VoltageTierIds.MEDIUM, 800.0D, 0.96D
        );
        MachineElectricalProfile machine = new MachineElectricalProfile(
                profileId, VoltageTierIds.LOW, role, 10_000.0D, maximumTransferJoulesPerTick, 8.0D
        );
        return new ElectricalDataSnapshot(
                1L,
                Map.of(VoltageTierIds.LOW, tier),
                Map.of(transformer.id(), transformer),
                Map.of(profileId, machine)
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private record Fixture(ElectricalNode node, EnergyStorageModule energy, ElectricalEnergyBridgeModule bridge) {
        private double totalEnergy() {
            return node.energyJoules() + energy.getEnergyStored();
        }
    }

    private static final class TestHost implements MachineModuleHost {
        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
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
