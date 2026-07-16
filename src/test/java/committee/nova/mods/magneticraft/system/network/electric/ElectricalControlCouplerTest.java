package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalControlCouplerTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void idealSwitchEqualizesBidirectionallyAndConservesReportedLoss() {
        Fixture fixture = fixture(ElectricalControlCoupler.DirectionMode.BIDIRECTIONAL, 0.0D);
        fixture.first.setVoltage(100.0D);
        double before = fixture.totalEnergy();

        ElectricalCoupler.CouplingResult forward = fixture.coupler.transfer(fixture.first, fixture.second, false);

        assertTrue(forward.moved());
        assertTrue(forward.firstWasSource());
        assertEquals(50.0D, fixture.first.voltage(), EPSILON);
        assertEquals(50.0D, fixture.second.voltage(), EPSILON);
        assertEquals(before - forward.lostJoules(), fixture.totalEnergy(), EPSILON);

        fixture.first.setVoltage(0.0D);
        fixture.second.setVoltage(100.0D);
        ElectricalCoupler.CouplingResult reverse = fixture.coupler.transfer(fixture.first, fixture.second, false);
        assertTrue(reverse.moved());
        assertFalse(reverse.firstWasSource());
    }

    @Test
    void diodeBlocksReverseVoltageAndAddsNoForwardDrop() {
        Fixture fixture = fixture(ElectricalControlCoupler.DirectionMode.FIRST_TO_SECOND, 0.0D);
        fixture.second.setVoltage(100.0D);
        assertFalse(fixture.coupler.transfer(fixture.first, fixture.second, false).moved());

        fixture.first.setVoltage(100.0D);
        fixture.second.setVoltage(0.0D);
        assertTrue(fixture.coupler.transfer(fixture.first, fixture.second, false).moved());
        assertEquals(fixture.first.voltage(), fixture.second.voltage(), EPSILON);
    }

    @Test
    void resistorUsesOhmsLawInBothDirectionsAndReportsDissipation() {
        Fixture fixture = fixture(ElectricalControlCoupler.DirectionMode.BIDIRECTIONAL, 10.0D);
        fixture.first.setVoltage(100.0D);
        double before = fixture.totalEnergy();

        ElectricalCoupler.CouplingResult forward = fixture.coupler.transfer(fixture.first, fixture.second, false);

        assertEquals(0.5D, forward.sourceChargeCoulombs(), EPSILON);
        assertEquals(10.0D, fixture.coupler.lastCurrentAmps(), EPSILON);
        assertTrue(forward.lostJoules() > 0.0D);
        assertEquals(before - forward.lostJoules(), fixture.totalEnergy(), EPSILON);

        fixture.first.setVoltage(0.0D);
        fixture.second.setVoltage(80.0D);
        ElectricalCoupler.CouplingResult reverse = fixture.coupler.transfer(fixture.first, fixture.second, false);
        assertFalse(reverse.firstWasSource());
        assertEquals(8.0D, fixture.coupler.lastCurrentAmps(), EPSILON);
    }

    @Test
    void disabledAndSimulatedTransfersDoNotMutateNodesOrTelemetry() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicReference<Double> resistance = new AtomicReference<>(5.0D);
        ElectricalControlCoupler coupler = coupler(
                ElectricalControlCoupler.DirectionMode.BIDIRECTIONAL,
                enabled,
                resistance
        );
        ElectricalNode first = node();
        ElectricalNode second = node();
        first.setVoltage(100.0D);
        double firstBefore = first.energyJoules();

        assertTrue(coupler.transfer(first, second, true).moved());
        assertEquals(firstBefore, first.energyJoules(), EPSILON);
        assertFalse(coupler.lastResult().moved());

        enabled.set(false);
        assertFalse(coupler.transfer(first, second, false).moved());
        assertEquals(firstBefore, first.energyJoules(), EPSILON);
    }

    private static Fixture fixture(ElectricalControlCoupler.DirectionMode mode, double resistance) {
        ElectricalNode first = node();
        ElectricalNode second = node();
        return new Fixture(first, second, coupler(
                mode,
                new AtomicBoolean(true),
                new AtomicReference<>(resistance)
        ));
    }

    private static ElectricalControlCoupler coupler(
            ElectricalControlCoupler.DirectionMode mode,
            AtomicBoolean enabled,
            AtomicReference<Double> resistance
    ) {
        return new ElectricalControlCoupler(
                new PhysicalNodeKey(BlockPos.ZERO, id("back")),
                new PhysicalNodeKey(BlockPos.ZERO, id("front")),
                mode,
                enabled::get,
                resistance::get
        );
    }

    private static ElectricalNode node() {
        return new ElectricalNode(1.0D, 1_000.0D, 0.01D);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private record Fixture(ElectricalNode first, ElectricalNode second, ElectricalControlCoupler coupler) {
        private double totalEnergy() {
            return first.energyJoules() + second.energyJoules();
        }
    }
}
