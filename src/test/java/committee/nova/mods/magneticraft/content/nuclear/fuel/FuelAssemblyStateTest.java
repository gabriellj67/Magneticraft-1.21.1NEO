package committee.nova.mods.magneticraft.content.nuclear.fuel;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuelAssemblyStateTest {
    @Test
    void roundTripPreservesEveryDurableColumnField() {
        FuelAssemblyState original = new FuelAssemblyState(
                NuclearFuelGrade.STANDARD_ENRICHMENT.definitionId(),
                0.42D,
                0.31D,
                12_345.5D,
                873.15D,
                0.76D,
                99_000L
        );

        assertEquals(original, FuelAssemblyState.load(original.save()).orElseThrow());
    }

    @Test
    void missingOrFutureSchemaNeverResetsToFreshFuel() {
        assertTrue(FuelAssemblyState.load(new CompoundTag()).isEmpty());
        CompoundTag future = FuelAssemblyState.fresh(NuclearFuelGrade.LOW_ENRICHMENT, 10L).save();
        future.putInt("schema_version", FuelAssemblyState.SCHEMA_VERSION + 1);
        assertTrue(FuelAssemblyState.load(future).isEmpty());
    }

    @Test
    void constructorRejectsOutOfRangeSafetyState() {
        assertThrows(IllegalArgumentException.class, () -> new FuelAssemblyState(
                NuclearFuelGrade.HIGH_ENRICHMENT.definitionId(),
                1.01D,
                0.0D,
                0.0D,
                293.15D,
                1.0D,
                0L
        ));
    }
}
