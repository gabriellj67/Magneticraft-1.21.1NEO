package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;
import java.util.Optional;

/** Owns one bounded, persistable language runtime without retaining world objects. */
public final class ScriptRuntime {
    public static final int MAX_SOURCE_BYTES = 8_192;
    public static final int MAX_OUTPUT_CHARACTERS = 2_048;
    public static final int MAX_INSTRUCTIONS_PER_TICK = 64;
    public static final int MAX_DEVICE_CALLS_PER_TICK = 4;

    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String HAS_PROGRAM_TAG = "has_program";
    private static final String LANGUAGE_TAG = "language";
    private static final String SOURCE_TAG = "source";
    private static final String STATE_TAG = "state";

    private ScriptProgram program;
    private ScriptEngine engine;
    private VmFault terminalFault = VmFault.NONE;

    public void replaceProgram(ScriptProgram replacement) {
        Objects.requireNonNull(replacement, "replacement");
        ScriptEngine replacementEngine = createEngine(replacement);
        program = replacement;
        engine = replacementEngine;
        terminalFault = VmFault.NONE;
    }

    public int executeTick(ComputerDeviceBus bus) {
        if (engine == null || !engine.running()) {
            return 0;
        }
        DeviceBudget boundedBus = new DeviceBudget(Objects.requireNonNull(bus, "bus"));
        return engine.executeTick(boundedBus, MAX_INSTRUCTIONS_PER_TICK);
    }

    public boolean restore(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(HAS_PROGRAM_TAG, Tag.TAG_BYTE)) {
            rejectSnapshot();
            return false;
        }
        if (!tag.getBoolean(HAS_PROGRAM_TAG)) {
            reset();
            return true;
        }
        if (!tag.contains(LANGUAGE_TAG, Tag.TAG_STRING)
                || !tag.contains(SOURCE_TAG, Tag.TAG_STRING)
                || !tag.contains(STATE_TAG, Tag.TAG_COMPOUND)) {
            rejectSnapshot();
            return false;
        }
        Optional<ScriptLanguage> language = ScriptLanguage.parse(tag.getString(LANGUAGE_TAG));
        if (language.isEmpty()) {
            rejectSnapshot();
            return false;
        }
        try {
            ScriptProgram restoredProgram = new ScriptProgram(language.get(), tag.getString(SOURCE_TAG));
            ScriptEngine restoredEngine = createEngine(restoredProgram);
            if (!restoredEngine.restoreState(tag.getCompound(STATE_TAG))) {
                rejectSnapshot();
                return false;
            }
            program = restoredProgram;
            engine = restoredEngine;
            terminalFault = VmFault.NONE;
            return true;
        } catch (IllegalArgumentException exception) {
            rejectSnapshot();
            return false;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putBoolean(HAS_PROGRAM_TAG, engine != null && program != null);
        if (engine != null && program != null) {
            tag.putString(LANGUAGE_TAG, program.language().serializedName());
            tag.putString(SOURCE_TAG, program.source());
            tag.put(STATE_TAG, engine.saveState());
        }
        return tag;
    }

    public void reset() {
        program = null;
        engine = null;
        terminalFault = VmFault.NONE;
    }

    public boolean hasProgram() {
        return program != null && engine != null;
    }

    public Optional<ScriptProgram> program() {
        return Optional.ofNullable(program);
    }

    public boolean running() {
        return engine != null && engine.running();
    }

    public int programCounter() {
        return engine == null ? 0 : engine.programCounter();
    }

    public int lastResult() {
        return engine == null ? 0 : engine.lastResult();
    }

    public VmFault fault() {
        return engine == null ? terminalFault : engine.fault();
    }

    public String output() {
        return engine == null ? "" : engine.output();
    }

    public Optional<VirtualDisk> virtualDisk() {
        return engine instanceof ShellEngine shell ? Optional.of(shell.diskCopy()) : Optional.empty();
    }

    public boolean replaceVirtualDisk(VirtualDisk disk) {
        if (!(engine instanceof ShellEngine shell)) {
            return false;
        }
        shell.replaceDisk(Objects.requireNonNull(disk, "disk"));
        return true;
    }

    private static ScriptEngine createEngine(ScriptProgram program) {
        return switch (program.language()) {
            case FORTH -> new ForthEngine(program.source());
            case LISP -> new LispEngine(program.source());
            case SHELL -> new ShellEngine(program.source());
        };
    }

    private void rejectSnapshot() {
        program = null;
        engine = null;
        terminalFault = VmFault.INVALID_SNAPSHOT;
    }

    private static final class DeviceBudget implements ComputerDeviceBus {
        private final ComputerDeviceBus delegate;
        private int calls;

        private DeviceBudget(ComputerDeviceBus delegate) {
            this.delegate = delegate;
        }

        @Override
        public DeviceResult execute(DeviceCommand command, int argument) {
            if (calls >= MAX_DEVICE_CALLS_PER_TICK) {
                return DeviceResult.fault(VmFault.DEVICE_BUDGET_EXHAUSTED);
            }
            calls++;
            return delegate.execute(command, argument);
        }
    }
}
