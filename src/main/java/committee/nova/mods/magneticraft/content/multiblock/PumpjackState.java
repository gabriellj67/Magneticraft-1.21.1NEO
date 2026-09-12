package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;
import java.util.Optional;

/** Durable, loader-independent state for the pumpjack's five-phase process. */
final class PumpjackState {
    static final int SCHEMA_VERSION = 1;

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String PHASE_TAG = "phase";
    private static final String CURSOR_INDEX_TAG = "cursor_index";
    private static final String PHASE_TICKS_TAG = "phase_ticks";
    private static final String DEPOSIT_ORIGIN_TAG = "deposit_origin";
    private static final String DEPOSIT_SIZE_TAG = "deposit_size";
    private static final String DEPOSIT_REMAINING_SOURCES_TAG = "deposit_remaining_sources";
    private static final String TARGET_SOURCE_TAG = "target_source";

    private Phase phase;
    private int cursorIndex;
    private int phaseTicks;
    private BlockPos depositOrigin;
    private int depositSize;
    private int depositRemainingSources;
    private BlockPos targetSource;

    PumpjackState() {
        reset();
    }

    static PumpjackState load(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        PumpjackState state = new PumpjackState();
        if (!hasRequiredFields(tag) || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION) {
            return state;
        }

        Phase loadedPhase = Phase.bySerializedName(tag.getString(PHASE_TAG)).orElse(null);
        int loadedCursorIndex = tag.getInt(CURSOR_INDEX_TAG);
        int loadedPhaseTicks = tag.getInt(PHASE_TICKS_TAG);
        int loadedDepositSize = tag.getInt(DEPOSIT_SIZE_TAG);
        int loadedRemainingSources = tag.getInt(DEPOSIT_REMAINING_SOURCES_TAG);
        if (loadedPhase == null
                || !validCursorIndex(loadedPhase, loadedCursorIndex)
                || !validPhaseTicks(loadedPhase, loadedPhaseTicks)
                || !validDepositCounts(loadedDepositSize, loadedRemainingSources)
                || invalidOptionalPosition(tag, DEPOSIT_ORIGIN_TAG)
                || invalidOptionalPosition(tag, TARGET_SOURCE_TAG)) {
            return state;
        }

        BlockPos loadedDepositOrigin = tag.contains(DEPOSIT_ORIGIN_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(DEPOSIT_ORIGIN_TAG))
                : null;
        BlockPos loadedTargetSource = tag.contains(TARGET_SOURCE_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(TARGET_SOURCE_TAG))
                : null;
        if (loadedPhase != Phase.SEARCHING_OIL && loadedDepositOrigin == null
                || loadedPhase == Phase.EXTRACTING && loadedTargetSource == null) {
            return state;
        }

        state.phase = loadedPhase;
        state.cursorIndex = loadedCursorIndex;
        state.phaseTicks = loadedPhaseTicks;
        state.depositOrigin = immutableOrNull(loadedDepositOrigin);
        state.depositSize = loadedDepositSize;
        state.depositRemainingSources = loadedRemainingSources;
        state.targetSource = immutableOrNull(loadedTargetSource);
        return state;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putString(PHASE_TAG, phase.serializedName());
        tag.putInt(CURSOR_INDEX_TAG, cursorIndex);
        tag.putInt(PHASE_TICKS_TAG, phaseTicks);
        tag.putInt(DEPOSIT_SIZE_TAG, depositSize);
        tag.putInt(DEPOSIT_REMAINING_SOURCES_TAG, depositRemainingSources);
        if (depositOrigin != null) {
            tag.putLong(DEPOSIT_ORIGIN_TAG, depositOrigin.asLong());
        }
        if (targetSource != null) {
            tag.putLong(TARGET_SOURCE_TAG, targetSource.asLong());
        }
        return tag;
    }

    Phase phase() {
        return phase;
    }

    int cursorIndex() {
        return cursorIndex;
    }

    int phaseTicks() {
        return phaseTicks;
    }

    Optional<BlockPos> depositOrigin() {
        return Optional.ofNullable(depositOrigin);
    }

    int depositSize() {
        return depositSize;
    }

    int depositRemainingSources() {
        return depositRemainingSources;
    }

    Optional<BlockPos> targetSource() {
        return Optional.ofNullable(targetSource);
    }

    void transitionTo(Phase nextPhase) {
        Objects.requireNonNull(nextPhase, "nextPhase");
        if (nextPhase != Phase.SEARCHING_OIL && depositOrigin == null) {
            throw new IllegalStateException("Pumpjack deposit origin is required for " + nextPhase);
        }
        if (nextPhase == Phase.EXTRACTING && targetSource == null) {
            throw new IllegalStateException("Pumpjack target source is required for extraction");
        }
        phase = nextPhase;
        cursorIndex = 0;
        phaseTicks = 0;
        if (nextPhase == Phase.SEARCHING_OIL) {
            depositOrigin = null;
            depositSize = 0;
            depositRemainingSources = 0;
            targetSource = null;
        } else if (nextPhase == Phase.SEARCHING_DEPOSIT) {
            depositSize = 0;
            depositRemainingSources = 0;
            targetSource = null;
        } else if (nextPhase != Phase.EXTRACTING) {
            targetSource = null;
        }
    }

    void setCursorIndex(int cursorIndex) {
        if (!validCursorIndex(phase, cursorIndex)) {
            throw new IllegalArgumentException("Invalid pumpjack cursor index for " + phase + ": " + cursorIndex);
        }
        this.cursorIndex = cursorIndex;
    }

    void advanceCursor() {
        setCursorIndex(Math.addExact(cursorIndex, 1));
    }

    void setPhaseTicks(int phaseTicks) {
        if (!validPhaseTicks(phase, phaseTicks)) {
            throw new IllegalArgumentException("Invalid pumpjack phase tick for " + phase + ": " + phaseTicks);
        }
        this.phaseTicks = phaseTicks;
    }

    boolean advancePhaseTick() {
        if (phase != Phase.DIGGING && phase != Phase.EXTRACTING) {
            throw new IllegalStateException("Pumpjack phase does not use a tick cadence: " + phase);
        }
        phaseTicks++;
        if (phaseTicks < PumpjackCursor.EXTRACTION_INTERVAL_TICKS) {
            return false;
        }
        phaseTicks = 0;
        return true;
    }

    void setDepositOrigin(BlockPos depositOrigin) {
        this.depositOrigin = Objects.requireNonNull(depositOrigin, "depositOrigin").immutable();
    }

    void setDepositCounts(int depositSize, int remainingSources) {
        if (!validDepositCounts(depositSize, remainingSources)) {
            throw new IllegalArgumentException(
                    "Invalid pumpjack deposit counts: " + remainingSources + "/" + depositSize
            );
        }
        this.depositSize = depositSize;
        depositRemainingSources = remainingSources;
    }

    void setTargetSource(BlockPos targetSource) {
        this.targetSource = Objects.requireNonNull(targetSource, "targetSource").immutable();
    }

    void clearTargetSource() {
        targetSource = null;
    }

    void reset() {
        phase = Phase.SEARCHING_OIL;
        cursorIndex = 0;
        phaseTicks = 0;
        depositOrigin = null;
        depositSize = 0;
        depositRemainingSources = 0;
        targetSource = null;
    }

    private static boolean hasRequiredFields(CompoundTag tag) {
        return tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                && tag.contains(PHASE_TAG, Tag.TAG_STRING)
                && tag.contains(CURSOR_INDEX_TAG, Tag.TAG_INT)
                && tag.contains(PHASE_TICKS_TAG, Tag.TAG_INT)
                && tag.contains(DEPOSIT_SIZE_TAG, Tag.TAG_INT)
                && tag.contains(DEPOSIT_REMAINING_SOURCES_TAG, Tag.TAG_INT);
    }

    private static boolean invalidOptionalPosition(CompoundTag tag, String key) {
        return tag.contains(key) && !tag.contains(key, Tag.TAG_LONG);
    }

    private static boolean validCursorIndex(Phase phase, int cursorIndex) {
        return cursorIndex >= 0 && cursorIndex <= PumpjackCursor.maximumCursorIndex(phase);
    }

    private static boolean validPhaseTicks(Phase phase, int phaseTicks) {
        if (phaseTicks < 0 || phaseTicks >= PumpjackCursor.EXTRACTION_INTERVAL_TICKS) {
            return false;
        }
        return phase == Phase.DIGGING || phase == Phase.EXTRACTING || phaseTicks == 0;
    }

    private static boolean validDepositCounts(int depositSize, int remainingSources) {
        return depositSize >= 0
                && depositSize <= PumpjackCursor.DEPOSIT_SCAN_MAX_POSITIONS
                && remainingSources >= 0
                && remainingSources <= depositSize;
    }

    private static BlockPos immutableOrNull(BlockPos position) {
        return position == null ? null : position.immutable();
    }

    enum Phase {
        SEARCHING_OIL("searching_oil"),
        SEARCHING_DEPOSIT("searching_deposit"),
        DIGGING("digging"),
        EXTRACTING("extracting"),
        SEARCHING_SOURCE("searching_source");

        private final String serializedName;

        Phase(String serializedName) {
            this.serializedName = serializedName;
        }

        String serializedName() {
            return serializedName;
        }

        static Optional<Phase> bySerializedName(String serializedName) {
            for (Phase phase : values()) {
                if (phase.serializedName.equals(serializedName)) {
                    return Optional.of(phase);
                }
            }
            return Optional.empty();
        }
    }
}
