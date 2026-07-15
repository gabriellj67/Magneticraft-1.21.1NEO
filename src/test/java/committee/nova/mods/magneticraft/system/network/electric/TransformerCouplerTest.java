package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformerCouplerTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void forwardTransferHonorsPowerEfficiencyAndConservation() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(120.0D);
        double before = fixture.first.energyJoules() + fixture.second.energyJoules();

        ElectricalCoupler.CouplingResult result = fixture.coupler.transfer(fixture.first, fixture.second, false);

        assertEquals(800.0D, result.withdrawnJoules(), EPSILON);
        assertEquals(768.0D, result.deliveredJoules(), EPSILON);
        assertEquals(32.0D, result.lostJoules(), EPSILON);
        assertTrue(result.firstWasSource());
        assertEquals(before - result.lostJoules(), fixture.first.energyJoules() + fixture.second.energyJoules(), EPSILON);
    }

    @Test
    void simulationIsStateNeutralAndReturnsSameTransfer() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(120.0D);
        double firstBefore = fixture.first.energyJoules();
        double secondBefore = fixture.second.energyJoules();

        ElectricalCoupler.CouplingResult simulated = fixture.coupler.transfer(fixture.first, fixture.second, true);

        assertTrue(simulated.moved());
        assertEquals(firstBefore, fixture.first.energyJoules(), EPSILON);
        assertEquals(secondBefore, fixture.second.energyJoules(), EPSILON);
        ElectricalCoupler.CouplingResult committed = fixture.coupler.transfer(fixture.first, fixture.second, false);
        assertEquals(simulated.withdrawnJoules(), committed.withdrawnJoules(), EPSILON);
        assertEquals(simulated.deliveredJoules(), committed.deliveredJoules(), EPSILON);
    }

    @Test
    void reverseRequiresBothSidesBelowNinetyFivePercentNominal() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(113.0D);
        fixture.second.setVoltage(455.0D);
        assertTrue(fixture.coupler.canReverse(fixture.first, fixture.second));

        fixture.first.setVoltage(114.0D);
        assertFalse(fixture.coupler.canReverse(fixture.first, fixture.second));
        fixture.first.setVoltage(100.0D);
        fixture.second.setVoltage(456.0D);
        assertFalse(fixture.coupler.canReverse(fixture.first, fixture.second));
    }

    @Test
    void reversedDirectionUsesSecondTerminalAsTheOnlySource() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(0.0D);
        fixture.second.setVoltage(400.0D);
        fixture.coupler.setReversed(true);
        double firstBefore = fixture.first.energyJoules();
        double secondBefore = fixture.second.energyJoules();

        ElectricalCoupler.CouplingResult result = fixture.coupler.transfer(fixture.first, fixture.second, false);

        assertTrue(result.moved());
        assertFalse(result.firstWasSource());
        assertTrue(fixture.first.energyJoules() > firstBefore);
        assertTrue(fixture.second.energyJoules() < secondBefore);
    }

    @Test
    void highOutputVoltageNeverCausesImplicitBackflow() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(50.0D);
        fixture.second.setVoltage(480.0D);

        ElectricalCoupler.CouplingResult result = fixture.coupler.transfer(fixture.first, fixture.second, false);

        assertFalse(result.moved());
        assertFalse(fixture.coupler.reversed());
    }

    @Test
    void destinationGeneratorVoltageCapsDelivery() {
        Fixture fixture = fixture();
        fixture.first.setVoltage(120.0D);
        fixture.second.setVoltage(500.0D);

        assertFalse(fixture.coupler.transfer(fixture.first, fixture.second, false).moved());
    }

    private static Fixture fixture() {
        VoltageTier low = tier(VoltageTierIds.LOW, 60.0D, 120.0D, 125.0D, 125.0D, 2.0D);
        VoltageTier medium = tier(VoltageTierIds.MEDIUM, 240.0D, 480.0D, 500.0D, 500.0D, 0.5D);
        ElectricalNode first = new ElectricalNode(low.machineCapacitanceFarads(), low.maximumVoltage(), low.nodeResistanceOhms());
        ElectricalNode second = new ElectricalNode(medium.machineCapacitanceFarads(), medium.maximumVoltage(), medium.nodeResistanceOhms());
        TransformerCoupler coupler = new TransformerCoupler(
                new PhysicalNodeKey(BlockPos.ZERO, id("input")),
                new PhysicalNodeKey(BlockPos.ZERO, id("output"))
        );
        coupler.reconfigure(
                new TransformerProfile(id("lv_to_mv"), VoltageTierIds.LOW, VoltageTierIds.MEDIUM, 800.0D, 0.96D),
                low,
                medium
        );
        return new Fixture(first, second, coupler);
    }

    private static VoltageTier tier(
            ResourceLocation id,
            double minimum,
            double nominal,
            double maximum,
            double generator,
            double capacitance
    ) {
        return new VoltageTier(
                id,
                "voltage_tier." + id.getNamespace() + "." + id.getPath(),
                minimum,
                nominal,
                maximum,
                generator,
                capacitance,
                capacitance / 4.0D,
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

    private record Fixture(ElectricalNode first, ElectricalNode second, TransformerCoupler coupler) {
    }
}
