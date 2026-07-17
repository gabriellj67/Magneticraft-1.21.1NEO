package committee.nova.mods.magneticraft.system.nuclear.radiation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiationExposureTest {
    @Test
    void radiationAndContaminationAccumulateSeparatelyAndProtectionAttenuatesBoth() {
        RadiationExposure result = RadiationExposure.ZERO.accumulate(
                new RadiationReading(100.0D, 20.0D), 1.0D, 0.75D);
        assertEquals(25.0D, result.cumulativeDoseMillisieverts());
        assertEquals(5.0D, result.contaminationMillisieverts());
        assertEquals(25.0D, result.lastDoseRateMillisievertsPerHour());
    }

    @Test
    void decontaminationDoesNotEraseRadiationDose() {
        RadiationExposure exposure = new RadiationExposure(42, 7, 3, 2).decontaminated();
        assertEquals(42.0D, exposure.cumulativeDoseMillisieverts());
        assertEquals(0.0D, exposure.contaminationMillisieverts());
    }

    @Test
    void schemaRoundTripsAndRejectsUnknownVersion() {
        RadiationExposure exposure = new RadiationExposure(1, 2, 3, 4);
        assertEquals(exposure, RadiationExposure.load(exposure.save()).orElseThrow());
        var tag = exposure.save();
        tag.putInt("schema_version", 99);
        assertTrue(RadiationExposure.load(tag).isEmpty());
    }
}
