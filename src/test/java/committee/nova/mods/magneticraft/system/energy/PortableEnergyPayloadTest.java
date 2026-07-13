package committee.nova.mods.magneticraft.system.energy;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortableEnergyPayloadTest {
    @Test
    void currentSchemaRoundTripsAndClampsEnergy() {
        CompoundTag stored = PortableEnergyPayload.write(750);

        assertEquals(PortableEnergyPayload.SCHEMA_VERSION, stored.getInt("schema_version"));
        assertEquals(750, PortableEnergyPayload.readEnergyOrDefault(stored, 1_000));
        assertEquals(500, PortableEnergyPayload.readEnergyOrDefault(stored, 500));
    }

    @Test
    void legacyOrUnsupportedPayloadResetsInsteadOfBeingMigrated() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("energy", 750);
        assertEquals(0, PortableEnergyPayload.readEnergyOrDefault(legacy, 1_000));

        CompoundTag future = PortableEnergyPayload.write(750);
        future.putInt("schema_version", PortableEnergyPayload.SCHEMA_VERSION + 1);
        assertEquals(0, PortableEnergyPayload.readEnergyOrDefault(future, 1_000));
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PortableEnergyPayload.readEnergyOrDefault(PortableEnergyPayload.write(1), 0)
        );
    }
}
