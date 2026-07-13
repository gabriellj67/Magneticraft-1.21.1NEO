package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OilDepositPersistenceTest {
    @Test
    void currentSchemaRoundTripPreservesAndBoundsReserve() {
        CompoundTag tag = new CompoundTag();
        OilDepositPersistence.write(tag, 125_000);

        assertEquals(1, tag.getInt("schema_version"));
        assertEquals(125_000, OilDepositPersistence.read(tag));

        OilDepositPersistence.write(tag, -1);
        assertEquals(0, OilDepositPersistence.read(tag));

        OilDepositPersistence.write(tag, Integer.MAX_VALUE);
        assertEquals(4_000_000, OilDepositPersistence.read(tag));

        tag.putInt("remaining_millibuckets", Integer.MAX_VALUE);
        assertEquals(4_000_000, OilDepositPersistence.read(tag));
    }

    @Test
    void missingAndUnsupportedSchemasResetWithoutReadingReserve() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("remaining_millibuckets", 125_000);
        assertEquals(4_000_000, OilDepositPersistence.read(legacyTag));

        CompoundTag futureTag = new CompoundTag();
        OilDepositPersistence.write(futureTag, 125_000);
        futureTag.putInt("schema_version", 2);
        assertEquals(4_000_000, OilDepositPersistence.read(futureTag));

        CompoundTag incompleteCurrentTag = new CompoundTag();
        incompleteCurrentTag.putInt("schema_version", 1);
        assertEquals(4_000_000, OilDepositPersistence.read(incompleteCurrentTag));
    }
}
