package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OilDepositSavedDataTest {
    private static final BlockPos DEPOSIT_POSITION = new BlockPos(17, 28, -9);
    private static final BlockPos FIELD_ORIGIN = new BlockPos(16, 24, -16);

    @Test
    void registerPreservesRemainingReserveAndFieldOrigin() {
        OilDepositSavedData data = new OilDepositSavedData();

        assertEquals(4_000, data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 4_000));
        assertEquals(
                new OilDepositSavedData.Deposit(FIELD_ORIGIN, 4_000),
                data.depositAt(DEPOSIT_POSITION)
        );

        BlockPos replacementOrigin = FIELD_ORIGIN.offset(32, 0, 0);
        assertEquals(4_000, data.register(DEPOSIT_POSITION, replacementOrigin, 9_000));
        assertEquals(FIELD_ORIGIN, data.depositAt(DEPOSIT_POSITION).fieldOrigin());
        assertEquals(1, data.size());
    }

    @Test
    void simulatedDrainHasNoSideEffectsForRegisteredOrFallbackDeposits() {
        OilDepositSavedData data = new OilDepositSavedData();
        data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 4_000);
        CompoundTag before = data.save(new CompoundTag());

        assertEquals(750, data.drain(DEPOSIT_POSITION, FIELD_ORIGIN, 4_000, 750, true));
        assertEquals(before, data.save(new CompoundTag()));

        BlockPos unregistered = DEPOSIT_POSITION.offset(1, 0, 0);
        assertEquals(250, data.drain(unregistered, FIELD_ORIGIN, 1_000, 250, true));
        assertNull(data.depositAt(unregistered));
        assertEquals(1, data.size());
    }

    @Test
    void executedDrainNeverExceedsRequestOrRemainingReserve() {
        OilDepositSavedData data = new OilDepositSavedData();
        data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 1_000);

        assertEquals(0, data.drain(DEPOSIT_POSITION, FIELD_ORIGIN, 1_000, -1, false));
        assertEquals(1_000, data.depositAt(DEPOSIT_POSITION).remainingMillibuckets());
        assertEquals(600, data.drain(DEPOSIT_POSITION, FIELD_ORIGIN, 1_000, 600, false));
        assertEquals(400, data.depositAt(DEPOSIT_POSITION).remainingMillibuckets());
        assertEquals(400, data.drain(DEPOSIT_POSITION, FIELD_ORIGIN, 1_000, 900, false));
        assertEquals(0, data.depositAt(DEPOSIT_POSITION).remainingMillibuckets());
        assertEquals(0, data.drain(DEPOSIT_POSITION, FIELD_ORIGIN, 1_000, 1, false));
    }

    @Test
    void removeReportsWhetherARegisteredDepositWasRemoved() {
        OilDepositSavedData data = new OilDepositSavedData();
        data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 4_000);

        assertTrue(data.remove(DEPOSIT_POSITION));
        assertNull(data.depositAt(DEPOSIT_POSITION));
        assertEquals(0, data.size());
        assertFalse(data.remove(DEPOSIT_POSITION));
    }

    @Test
    void currentSchemaRoundTripPreservesDepositsAndOrigins() {
        OilDepositSavedData data = new OilDepositSavedData();
        BlockPos otherPosition = new BlockPos(-31, 12, 48);
        BlockPos otherOrigin = new BlockPos(-32, 8, 48);
        data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 4_000);
        data.register(otherPosition, otherOrigin, 7_500);

        CompoundTag encoded = data.save(new CompoundTag());
        OilDepositSavedData restored = OilDepositSavedData.load(encoded);

        assertEquals(OilDepositSavedData.SCHEMA_VERSION, encoded.getInt("schema_version"));
        assertEquals(2, restored.size());
        assertEquals(
                new OilDepositSavedData.Deposit(FIELD_ORIGIN, 4_000),
                restored.depositAt(DEPOSIT_POSITION)
        );
        assertEquals(
                new OilDepositSavedData.Deposit(otherOrigin, 7_500),
                restored.depositAt(otherPosition)
        );
        assertEquals(encoded, restored.save(new CompoundTag()));
    }

    @Test
    void schemaOneMigratesEveryLegacyOilAmountStageWithoutOverridingBlockEntityMigration() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("schema_version", 1);
        ListTag deposits = new ListTag();
        for (int extractedStages = 0; extractedStages <= OilDepositBlockEntity.EXTRACTION_STAGES;
             extractedStages++) {
            CompoundTag deposit = new CompoundTag();
            deposit.putLong("position", DEPOSIT_POSITION.offset(extractedStages, 0, 0).asLong());
            deposit.putLong("field_origin", FIELD_ORIGIN.asLong());
            deposit.putInt("remaining_millibuckets", 4_000_000 - extractedStages * 400_000);
            deposits.add(deposit);
        }
        legacy.put("deposits", deposits);

        OilDepositSavedData restored = OilDepositSavedData.load(legacy);

        assertEquals(OilDepositBlockEntity.EXTRACTION_STAGES + 1, restored.size());
        for (int extractedStages = 0; extractedStages <= OilDepositBlockEntity.EXTRACTION_STAGES;
             extractedStages++) {
            assertEquals(
                    10_000 - extractedStages * OilDepositBlockEntity.EXTRACTION_STAGE_MILLIBUCKETS,
                    restored.depositAt(DEPOSIT_POSITION.offset(extractedStages, 0, 0)).remainingMillibuckets()
            );
        }
        assertEquals(OilDepositSavedData.SCHEMA_VERSION,
                restored.save(new CompoundTag()).getInt("schema_version"));
    }

    @Test
    void malformedSchemasAndDepositEntriesFailClosed() {
        CompoundTag versionless = new CompoundTag();
        versionless.put("deposits", new ListTag());
        assertEquals(0, OilDepositSavedData.load(versionless).size());

        CompoundTag futureSchema = new CompoundTag();
        futureSchema.putInt("schema_version", OilDepositSavedData.SCHEMA_VERSION + 1);
        futureSchema.put("deposits", new ListTag());
        assertEquals(0, OilDepositSavedData.load(futureSchema).size());

        CompoundTag wrongContainerType = new CompoundTag();
        wrongContainerType.putInt("schema_version", OilDepositSavedData.SCHEMA_VERSION);
        wrongContainerType.putString("deposits", "not_a_list");
        assertEquals(0, OilDepositSavedData.load(wrongContainerType).size());

        CompoundTag malformedEntries = new CompoundTag();
        malformedEntries.putInt("schema_version", OilDepositSavedData.SCHEMA_VERSION);
        ListTag deposits = new ListTag();
        CompoundTag missingOrigin = new CompoundTag();
        missingOrigin.putLong("position", DEPOSIT_POSITION.asLong());
        missingOrigin.putInt("remaining_millibuckets", 1_000);
        deposits.add(missingOrigin);
        CompoundTag wrongPositionType = new CompoundTag();
        wrongPositionType.putString("position", "invalid");
        wrongPositionType.putLong("field_origin", FIELD_ORIGIN.asLong());
        wrongPositionType.putInt("remaining_millibuckets", 1_000);
        deposits.add(wrongPositionType);
        CompoundTag negativeReserve = new CompoundTag();
        negativeReserve.putLong("position", DEPOSIT_POSITION.asLong());
        negativeReserve.putLong("field_origin", FIELD_ORIGIN.asLong());
        negativeReserve.putInt("remaining_millibuckets", -1);
        deposits.add(negativeReserve);
        malformedEntries.put("deposits", deposits);

        OilDepositSavedData restored = OilDepositSavedData.load(malformedEntries);
        assertEquals(1, restored.size());
        assertEquals(
                new OilDepositSavedData.Deposit(FIELD_ORIGIN, 0),
                restored.depositAt(DEPOSIT_POSITION)
        );
    }

    @Test
    void surveyAggregatesOneFieldAndSelectsTheNearestOriginDeterministically() {
        OilDepositSavedData data = new OilDepositSavedData();
        BlockPos fartherOrigin = new BlockPos(80, 20, 0);
        data.register(new BlockPos(80, 12, 0), fartherOrigin, 9_000);
        data.register(new BlockPos(16, 18, 0), FIELD_ORIGIN, 7_500);
        data.register(new BlockPos(24, 42, 0), FIELD_ORIGIN, 2_500);

        OilFieldSurvey survey = data.survey(BlockPos.ZERO, 128).orElseThrow();

        assertEquals(FIELD_ORIGIN, survey.origin());
        assertEquals(18, survey.minimumY());
        assertEquals(42, survey.maximumY());
        assertEquals(10_000L, survey.remainingMillibuckets());
        assertEquals(20_000L, survey.knownCapacityMillibuckets());
        assertEquals(2, survey.depositCount());
        assertEquals(50, survey.remainingPercent());
        assertFalse(survey.depleted());
    }

    @Test
    void surveyIsReadOnlyIncludesDepletedFieldsAndHonorsHorizontalRadius() {
        OilDepositSavedData data = new OilDepositSavedData();
        data.register(DEPOSIT_POSITION, FIELD_ORIGIN, 0);
        CompoundTag before = data.save(new CompoundTag());

        OilFieldSurvey depleted = data.survey(BlockPos.ZERO, 128).orElseThrow();

        assertTrue(depleted.depleted());
        assertEquals(0, depleted.remainingPercent());
        assertEquals(before, data.save(new CompoundTag()));
        assertTrue(data.survey(new BlockPos(512, 0, 512), 128).isEmpty());
    }
}
