package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpentFuelHandlingStatusTest {
    @Test
    void exposesTransferAndEncapsulationAsSeparateMilestones() {
        assertEquals(SpentFuelHandlingStatus.EMPTY,
                SpentFuelHandlingStatus.from(0, 0, 0, false));
        assertEquals(SpentFuelHandlingStatus.DANGEROUS,
                SpentFuelHandlingStatus.from(2, 0, 0, false));
        assertEquals(SpentFuelHandlingStatus.COOLING,
                SpentFuelHandlingStatus.from(2, 0, 0, true));
        assertEquals(SpentFuelHandlingStatus.TRANSFERABLE,
                SpentFuelHandlingStatus.from(2, 1, 0, true));
        assertEquals(SpentFuelHandlingStatus.SEALABLE,
                SpentFuelHandlingStatus.from(2, 2, 2, false));
    }
}
