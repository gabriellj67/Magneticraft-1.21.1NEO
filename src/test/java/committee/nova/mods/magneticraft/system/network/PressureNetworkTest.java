package committee.nova.mods.magneticraft.system.network;

import committee.nova.mods.magneticraft.system.network.pressure.PressureLink;
import committee.nova.mods.magneticraft.system.network.pressure.PressureGasStack;
import committee.nova.mods.magneticraft.system.network.pressure.PressureFluidConversion;
import committee.nova.mods.magneticraft.system.network.pressure.PressureLogisticsMath;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureNetworkTest {
    private static final ResourceLocation STEAM =
            ResourceLocation.fromNamespaceAndPath("magneticraft", "steam");
    private static final ResourceLocation NATURAL_GAS =
            ResourceLocation.fromNamespaceAndPath("magneticraft", "natural_gas");

    @Test
    void gasTransferIsConservativeWithoutLeak() {
        PressureNode source = new PressureNode(2.0, 1_000.0);
        PressureNode target = new PressureNode(1.0, 1_000.0);
        source.setPressureKpa(STEAM, 600.0);
        target.setPressureKpa(STEAM, 100.0);
        double before = source.gasKpaLiters() + target.gasKpaLiters();

        PressureLink.Transfer transfer = PressureLink.transfer(source, target, 1.0, 100.0, 0.0);

        assertTrue(transfer.moved());
        assertEquals(before, source.gasKpaLiters() + target.gasKpaLiters(), 1.0E-9);
        assertEquals(0.0, transfer.leaked(), 1.0E-9);
        assertTrue(source.pressureKpa() > target.pressureKpa());
    }

    @Test
    void explicitLeakAndCapacityAreAccountedFor() {
        PressureNode source = new PressureNode(1.0, 1_000.0);
        PressureNode target = new PressureNode(1.0, 150.0);
        source.setPressureKpa(STEAM, 900.0);
        double before = source.gasKpaLiters();

        PressureLink.Transfer transfer = PressureLink.transfer(source, target, 10.0, 1_000.0, 0.1);

        assertEquals(before - transfer.leaked(), source.gasKpaLiters() + target.gasKpaLiters(), 1.0E-9);
        assertTrue(target.pressureKpa() <= 150.0);
    }

    @Test
    void unlikeGasesNeverMixOrFlow() {
        PressureNode steam = new PressureNode(1.0D, 1_000.0D);
        PressureNode naturalGas = new PressureNode(1.0D, 1_000.0D);
        steam.setPressureKpa(STEAM, 600.0D);
        naturalGas.setPressureKpa(NATURAL_GAS, 100.0D);

        PressureLink.Transfer transfer = PressureLink.transfer(steam, naturalGas, 4.0D, 400.0D, 0.0D);

        assertFalse(transfer.moved());
        assertEquals(600.0D, steam.gasKpaLiters(), 1.0E-9D);
        assertEquals(100.0D, naturalGas.gasKpaLiters(), 1.0E-9D);
        assertEquals(0.0D, naturalGas.insert(new PressureGasStack(STEAM, 10.0D), false), 1.0E-9D);
    }

    @Test
    void emptyNodeAdoptsGasAndFullNodeRefusesOverflow() {
        PressureNode node = new PressureNode(2.0D, 100.0D);

        assertEquals(200.0D, node.insert(new PressureGasStack(STEAM, 250.0D), false), 1.0E-9D);
        assertEquals(STEAM, node.gasId().orElseThrow());
        assertEquals(0.0D, node.insert(new PressureGasStack(STEAM, 1.0D), false), 1.0E-9D);
        assertEquals(200.0D, node.extract(STEAM, 250.0D, false).gasKpaLiters(), 1.0E-9D);
        assertTrue(node.gasId().isEmpty());
    }

    @Test
    void fluidRoundingLeavesSubMillibucketRemainderInNode() {
        double gas = PressureFluidConversion.toGasKpaLiters(10) + 0.1D;

        assertEquals(10, PressureFluidConversion.wholeMillibuckets(gas));
        assertEquals(
                0.1D,
                gas - PressureFluidConversion.toGasKpaLiters(
                        PressureFluidConversion.wholeMillibuckets(gas)
                ),
                1.0E-9D
        );
    }

    @Test
    void pneumaticSpeedAndConsumptionFollowPressureCurve() {
        assertEquals(2.0D, PressureLogisticsMath.progressPerTick(0.0D), 1.0E-9D);
        assertEquals(5.0D, PressureLogisticsMath.progressPerTick(50.0D), 1.0E-9D);
        assertEquals(8.0D, PressureLogisticsMath.progressPerTick(100.0D), 1.0E-9D);
        assertEquals(12.0D, PressureLogisticsMath.progressPerTick(250.0D), 1.0E-9D);
        assertEquals(16.0D, PressureLogisticsMath.progressPerTick(400.0D), 1.0E-9D);
        assertEquals(16.0D, PressureLogisticsMath.progressPerTick(1_000.0D), 1.0E-9D);
        assertEquals(0.25D, PressureLogisticsMath.segmentCostKpaLiters(8), 1.0E-9D);
        assertEquals(0.50D, PressureLogisticsMath.segmentCostKpaLiters(9), 1.0E-9D);
    }
}
