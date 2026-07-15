package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleContainer;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalPowerModuleTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void profileCapacityConfiguresTheOnlyNodeBalance() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalPowerModule.ForgeEnergyAccess.NONE);

        assertEquals(1.28D, fixture.node.capacitance(), EPSILON);
        assertEquals(10_000.0D, fixture.node.ratedMaximumEnergyJoules(), EPSILON);
        assertEquals(10_000.0D, fixture.power.ratedCapacityJoules(), EPSILON);

        fixture.power.setStoredJoules(5_000.0D);
        assertEquals(5_000.0D, fixture.node.energyJoules(), EPSILON);
        assertEquals(fixture.node.energyJoules(), fixture.power.storedJoules(), EPSILON);
    }

    @Test
    void transactionalRestorePreservesAnExistingOverratedBalance() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalPowerModule.ForgeEnergyAccess.NONE);
        fixture.node.setEnergyJoules(12_000.0D);
        double before = fixture.power.storedJoules();
        assertEquals(100, fixture.power.consumeJoules(100, false));

        fixture.power.restoreStoredJoules(before);

        assertEquals(12_000.0D, fixture.node.energyJoules(), EPSILON);
    }

    @Test
    void consumerRequiresMinimumVoltageAndSimulationIsPure() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalPowerModule.ForgeEnergyAccess.NONE);
        fixture.node.setVoltage(59.0D);
        double lowVoltageEnergy = fixture.node.energyJoules();

        assertEquals(0, fixture.power.consumeJoules(40, true));
        assertEquals(lowVoltageEnergy, fixture.node.energyJoules(), EPSILON);

        fixture.node.setVoltage(120.0D);
        double before = fixture.node.energyJoules();
        assertEquals(40, fixture.power.consumeJoules(40, true));
        assertEquals(before, fixture.node.energyJoules(), EPSILON);
        assertEquals(40, fixture.power.consumeJoules(40, false));
        assertEquals(before - 40.0D, fixture.node.energyJoules(), EPSILON);
    }

    @Test
    void reconstructedThresholdAndNominalVoltagesKeepTheirExactContracts() {
        Fixture fixture = fixture(ElectricalRole.CONSUMER, ElectricalPowerModule.ForgeEnergyAccess.NONE);

        fixture.node.setVoltage(fixture.tier.minimumOperatingVoltage());
        assertTrue(fixture.tier.meetsMinimumOperatingVoltage(fixture.node.voltage()));
        assertEquals(0.0D, fixture.tier.operatingRateFraction(fixture.node.voltage()), EPSILON);

        fixture.node.setVoltage(fixture.tier.nominalVoltage());
        assertEquals(1.0D, fixture.tier.operatingRateFraction(fixture.node.voltage()), EPSILON);
    }

    @Test
    void generatorUsesProfileRateAndVoltageSetpointWithoutASecondBuffer() {
        Fixture fixture = fixture(ElectricalRole.GENERATOR, ElectricalPowerModule.ForgeEnergyAccess.NONE);

        assertEquals(200.0D, fixture.power.generateJoules(500.0D, true), EPSILON);
        assertEquals(0.0D, fixture.node.energyJoules(), EPSILON);
        assertEquals(200.0D, fixture.power.generateJoules(500.0D, false), EPSILON);
        assertEquals(200.0D, fixture.node.energyJoules(), EPSILON);

        fixture.node.setVoltage(125.0D);
        assertEquals(0.0D, fixture.power.generateJoules(500.0D, false), EPSILON);
        assertEquals(10_000.0D, fixture.node.energyJoules(), EPSILON);
    }

    @Test
    void forgeEnergyInputConvertsDirectlyAndKeepsSimulationPure() {
        Fixture input = fixture(ElectricalRole.CONVERTER, ElectricalPowerModule.ForgeEnergyAccess.INPUT);
        assertEquals(200, input.power.receiveForgeEnergy(1_000, true));
        assertEquals(0.0D, input.node.energyJoules(), EPSILON);
        assertEquals(200, input.power.receiveForgeEnergy(1_000, false));
        assertEquals(200.0D, input.node.energyJoules(), EPSILON);
        assertEquals(0, input.power.receiveForgeEnergy(1_000, false));
        assertEquals(200.0D, input.node.energyJoules(), EPSILON);
    }

    @Test
    void passiveNativeProfileBindsWithoutAForgeEnergyAdapter() {
        Fixture fixture = fixture(ElectricalRole.PASSIVE, ElectricalPowerModule.ForgeEnergyAccess.NONE);

        assertTrue(fixture.power.electricalControllerBound());
        fixture.power.setStoredJoules(1_000.0D);
        assertEquals(1_000.0D, fixture.node.energyJoules(), EPSILON);
        assertEquals(0, fixture.power.receiveForgeEnergy(1_000, false));
    }

    @Test
    void forgeEnergyInputNeverConvertsFractionalRoom() {
        Fixture input = fixture(ElectricalRole.CONVERTER, ElectricalPowerModule.ForgeEnergyAccess.INPUT);
        input.node.setEnergyJoules(input.power.ratedCapacityJoules() - 0.5D);

        assertEquals(0, input.power.receiveForgeEnergy(1, true));
        assertEquals(0, input.power.receiveForgeEnergy(1, false));
        assertEquals(input.power.ratedCapacityJoules() - 0.5D, input.node.energyJoules(), EPSILON);
    }

    @Test
    void legacyIntegerBalanceCombinesWithTheNodeAndIsNotSavedAgain() {
        Fixture fixture = fixture(ElectricalRole.STORAGE, ElectricalPowerModule.ForgeEnergyAccess.NONE);
        MachineModuleContainer modules = new MachineModuleContainer();
        modules.add(fixture.electricity);
        modules.add(fixture.power);
        fixture.node.setEnergyJoules(100.0D);
        CompoundTag legacy = modules.save();
        legacy.getCompound(fixture.power.id().toString()).putInt("energy", 250);

        fixture.node.setEnergyJoules(0.0D);
        modules.load(legacy);
        assertEquals(350.0D, fixture.node.energyJoules(), EPSILON);
        modules.load(legacy);
        assertEquals(350.0D, fixture.node.energyJoules(), EPSILON);

        CompoundTag current = modules.save();
        assertFalse(current.getCompound(fixture.power.id().toString()).contains("energy"));
        fixture.node.setEnergyJoules(0.0D);
        modules.load(current);
        assertEquals(350.0D, fixture.node.energyJoules(), EPSILON);
    }

    private static Fixture fixture(
            ElectricalRole role,
            ElectricalPowerModule.ForgeEnergyAccess forgeEnergyAccess
    ) {
        TestHost host = new TestHost();
        ElectricalNode node = new ElectricalNode(2.0D, 125.0D, 0.005D);
        ElectricalNetworkModule electricity = new ElectricalNetworkModule(
                id("electricity"), host, node, side -> true
        );
        ResourceLocation profileId = id("test_machine");
        ElectricalPowerModule power = new ElectricalPowerModule(
                id("energy_storage"),
                profileId,
                host,
                electricity,
                forgeEnergyAccess,
                side -> true,
                false
        );
        ElectricalDataSnapshot snapshot = snapshot(profileId, role);
        power.rebindElectricalProfile(snapshot);
        return new Fixture(electricity, node, power, snapshot.voltageTier(VoltageTierIds.LOW).orElseThrow());
    }

    private static ElectricalDataSnapshot snapshot(ResourceLocation profileId, ElectricalRole role) {
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
                profileId, VoltageTierIds.LOW, role, 10_000.0D, 200.0D, 8.0D
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

    private record Fixture(
            ElectricalNetworkModule electricity,
            ElectricalNode node,
            ElectricalPowerModule power,
            VoltageTier tier
    ) {
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
