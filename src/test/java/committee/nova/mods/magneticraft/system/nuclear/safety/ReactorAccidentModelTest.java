package committee.nova.mods.magneticraft.system.nuclear.safety;

import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorAccidentStage;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorAccidentModelTest {
    @Test
    void warningStagesAreDeterministicAndRecoverableBeforeDamage() {
        ReactorParameters p = ReactorParameters.DEFAULT;
        var shortage = ReactorAccidentModel.step(input(ReactorAccidentStage.NORMAL,
                1000, 0.1, 500, 1, 15.5, 1, 1, 0), p);
        assertEquals(ReactorAccidentStage.COOLING_SHORTAGE, shortage.stage());

        var recovered = ReactorAccidentModel.step(input(shortage.stage(),
                1000, 1.0, 500, 1, shortage.pressureMegapascals(), 1, 1, 0), p);
        assertEquals(ReactorAccidentStage.NORMAL, recovered.stage());
    }

    @Test
    void damageProgressionCannotBeReversedByChunkUnloadingOrCooling() {
        ReactorParameters p = ReactorParameters.DEFAULT;
        var damaged = ReactorAccidentModel.step(input(ReactorAccidentStage.NORMAL,
                1000, 0.1, 800, 0.2, 15.5, 1, 1, 0), p);
        assertEquals(ReactorAccidentStage.CLADDING_DAMAGE, damaged.stage());

        var cooled = ReactorAccidentModel.step(input(damaged.stage(),
                0, 1, 300, 0.2, 0.1, 1, 1, 0), p);
        assertEquals(ReactorAccidentStage.CLADDING_DAMAGE, cooled.stage());
    }

    @Test
    void pressureDamagesVesselThenContainmentWithoutRandomTransition() {
        ReactorParameters p = ReactorParameters.DEFAULT;
        var result = ReactorAccidentModel.step(input(ReactorAccidentStage.FUEL_MELT,
                2_000_000, 0, 3_000, 0, p.vesselBurstPressureMegapascals(), 1, 0.000001, 0), p);
        assertEquals(ReactorAccidentStage.CONTAINMENT_BREACH, result.stage());
        assertEquals(0.0D, result.vesselIntegrity());
        assertEquals(0.0D, result.containmentIntegrity());
        assertTrue(result.accidentEnergyJoules() > 0.0D);
    }

    private static ReactorAccidentModel.Input input(
            ReactorAccidentStage stage, double heat, double coolant, double temperature,
            double cladding, double pressure, double vessel, double containment, double energy
    ) {
        return new ReactorAccidentModel.Input(
                stage, heat, coolant, temperature, cladding, pressure, vessel, containment, energy);
    }
}
