package committee.nova.mods.magneticraft.system.nuclear.safety;

import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearTerrainDamageTest {
    @Test
    void radiusIsDeterministicBoundedAndAffectedByContainment() {
        ReactorParameters p = ReactorParameters.DEFAULT;
        int contained = NuclearTerrainDamage.radius(p.accidentTerrainDamageEnergyJoules(), 1.0D, p, 1.0D);
        int breached = NuclearTerrainDamage.radius(p.accidentTerrainDamageEnergyJoules(), 0.0D, p, 1.0D);
        assertTrue(contained >= 6 && contained <= 24);
        assertTrue(breached > contained && breached <= 24);
        assertEquals(breached, NuclearTerrainDamage.radius(
                p.accidentTerrainDamageEnergyJoules(), 0.0D, p, 1.0D));
    }
}
