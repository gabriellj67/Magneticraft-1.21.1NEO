package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Versioned codec for all program, filesystem and preset state owned by one floppy disk. */
final class FloppyDiskPersistence {
    static final String SCHEMA_VERSION_TAG = "schema_version";
    static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int SCRIPT_SCHEMA_VERSION = 1;
    private static final String PROGRAM_TAG = "program";
    private static final String SCRIPT_TAG = "script";
    private static final String LANGUAGE_TAG = "language";
    private static final String SOURCE_TAG = "source";
    private static final String DISK_TAG = "disk";
    private static final String PRESET_TAG = "preset";
    private static final String READ_ONLY_TAG = "read_only";

    private FloppyDiskPersistence() {
    }

    static void writeLegacy(CompoundTag tag, List<ComputerInstruction> program) {
        Objects.requireNonNull(tag, "tag");
        clearOwnedState(tag);
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ProgramNbt.writeProgram(tag, PROGRAM_TAG, program);
    }

    static void writeScript(CompoundTag tag, ScriptProgram program, Optional<VirtualDisk> disk) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(disk, "disk");
        if (disk.isPresent() && program.language() != ScriptLanguage.SHELL) {
            throw new IllegalArgumentException("Only shell media can own a virtual disk");
        }
        clearOwnedState(tag);
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.put(SCRIPT_TAG, writeScript(program));
        disk.ifPresent(value -> tag.put(DISK_TAG, value.save()));
    }

    static void writePreset(CompoundTag tag, String preset, @org.jetbrains.annotations.Nullable ScriptLanguage language) {
        Objects.requireNonNull(tag, "tag");
        String normalized = normalizePreset(preset);
        clearOwnedState(tag);
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putString(PRESET_TAG, normalized);
        tag.putBoolean(READ_ONLY_TAG, true);
        if (language != null) {
            tag.put(SCRIPT_TAG, writeScript(new ScriptProgram(language, "")));
            if (language == ScriptLanguage.SHELL) {
                VirtualDisk disk = new VirtualDisk();
                disk.format();
                disk.setLabel("Shell");
                tag.put(DISK_TAG, disk.save());
            }
        }
    }

    static Optional<State> read(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)) {
            return Optional.of(State.empty());
        }
        int schemaVersion = tag.getInt(SCHEMA_VERSION_TAG);
        if (schemaVersion == LEGACY_SCHEMA_VERSION) {
            return ProgramNbt.readProgram(tag, PROGRAM_TAG)
                    .map(program -> new State(Optional.of(program), Optional.empty(), Optional.empty(), "user", false));
        }
        if (schemaVersion != SCHEMA_VERSION) {
            return Optional.empty();
        }

        Optional<List<ComputerInstruction>> legacy = tag.contains(PROGRAM_TAG, Tag.TAG_LIST)
                ? ProgramNbt.readProgram(tag, PROGRAM_TAG)
                : Optional.empty();
        if (tag.contains(PROGRAM_TAG) && legacy.isEmpty()) {
            return Optional.empty();
        }
        Optional<ScriptProgram> script = Optional.empty();
        if (tag.contains(SCRIPT_TAG, Tag.TAG_COMPOUND)) {
            script = readScript(tag.getCompound(SCRIPT_TAG));
            if (script.isEmpty()) {
                return Optional.empty();
            }
        }
        if (legacy.isPresent() && script.isPresent()) {
            return Optional.empty();
        }

        Optional<VirtualDisk> disk = Optional.empty();
        if (tag.contains(DISK_TAG, Tag.TAG_COMPOUND)) {
            VirtualDisk restored = new VirtualDisk();
            if (!restored.restore(tag.getCompound(DISK_TAG))) {
                return Optional.empty();
            }
            disk = Optional.of(restored);
        }
        if (disk.isPresent() && (script.isEmpty() || script.get().language() != ScriptLanguage.SHELL)) {
            return Optional.empty();
        }

        String preset;
        try {
            preset = tag.contains(PRESET_TAG, Tag.TAG_STRING) ? normalizePreset(tag.getString(PRESET_TAG)) : "user";
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        boolean readOnly = tag.getBoolean(READ_ONLY_TAG);
        return Optional.of(new State(legacy, script, disk, preset, readOnly));
    }

    private static CompoundTag writeScript(ScriptProgram program) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCRIPT_SCHEMA_VERSION);
        tag.putString(LANGUAGE_TAG, program.language().serializedName());
        tag.putString(SOURCE_TAG, program.source());
        return tag;
    }

    private static Optional<ScriptProgram> readScript(CompoundTag tag) {
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCRIPT_SCHEMA_VERSION
                || !tag.contains(LANGUAGE_TAG, Tag.TAG_STRING)
                || !tag.contains(SOURCE_TAG, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Optional<ScriptLanguage> language = ScriptLanguage.parse(tag.getString(LANGUAGE_TAG));
        if (language.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ScriptProgram(language.get(), tag.getString(SOURCE_TAG)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String normalizePreset(String preset) {
        String normalized = Objects.requireNonNull(preset, "preset").toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{1,32}")) {
            throw new IllegalArgumentException("Invalid floppy preset");
        }
        return normalized;
    }

    private static void clearOwnedState(CompoundTag tag) {
        tag.remove(PROGRAM_TAG);
        tag.remove(SCRIPT_TAG);
        tag.remove(DISK_TAG);
        tag.remove(PRESET_TAG);
        tag.remove(READ_ONLY_TAG);
    }

    record State(
            Optional<List<ComputerInstruction>> legacyProgram,
            Optional<ScriptProgram> scriptProgram,
            Optional<VirtualDisk> disk,
            String preset,
            boolean readOnly
    ) {
        State {
            legacyProgram = Objects.requireNonNull(legacyProgram, "legacyProgram").map(List::copyOf);
            scriptProgram = Objects.requireNonNull(scriptProgram, "scriptProgram");
            disk = Objects.requireNonNull(disk, "disk").map(VirtualDisk::copy);
            preset = normalizePreset(preset);
            if (legacyProgram.isPresent() && scriptProgram.isPresent()) {
                throw new IllegalArgumentException("A floppy cannot contain two executable formats");
            }
            if (disk.isPresent() && (scriptProgram.isEmpty()
                    || scriptProgram.get().language() != ScriptLanguage.SHELL)) {
                throw new IllegalArgumentException("Only shell media can own a virtual disk");
            }
        }

        static State empty() {
            return new State(Optional.empty(), Optional.empty(), Optional.empty(), "user", false);
        }

        boolean executable() {
            return legacyProgram.isPresent() || scriptProgram.isPresent();
        }
    }
}
