package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PumpjackStateTest {
    private static final BlockPos DEPOSIT_ORIGIN = new BlockPos(12, 20, -8);
    private static final BlockPos TARGET_SOURCE = new BlockPos(16, 21, -3);

    @Test
    void defaultStateWritesTheCurrentLocalSchema() {
        PumpjackState state = new PumpjackState();
        CompoundTag tag = state.save();

        assertEquals(PumpjackState.SCHEMA_VERSION, tag.getInt("schema_version"));
        assertEquals("searching_oil", tag.getString("phase"));
        assertFalse(tag.contains("deposit_origin"));
        assertFalse(tag.contains("target_source"));
        assertDefault(state);
    }

    @Test
    void everyPhaseRoundTripsItsCursorCadencePositionsAndCounts() {
        for (PumpjackState.Phase phase : PumpjackState.Phase.values()) {
            PumpjackState original = stateIn(phase);

            PumpjackState restored = PumpjackState.load(original.save());

            assertEquals(original.phase(), restored.phase());
            assertEquals(original.cursorIndex(), restored.cursorIndex());
            assertEquals(original.phaseTicks(), restored.phaseTicks());
            assertEquals(original.depositOrigin(), restored.depositOrigin());
            assertEquals(original.depositSize(), restored.depositSize());
            assertEquals(original.depositRemainingSources(), restored.depositRemainingSources());
            assertEquals(original.targetSource(), restored.targetSource());
        }
    }

    @Test
    void futureMissingAndMalformedEnvelopesResetOnlyThisState() {
        CompoundTag missingVersion = stateIn(PumpjackState.Phase.EXTRACTING).save();
        missingVersion.remove("schema_version");
        assertDefault(PumpjackState.load(missingVersion));

        CompoundTag futureVersion = stateIn(PumpjackState.Phase.EXTRACTING).save();
        futureVersion.putInt("schema_version", PumpjackState.SCHEMA_VERSION + 1);
        assertDefault(PumpjackState.load(futureVersion));

        CompoundTag unknownPhase = stateIn(PumpjackState.Phase.EXTRACTING).save();
        unknownPhase.putString("phase", "teleporting_oil");
        assertDefault(PumpjackState.load(unknownPhase));

        CompoundTag negativeCursor = stateIn(PumpjackState.Phase.SEARCHING_DEPOSIT).save();
        negativeCursor.putInt("cursor_index", -1);
        assertDefault(PumpjackState.load(negativeCursor));

        CompoundTag oversizedCursor = stateIn(PumpjackState.Phase.SEARCHING_DEPOSIT).save();
        oversizedCursor.putInt("cursor_index", PumpjackCursor.DEPOSIT_SCAN_MAX_POSITIONS + 1);
        assertDefault(PumpjackState.load(oversizedCursor));

        CompoundTag invalidCadence = stateIn(PumpjackState.Phase.EXTRACTING).save();
        invalidCadence.putInt("phase_ticks", PumpjackCursor.EXTRACTION_INTERVAL_TICKS);
        assertDefault(PumpjackState.load(invalidCadence));

        CompoundTag invalidCounts = stateIn(PumpjackState.Phase.DIGGING).save();
        invalidCounts.putInt("deposit_remaining_sources", invalidCounts.getInt("deposit_size") + 1);
        assertDefault(PumpjackState.load(invalidCounts));

        CompoundTag wrongPositionType = stateIn(PumpjackState.Phase.DIGGING).save();
        wrongPositionType.putString("deposit_origin", "not_a_position");
        assertDefault(PumpjackState.load(wrongPositionType));
    }

    @Test
    void phasesThatNeedWorldPositionsFailClosedWhenThosePositionsAreMissing() {
        CompoundTag missingDeposit = stateIn(PumpjackState.Phase.DIGGING).save();
        missingDeposit.remove("deposit_origin");
        assertDefault(PumpjackState.load(missingDeposit));

        CompoundTag missingTarget = stateIn(PumpjackState.Phase.EXTRACTING).save();
        missingTarget.remove("target_source");
        assertDefault(PumpjackState.load(missingTarget));
    }

    @Test
    void transitionsResetPhaseLocalProgressAndClearStaleTargets() {
        PumpjackState state = stateIn(PumpjackState.Phase.EXTRACTING);

        state.transitionTo(PumpjackState.Phase.SEARCHING_DEPOSIT);

        assertEquals(PumpjackState.Phase.SEARCHING_DEPOSIT, state.phase());
        assertEquals(0, state.cursorIndex());
        assertEquals(0, state.phaseTicks());
        assertEquals(0, state.depositSize());
        assertEquals(0, state.depositRemainingSources());
        assertEquals(Optional.of(DEPOSIT_ORIGIN), state.depositOrigin());
        assertTrue(state.targetSource().isEmpty());

        state.transitionTo(PumpjackState.Phase.SEARCHING_OIL);
        assertDefault(state);
    }

    @Test
    void cadenceWrapsOnlyAfterTwentyTicksInCadencedPhases() {
        PumpjackState state = stateIn(PumpjackState.Phase.DIGGING);
        state.setPhaseTicks(0);

        for (int tick = 1; tick < PumpjackCursor.EXTRACTION_INTERVAL_TICKS; tick++) {
            assertFalse(state.advancePhaseTick());
            assertEquals(tick, state.phaseTicks());
        }
        assertTrue(state.advancePhaseTick());
        assertEquals(0, state.phaseTicks());

        state.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
        assertThrows(IllegalStateException.class, state::advancePhaseTick);
    }

    @Test
    void mutatorsRejectStateThatCannotBeSafelyPersisted() {
        PumpjackState state = new PumpjackState();

        assertThrows(IllegalStateException.class,
                () -> state.transitionTo(PumpjackState.Phase.DIGGING));
        assertThrows(IllegalArgumentException.class, () -> state.setCursorIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> state.setPhaseTicks(1));
        assertThrows(IllegalArgumentException.class, () -> state.setDepositCounts(1, 2));

        state.setDepositOrigin(DEPOSIT_ORIGIN);
        state.transitionTo(PumpjackState.Phase.SEARCHING_SOURCE);
        assertThrows(IllegalStateException.class,
                () -> state.transitionTo(PumpjackState.Phase.EXTRACTING));
    }

    private static PumpjackState stateIn(PumpjackState.Phase phase) {
        PumpjackState state = new PumpjackState();
        if (phase == PumpjackState.Phase.SEARCHING_OIL) {
            state.setCursorIndex(23);
            return state;
        }

        state.setDepositOrigin(DEPOSIT_ORIGIN);
        state.setDepositCounts(12, 7);
        if (phase == PumpjackState.Phase.EXTRACTING) {
            state.setTargetSource(TARGET_SOURCE);
        }
        state.transitionTo(phase);
        state.setDepositCounts(12, 7);
        if (phase == PumpjackState.Phase.DIGGING || phase == PumpjackState.Phase.EXTRACTING) {
            state.setPhaseTicks(17);
        }
        if (phase != PumpjackState.Phase.EXTRACTING) {
            state.setCursorIndex(23);
        }
        return state;
    }

    private static void assertDefault(PumpjackState state) {
        assertEquals(PumpjackState.Phase.SEARCHING_OIL, state.phase());
        assertEquals(0, state.cursorIndex());
        assertEquals(0, state.phaseTicks());
        assertTrue(state.depositOrigin().isEmpty());
        assertEquals(0, state.depositSize());
        assertEquals(0, state.depositRemainingSources());
        assertTrue(state.targetSource().isEmpty());
    }
}
