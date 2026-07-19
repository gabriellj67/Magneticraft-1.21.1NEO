package committee.nova.mods.magneticraft.content.nuclear.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReactorOperatorStatusTest {
    @Test
    void groupsDetailedAccidentStagesIntoFourOperatorStates() {
        assertEquals(ReactorOperatorStatus.NORMAL,
                ReactorOperatorStatus.from(ReactorAccidentStage.NORMAL));
        assertEquals(ReactorOperatorStatus.COOLING_WARNING,
                ReactorOperatorStatus.from(ReactorAccidentStage.COOLING_SHORTAGE));
        assertEquals(ReactorOperatorStatus.COOLING_WARNING,
                ReactorOperatorStatus.from(ReactorAccidentStage.LOCAL_BOILING));
        assertEquals(ReactorOperatorStatus.CORE_DAMAGE,
                ReactorOperatorStatus.from(ReactorAccidentStage.CLADDING_DAMAGE));
        assertEquals(ReactorOperatorStatus.MELTDOWN_OR_RELEASE,
                ReactorOperatorStatus.from(ReactorAccidentStage.FUEL_MELT));
        assertEquals(ReactorOperatorStatus.MELTDOWN_OR_RELEASE,
                ReactorOperatorStatus.from(ReactorAccidentStage.PRESSURE_RISE));
        assertEquals(ReactorOperatorStatus.MELTDOWN_OR_RELEASE,
                ReactorOperatorStatus.from(ReactorAccidentStage.VESSEL_BREACH));
        assertEquals(ReactorOperatorStatus.MELTDOWN_OR_RELEASE,
                ReactorOperatorStatus.from(ReactorAccidentStage.CONTAINMENT_BREACH));
    }
}
