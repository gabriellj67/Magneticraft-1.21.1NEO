package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OilDepositPersistenceTest {
    @Test
    void currentSchemaRoundTripPreservesAndBoundsReserve() {
        CompoundTag tag = new CompoundTag();
        OilDepositPersistence.write(tag, 7_500);

        assertEquals(2, tag.getInt("schema_version"));
        assertEquals(7_500, OilDepositPersistence.read(tag));

        OilDepositPersistence.write(tag, -1);
        assertEquals(0, OilDepositPersistence.read(tag));

        OilDepositPersistence.write(tag, Integer.MAX_VALUE);
        assertEquals(10_000, OilDepositPersistence.read(tag));

        tag.putInt("remaining_millibuckets", Integer.MAX_VALUE);
        assertEquals(10_000, OilDepositPersistence.read(tag));
    }

    @Test
    void missingAndUnsupportedSchemasResetWithoutReadingReserve() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("remaining_millibuckets", 125_000);
        assertEquals(10_000, OilDepositPersistence.read(legacyTag));

        CompoundTag futureTag = new CompoundTag();
        OilDepositPersistence.write(futureTag, 7_500);
        futureTag.putInt("schema_version", 3);
        assertEquals(10_000, OilDepositPersistence.read(futureTag));

        CompoundTag incompleteCurrentTag = new CompoundTag();
        incompleteCurrentTag.putInt("schema_version", 2);
        assertEquals(10_000, OilDepositPersistence.read(incompleteCurrentTag));
    }

    @Test
    void schemaOneMigratesEveryLegacyOilAmountStageWithoutChangingItsPercentage() {
        assertEquals(10, OilDepositBlockEntity.EXTRACTION_STAGES);
        assertEquals(1_000, OilDepositBlockEntity.EXTRACTION_STAGE_MILLIBUCKETS);
        assertEquals(10_000, OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS);

        for (int extractedStages = 0; extractedStages <= OilDepositBlockEntity.EXTRACTION_STAGES;
             extractedStages++) {
            CompoundTag legacy = new CompoundTag();
            legacy.putInt("schema_version", 1);
            legacy.putInt("remaining_millibuckets", 4_000_000 - extractedStages * 400_000);

            assertEquals(
                    10_000 - extractedStages * OilDepositBlockEntity.EXTRACTION_STAGE_MILLIBUCKETS,
                    OilDepositPersistence.read(legacy)
            );
        }
    }
}
