package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalEnergyBridgeModuleTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void chargeUsesTenVoltLinearRampAndPreservesOneJoulePerFe() {
        Fixture fixture = fixture(65.0D, 0);
        double before = fixture.node.energyJoules() + fixture.energy.getEnergyStored();

        fixture.bridge.serverTick();

        assertEquals(100, fixture.energy.getEnergyStored());
        assertEquals(before - 100.0D, fixture.node.energyJoules(), EPSILON);
        assertEquals(before, fixture.node.energyJoules() + fixture.energy.getEnergyStored(), EPSILON);
    }

    @Test
    void exactThresholdDoesNotMoveEnergy() {
        Fixture fixture = fixture(60.0D, 500);
        double nodeEnergy = fixture.node.energyJoules();

        fixture.bridge.serverTick();

        assertEquals(500, fixture.energy.getEnergyStored());
        assertEquals(nodeEnergy, fixture.node.energyJoules(), EPSILON);
    }

    @Test
    void fullRateModeChargesAtTheExactThresholdAndReportsTheTransfer() {
        Fixture fixture = fixture(
                60.0D,
                0,
                1_000,
                ElectricalEnergyBridgeModule.ChargeMode.FULL_RATE_AT_THRESHOLD
        );
        double before = fixture.node.energyJoules() + fixture.energy.getEnergyStored();

        fixture.bridge.serverTick();

        assertEquals(1_000, fixture.bridge.lastChargeTransfer());
        assertEquals(1_000, fixture.energy.getEnergyStored());
        assertEquals(before, fixture.node.energyJoules() + fixture.energy.getEnergyStored(), EPSILON);
    }

    @Test
    void lowVoltageBackfeedsWithoutRequiringAConnection() {
        Fixture fixture = fixture(55.0D, 500);
        double before = fixture.node.energyJoules() + fixture.energy.getEnergyStored();

        fixture.bridge.serverTick();

        assertEquals(300, fixture.energy.getEnergyStored());
        assertEquals(before, fixture.node.energyJoules() + fixture.energy.getEnergyStored(), EPSILON);
        fixture.node.beginNetworkTick();
        fixture.node.completeNetworkTick();
        var reading = fixture.electricity.electricalReading(Direction.UP).orElseThrow();
        assertTrue(reading.currentAmps() > 0.0D);
        assertTrue(reading.powerWatts() > 0.0D);
    }

    @Test
    void isolatedNodeBelowChargeThresholdIsNotForceDrained() {
        Fixture fixture = fixture(50.0D, 0);
        double nodeEnergy = fixture.node.energyJoules();

        fixture.bridge.serverTick();

        assertEquals(0, fixture.energy.getEnergyStored());
        assertEquals(nodeEnergy, fixture.node.energyJoules(), EPSILON);
    }

    private static Fixture fixture(double voltage, int storedEnergy) {
        return fixture(voltage, storedEnergy, 200, ElectricalEnergyBridgeModule.ChargeMode.LINEAR_RAMP);
    }

    private static Fixture fixture(
            double voltage,
            int storedEnergy,
            int maxTransfer,
            ElectricalEnergyBridgeModule.ChargeMode chargeMode
    ) {
        TestHost host = new TestHost();
        ElectricalNode node = new ElectricalNode(1.0D, 125.0D, 0.001D);
        node.setVoltage(voltage);
        EnergyStorageModule energy = new EnergyStorageModule(
                id("energy"), host, 10_000, maxTransfer, maxTransfer, side -> false, false, false
        );
        energy.setEnergyStored(storedEnergy);
        ElectricalNetworkModule electricity = new ElectricalNetworkModule(
                id("electricity"), host, node, side -> true
        );
        ElectricalEnergyBridgeModule bridge = new ElectricalEnergyBridgeModule(
                id("bridge"), electricity, energy, 60.0D, 60.0D, maxTransfer, chargeMode
        );
        return new Fixture(node, energy, electricity, bridge);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private record Fixture(
            ElectricalNode node,
            EnergyStorageModule energy,
            ElectricalNetworkModule electricity,
            ElectricalEnergyBridgeModule bridge
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
