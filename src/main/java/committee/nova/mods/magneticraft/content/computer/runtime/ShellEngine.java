package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus.DeviceCommand;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Shell 1.1 command dispatcher backed only by a bounded virtual disk and device bus. */
final class ShellEngine implements ScriptEngine {
    static final int MAX_COMMANDS = 256;
    static final int MAX_COMMAND_LENGTH = 512;

    private static final String PC_TAG = "program_counter";
    private static final String RUNNING_TAG = "running";
    private static final String FAULT_TAG = "fault";
    private static final String LAST_RESULT_TAG = "last_result";
    private static final String OUTPUT_TAG = "output";
    private static final String CURRENT_DIRECTORY_TAG = "current_directory";
    private static final String DISK_TAG = "disk";
    private static final String HELP = "help ls cd mkdir rm format free fs cat touch write update_disk quarry label";

    private final List<Command> commands;
    private final ScriptOutput output = new ScriptOutput();
    private VirtualDisk disk = new VirtualDisk();
    private String currentDirectory = "/";
    private int programCounter;
    private boolean running;
    private VmFault fault = VmFault.NONE;
    private int lastResult;

    ShellEngine(String source) {
        commands = parse(source);
        running = !commands.isEmpty();
    }

    @Override
    public int executeTick(ComputerDeviceBus bus, int instructionBudget) {
        if (!running || fault != VmFault.NONE || instructionBudget <= 0) {
            return 0;
        }
        if (programCounter < 0 || programCounter >= commands.size()) {
            running = false;
            return 0;
        }
        boolean complete = execute(commands.get(programCounter), bus);
        if (complete) {
            programCounter++;
            if (programCounter >= commands.size()) {
                running = false;
            }
        }
        return 1;
    }

    private boolean execute(Command command, ComputerDeviceBus bus) {
        List<String> arguments = command.arguments();
        switch (command.name()) {
            case "help" -> output.appendLine(HELP);
            case "format" -> {
                disk.format();
                currentDirectory = "/";
                output.appendLine("Disk formatted");
            }
            case "free" -> output.appendLine(disk.freeBytes() + " bytes free");
            case "fs" -> output.appendLine("label=" + disk.label() + " formatted=" + disk.formatted()
                    + " entries=" + disk.entryCount() + " used=" + disk.usedBytes());
            case "ls" -> {
                if (!requireFormatted()) {
                    break;
                }
                String path = arguments.isEmpty() ? "." : arguments.get(0);
                List<String> entries = disk.list(currentDirectory, path);
                output.appendLine(String.join(" ", entries));
            }
            case "cd" -> {
                if (!requireFormatted()) {
                    break;
                }
                Optional<String> target = disk.changeDirectory(currentDirectory, arguments.get(0));
                if (target.isPresent()) {
                    currentDirectory = target.get();
                    lastResult = 1;
                } else {
                    commandError("directory not found");
                }
            }
            case "mkdir" -> result(disk.mkdir(currentDirectory, arguments.get(0)), "unable to create directory");
            case "rm" -> result(disk.remove(currentDirectory, arguments.get(0)), "file or directory not found");
            case "cat" -> {
                if (!requireFormatted()) {
                    break;
                }
                Optional<String> content = disk.read(currentDirectory, arguments.get(0));
                if (content.isPresent()) {
                    output.appendLine(content.get());
                    lastResult = 1;
                } else {
                    commandError("file not found");
                }
            }
            case "touch" -> result(disk.touch(currentDirectory, arguments.get(0)), "unable to create file");
            case "write" -> result(
                    disk.write(currentDirectory, arguments.get(0), String.join(" ", arguments.subList(1, arguments.size()))),
                    "unable to write file"
            );
            case "update_disk" -> result(disk.formatted(), "disk not formatted");
            case "label" -> result(disk.setLabel(arguments.get(0)), "invalid disk label");
            case "quarry" -> {
                int size = parseQuarrySize(arguments.get(0));
                ComputerDeviceBus.DeviceResult result = bus.execute(DeviceCommand.QUARRY, size);
                switch (result.status()) {
                    case WAIT -> {
                        return false;
                    }
                    case FAULT -> {
                        latch(result.fault());
                        return false;
                    }
                    case COMPLETE -> {
                        lastResult = result.value();
                        output.appendLine(result.value() == 0 ? "Quarry stopped" : "Quarry finished");
                    }
                }
            }
            default -> throw new IllegalStateException("Unhandled shell command " + command.name());
        }
        return true;
    }

    private boolean requireFormatted() {
        if (!disk.formatted()) {
            commandError("disk not formatted");
            return false;
        }
        return true;
    }

    private void result(boolean success, String error) {
        lastResult = success ? 1 : 0;
        if (!success) {
            commandError(error);
        }
    }

    private void commandError(String message) {
        lastResult = 0;
        output.appendLine("Error: " + message);
    }

    private void latch(VmFault newFault) {
        fault = newFault;
        running = false;
    }

    @Override
    public boolean running() {
        return running;
    }

    @Override
    public int programCounter() {
        return programCounter;
    }

    @Override
    public int lastResult() {
        return lastResult;
    }

    @Override
    public VmFault fault() {
        return fault;
    }

    @Override
    public String output() {
        return output.value();
    }

    @Override
    public CompoundTag saveState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(PC_TAG, programCounter);
        tag.putBoolean(RUNNING_TAG, running);
        tag.putString(FAULT_TAG, fault.name());
        tag.putInt(LAST_RESULT_TAG, lastResult);
        tag.putString(OUTPUT_TAG, output.value());
        tag.putString(CURRENT_DIRECTORY_TAG, currentDirectory);
        tag.put(DISK_TAG, disk.save());
        return tag;
    }

    @Override
    public boolean restoreState(CompoundTag tag) {
        if (!tag.contains(PC_TAG, Tag.TAG_INT)
                || !tag.contains(RUNNING_TAG, Tag.TAG_BYTE)
                || !tag.contains(FAULT_TAG, Tag.TAG_STRING)
                || !tag.contains(LAST_RESULT_TAG, Tag.TAG_INT)
                || !tag.contains(OUTPUT_TAG, Tag.TAG_STRING)
                || !tag.contains(CURRENT_DIRECTORY_TAG, Tag.TAG_STRING)
                || !tag.contains(DISK_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        int restoredPc = tag.getInt(PC_TAG);
        VmFault restoredFault = VmFault.fromPersistentName(tag.getString(FAULT_TAG));
        VirtualDisk restoredDisk = new VirtualDisk();
        String restoredDirectory = tag.getString(CURRENT_DIRECTORY_TAG);
        if (restoredPc < 0 || restoredPc > commands.size()
                || restoredFault == VmFault.INVALID_SNAPSHOT
                || !restoredDisk.restore(tag.getCompound(DISK_TAG))
                || restoredDisk.changeDirectory("/", restoredDirectory).isEmpty()) {
            return false;
        }
        programCounter = restoredPc;
        running = tag.getBoolean(RUNNING_TAG);
        fault = restoredFault;
        lastResult = tag.getInt(LAST_RESULT_TAG);
        output.restore(tag.getString(OUTPUT_TAG));
        currentDirectory = restoredDirectory;
        disk = restoredDisk;
        return (!running || programCounter < commands.size()) && (!running || fault == VmFault.NONE);
    }

    VirtualDisk diskCopy() {
        return disk.copy();
    }

    void replaceDisk(VirtualDisk replacement) {
        disk = replacement.copy();
        currentDirectory = "/";
    }

    private static int parseQuarrySize(String value) {
        try {
            int size = Integer.parseInt(value);
            if (size < 1 || size > 16) {
                throw invalid("quarry size must be between 1 and 16");
            }
            return size;
        } catch (NumberFormatException exception) {
            throw invalid("invalid quarry size '" + value + "'");
        }
    }

    private static List<Command> parse(String source) {
        List<Command> result = new ArrayList<>();
        for (String rawLine : source.split("\\R", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.length() > MAX_COMMAND_LENGTH) {
                throw invalid("command exceeds " + MAX_COMMAND_LENGTH + " characters");
            }
            List<String> tokens = tokenize(line);
            String name = tokens.get(0).toLowerCase(Locale.ROOT);
            List<String> arguments = List.copyOf(tokens.subList(1, tokens.size()));
            validate(name, arguments);
            result.add(new Command(name, arguments));
            if (result.size() > MAX_COMMANDS) {
                throw invalid("program exceeds " + MAX_COMMANDS + " commands");
            }
        }
        return List.copyOf(result);
    }

    private static void validate(String name, List<String> arguments) {
        int minimum;
        int maximum;
        switch (name) {
            case "help", "format", "free", "fs", "update_disk" -> {
                minimum = 0;
                maximum = 0;
            }
            case "ls" -> {
                minimum = 0;
                maximum = 1;
            }
            case "cd", "mkdir", "rm", "cat", "touch", "quarry", "label" -> {
                minimum = 1;
                maximum = 1;
            }
            case "write" -> {
                minimum = 2;
                maximum = Integer.MAX_VALUE;
            }
            case "pastebin", "update" -> throw invalid("outbound network command '" + name + "' is excluded");
            default -> throw invalid("unknown command '" + name + "'");
        }
        if (arguments.size() < minimum || arguments.size() > maximum) {
            throw invalid("incorrect argument count for '" + name + "'");
        }
        if (name.equals("quarry")) {
            parseQuarrySize(arguments.get(0));
        }
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '\\' && index + 1 < line.length()) {
                current.append(line.charAt(++index));
            } else if (Character.isWhitespace(character) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw invalid("unclosed quote");
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        if (tokens.isEmpty()) {
            throw invalid("empty command");
        }
        return tokens;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid Shell source: " + message);
    }

    private record Command(String name, List<String> arguments) {
    }
}
