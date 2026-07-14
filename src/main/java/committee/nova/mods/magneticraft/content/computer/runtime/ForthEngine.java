package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus.DeviceCommand;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Small FORTH 1.1-compatible core with bounded stacks, memory and code. */
final class ForthEngine implements ScriptEngine {
    static final int MAX_CODE_SIZE = 1_024;
    static final int DATA_STACK_SIZE = 64;
    static final int RETURN_STACK_SIZE = 32;
    static final int MEMORY_SIZE = 64;

    private static final String PC_TAG = "program_counter";
    private static final String DATA_STACK_TAG = "data_stack";
    private static final String RETURN_STACK_TAG = "return_stack";
    private static final String MEMORY_TAG = "memory";
    private static final String RUNNING_TAG = "running";
    private static final String FAULT_TAG = "fault";
    private static final String LAST_RESULT_TAG = "last_result";
    private static final String EXECUTED_TAG = "executed_instructions";
    private static final String OUTPUT_TAG = "output";
    private static final String WORDS = "+ - * / mod dup drop swap over @ ! . emit words free times ticks "
            + "mine front forward back left right up down scan redstone inventory energy quarry";

    private final List<Instruction> code;
    private final int[] dataStack = new int[DATA_STACK_SIZE];
    private final int[] returnStack = new int[RETURN_STACK_SIZE];
    private final int[] memory = new int[MEMORY_SIZE];
    private final ScriptOutput output = new ScriptOutput();
    private int dataSize;
    private int returnSize;
    private int programCounter;
    private boolean running = true;
    private VmFault fault = VmFault.NONE;
    private int lastResult;
    private long executedInstructions;

    ForthEngine(String source) {
        code = compile(source);
    }

    @Override
    public int executeTick(ComputerDeviceBus bus, int instructionBudget) {
        int budget = Math.max(0, Math.min(ScriptRuntime.MAX_INSTRUCTIONS_PER_TICK, instructionBudget));
        int executed = 0;
        while (running && fault == VmFault.NONE && executed < budget) {
            if (programCounter < 0 || programCounter >= code.size()) {
                latch(VmFault.INVALID_PROGRAM_COUNTER);
                break;
            }
            Instruction instruction = code.get(programCounter);
            executed++;
            executedInstructions++;
            boolean yield = execute(instruction, bus);
            if (yield) {
                break;
            }
        }
        return executed;
    }

    private boolean execute(Instruction instruction, ComputerDeviceBus bus) {
        switch (instruction.op()) {
            case PUSH -> {
                push(instruction.operand());
                programCounter++;
            }
            case ADD -> binary((left, right) -> left + right);
            case SUBTRACT -> binary((left, right) -> left - right);
            case MULTIPLY -> binary((left, right) -> left * right);
            case DIVIDE -> divide(false);
            case MODULO -> divide(true);
            case DUPLICATE -> {
                if (requireData(1)) {
                    push(dataStack[dataSize - 1]);
                    programCounter++;
                }
            }
            case DROP -> {
                if (requireData(1)) {
                    dataSize--;
                    programCounter++;
                }
            }
            case SWAP -> {
                if (requireData(2)) {
                    int value = dataStack[dataSize - 1];
                    dataStack[dataSize - 1] = dataStack[dataSize - 2];
                    dataStack[dataSize - 2] = value;
                    programCounter++;
                }
            }
            case OVER -> {
                if (requireData(2)) {
                    push(dataStack[dataSize - 2]);
                    programCounter++;
                }
            }
            case FETCH -> {
                if (requireData(1)) {
                    int address = pop();
                    if (validAddress(address)) {
                        push(memory[address]);
                        programCounter++;
                    }
                }
            }
            case STORE -> {
                if (requireData(2)) {
                    int address = pop();
                    int value = pop();
                    if (validAddress(address)) {
                        memory[address] = value;
                        programCounter++;
                    }
                }
            }
            case OUTPUT_NUMBER -> {
                if (requireData(1)) {
                    output.append(Integer.toString(pop()));
                    output.append(" ");
                    programCounter++;
                }
            }
            case EMIT -> {
                if (requireData(1)) {
                    output.append(Character.toString((char) (pop() & 0xFF)));
                    programCounter++;
                }
            }
            case WORDS -> {
                output.appendLine(WORDS);
                programCounter++;
            }
            case FREE -> {
                push(MEMORY_SIZE - usedMemoryCells());
                programCounter++;
            }
            case TICKS -> {
                push((int) Math.min(Integer.MAX_VALUE, executedInstructions));
                programCounter++;
            }
            case JUMP -> programCounter = instruction.operand();
            case JUMP_IF_ZERO -> {
                if (requireData(1)) {
                    programCounter = pop() == 0 ? instruction.operand() : programCounter + 1;
                }
            }
            case CALL -> {
                if (returnSize >= RETURN_STACK_SIZE) {
                    latch(VmFault.RETURN_STACK_OVERFLOW);
                } else {
                    returnStack[returnSize++] = programCounter + 1;
                    programCounter = instruction.operand();
                }
            }
            case RETURN -> {
                if (returnSize == 0) {
                    running = false;
                } else {
                    programCounter = returnStack[--returnSize];
                }
            }
            case DEVICE -> {
                return executeDevice(instruction, bus);
            }
            case HALT -> running = false;
        }
        return fault != VmFault.NONE || !running;
    }

    private boolean executeDevice(Instruction instruction, ComputerDeviceBus bus) {
        DeviceCommand command = DeviceCommand.values()[instruction.operand()];
        boolean hasArgument = switch (command) {
            case SET_REDSTONE, MOVE, MINE, SCAN, QUARRY -> true;
            default -> false;
        };
        if (hasArgument && !requireData(1)) {
            return true;
        }
        int argument = hasArgument ? dataStack[dataSize - 1] : 0;
        ComputerDeviceBus.DeviceResult result = bus.execute(command, argument);
        switch (result.status()) {
            case WAIT -> {
                return true;
            }
            case FAULT -> {
                latch(result.fault());
                return true;
            }
            case COMPLETE -> {
                if (hasArgument) {
                    dataSize--;
                }
                push(result.value());
                lastResult = result.value();
                programCounter++;
                return true;
            }
        }
        throw new IllegalStateException("Unhandled device status " + result.status());
    }

    private void binary(IntOperation operation) {
        if (!requireData(2)) {
            return;
        }
        int right = pop();
        int left = pop();
        push(operation.apply(left, right));
        programCounter++;
    }

    private void divide(boolean modulo) {
        if (!requireData(2)) {
            return;
        }
        int right = dataStack[dataSize - 1];
        if (right == 0) {
            latch(VmFault.DIVISION_BY_ZERO);
            return;
        }
        right = pop();
        int left = pop();
        push(modulo ? left % right : left / right);
        programCounter++;
    }

    private int usedMemoryCells() {
        int used = 0;
        for (int value : memory) {
            if (value != 0) {
                used++;
            }
        }
        return used;
    }

    private boolean validAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            latch(VmFault.INVALID_MEMORY_ADDRESS);
            return false;
        }
        return true;
    }

    private boolean requireData(int count) {
        if (dataSize < count) {
            latch(VmFault.DATA_STACK_UNDERFLOW);
            return false;
        }
        return true;
    }

    private void push(int value) {
        if (dataSize >= DATA_STACK_SIZE) {
            latch(VmFault.DATA_STACK_OVERFLOW);
            return;
        }
        dataStack[dataSize++] = value;
    }

    private int pop() {
        return dataStack[--dataSize];
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
        tag.putIntArray(DATA_STACK_TAG, Arrays.copyOf(dataStack, dataSize));
        tag.putIntArray(RETURN_STACK_TAG, Arrays.copyOf(returnStack, returnSize));
        tag.putIntArray(MEMORY_TAG, memory.clone());
        tag.putBoolean(RUNNING_TAG, running);
        tag.putString(FAULT_TAG, fault.name());
        tag.putInt(LAST_RESULT_TAG, lastResult);
        tag.putLong(EXECUTED_TAG, executedInstructions);
        tag.putString(OUTPUT_TAG, output.value());
        return tag;
    }

    @Override
    public boolean restoreState(CompoundTag tag) {
        if (!tag.contains(PC_TAG, Tag.TAG_INT)
                || !tag.contains(DATA_STACK_TAG, Tag.TAG_INT_ARRAY)
                || !tag.contains(RETURN_STACK_TAG, Tag.TAG_INT_ARRAY)
                || !tag.contains(MEMORY_TAG, Tag.TAG_INT_ARRAY)
                || !tag.contains(RUNNING_TAG, Tag.TAG_BYTE)
                || !tag.contains(FAULT_TAG, Tag.TAG_STRING)
                || !tag.contains(LAST_RESULT_TAG, Tag.TAG_INT)
                || !tag.contains(EXECUTED_TAG, Tag.TAG_LONG)
                || !tag.contains(OUTPUT_TAG, Tag.TAG_STRING)) {
            return false;
        }
        int restoredPc = tag.getInt(PC_TAG);
        int[] restoredData = tag.getIntArray(DATA_STACK_TAG);
        int[] restoredReturn = tag.getIntArray(RETURN_STACK_TAG);
        int[] restoredMemory = tag.getIntArray(MEMORY_TAG);
        VmFault restoredFault = VmFault.fromPersistentName(tag.getString(FAULT_TAG));
        if (restoredPc < 0 || restoredPc >= code.size()
                || restoredData.length > DATA_STACK_SIZE
                || restoredReturn.length > RETURN_STACK_SIZE
                || restoredMemory.length != MEMORY_SIZE
                || restoredFault == VmFault.INVALID_SNAPSHOT) {
            return false;
        }
        programCounter = restoredPc;
        dataSize = restoredData.length;
        returnSize = restoredReturn.length;
        System.arraycopy(restoredData, 0, dataStack, 0, restoredData.length);
        System.arraycopy(restoredReturn, 0, returnStack, 0, restoredReturn.length);
        System.arraycopy(restoredMemory, 0, memory, 0, memory.length);
        running = tag.getBoolean(RUNNING_TAG);
        fault = restoredFault;
        lastResult = tag.getInt(LAST_RESULT_TAG);
        executedInstructions = Math.max(0L, tag.getLong(EXECUTED_TAG));
        output.restore(tag.getString(OUTPUT_TAG));
        return !running || fault == VmFault.NONE;
    }

    int stackSize() {
        return dataSize;
    }

    int peek() {
        if (dataSize == 0) {
            throw new IllegalStateException("Empty FORTH data stack");
        }
        return dataStack[dataSize - 1];
    }

    int memory(int address) {
        return memory[address];
    }

    private static List<Instruction> compile(String source) {
        List<String> tokens = tokenize(source);
        Map<String, List<String>> definitions = new LinkedHashMap<>();
        List<String> main = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index++) {
            String token = normalized(tokens.get(index));
            if (!token.equals(":")) {
                main.add(token);
                continue;
            }
            if (++index >= tokens.size()) {
                throw invalid("Missing word name after ':'");
            }
            String name = normalized(tokens.get(index));
            if (name.isBlank() || isBuiltin(name) || definitions.containsKey(name)) {
                throw invalid("Invalid or duplicate word '" + name + "'");
            }
            List<String> body = new ArrayList<>();
            while (++index < tokens.size() && !tokens.get(index).equals(";")) {
                body.add(normalized(tokens.get(index)));
            }
            if (index >= tokens.size()) {
                throw invalid("Word '" + name + "' is missing ';'");
            }
            definitions.put(name, List.copyOf(body));
        }

        List<Instruction> code = new ArrayList<>();
        compileSequence(main, definitions, code, false);
        code.add(Instruction.simple(Op.HALT));
        Map<String, Integer> addresses = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> definition : definitions.entrySet()) {
            addresses.put(definition.getKey(), code.size());
            compileSequence(definition.getValue(), definitions, code, true);
            code.add(Instruction.simple(Op.RETURN));
        }
        for (int index = 0; index < code.size(); index++) {
            Instruction instruction = code.get(index);
            if (instruction.op() == Op.CALL && instruction.text() != null) {
                Integer address = addresses.get(instruction.text());
                if (address == null) {
                    throw invalid("Unknown word '" + instruction.text() + "'");
                }
                code.set(index, new Instruction(Op.CALL, address, null));
            }
        }
        if (code.size() > MAX_CODE_SIZE) {
            throw invalid("Compiled program exceeds " + MAX_CODE_SIZE + " instructions");
        }
        return List.copyOf(code);
    }

    private static void compileSequence(
            List<String> tokens,
            Map<String, List<String>> definitions,
            List<Instruction> code,
            boolean definition
    ) {
        Deque<Control> controls = new ArrayDeque<>();
        for (String token : tokens) {
            Integer number = parseNumber(token);
            if (number != null) {
                code.add(Instruction.withOperand(Op.PUSH, number));
                continue;
            }
            switch (token) {
                case "+" -> code.add(Instruction.simple(Op.ADD));
                case "-" -> code.add(Instruction.simple(Op.SUBTRACT));
                case "*" -> code.add(Instruction.simple(Op.MULTIPLY));
                case "/" -> code.add(Instruction.simple(Op.DIVIDE));
                case "mod" -> code.add(Instruction.simple(Op.MODULO));
                case "dup" -> code.add(Instruction.simple(Op.DUPLICATE));
                case "drop" -> code.add(Instruction.simple(Op.DROP));
                case "swap" -> code.add(Instruction.simple(Op.SWAP));
                case "over" -> code.add(Instruction.simple(Op.OVER));
                case "@" -> code.add(Instruction.simple(Op.FETCH));
                case "!" -> code.add(Instruction.simple(Op.STORE));
                case ".", "print" -> code.add(Instruction.simple(Op.OUTPUT_NUMBER));
                case "emit" -> code.add(Instruction.simple(Op.EMIT));
                case "words" -> code.add(Instruction.simple(Op.WORDS));
                case "free" -> code.add(Instruction.simple(Op.FREE));
                case "times", "ticks" -> code.add(Instruction.simple(Op.TICKS));
                case "front", "forward" -> code.add(device(DeviceCommand.MOVE_FRONT));
                case "back" -> code.add(device(DeviceCommand.MOVE_BACK));
                case "left" -> code.add(device(DeviceCommand.ROTATE_LEFT));
                case "right" -> code.add(device(DeviceCommand.ROTATE_RIGHT));
                case "up" -> code.add(device(DeviceCommand.ROTATE_UP));
                case "down" -> code.add(device(DeviceCommand.ROTATE_DOWN));
                case "mine" -> code.add(device(DeviceCommand.MINE_FRONT));
                case "scan" -> code.add(device(DeviceCommand.SCAN_FRONT));
                case "redstone" -> code.add(device(DeviceCommand.SET_REDSTONE));
                case "inventory" -> code.add(device(DeviceCommand.INVENTORY_COUNT));
                case "energy" -> code.add(device(DeviceCommand.ENERGY_STORED));
                case "quarry" -> code.add(device(DeviceCommand.QUARRY));
                case "if" -> {
                    controls.push(new Control(ControlKind.IF, code.size()));
                    code.add(Instruction.withOperand(Op.JUMP_IF_ZERO, -1));
                }
                case "else" -> {
                    Control start = popControl(controls, ControlKind.IF, "else without if");
                    patch(code, start.index(), code.size() + 1);
                    controls.push(new Control(ControlKind.ELSE, code.size()));
                    code.add(Instruction.withOperand(Op.JUMP, -1));
                }
                case "then" -> {
                    if (controls.isEmpty()
                            || controls.peek().kind() != ControlKind.IF && controls.peek().kind() != ControlKind.ELSE) {
                        throw invalid("then without if");
                    }
                    patch(code, controls.pop().index(), code.size());
                }
                case "begin" -> controls.push(new Control(ControlKind.BEGIN, code.size()));
                case "until" -> {
                    Control begin = popControl(controls, ControlKind.BEGIN, "until without begin");
                    code.add(Instruction.withOperand(Op.JUMP_IF_ZERO, begin.index()));
                }
                case "again" -> {
                    Control begin = popControl(controls, ControlKind.BEGIN, "again without begin");
                    code.add(Instruction.withOperand(Op.JUMP, begin.index()));
                }
                case "exit" -> code.add(Instruction.simple(definition ? Op.RETURN : Op.HALT));
                default -> {
                    if (!definitions.containsKey(token)) {
                        throw invalid("Unknown word '" + token + "'");
                    }
                    code.add(new Instruction(Op.CALL, -1, token));
                }
            }
            if (code.size() > MAX_CODE_SIZE) {
                throw invalid("Compiled program exceeds " + MAX_CODE_SIZE + " instructions");
            }
        }
        if (!controls.isEmpty()) {
            throw invalid("Unclosed control word " + controls.peek().kind().name().toLowerCase(Locale.ROOT));
        }
    }

    private static Control popControl(Deque<Control> controls, ControlKind expected, String message) {
        if (controls.isEmpty() || controls.peek().kind() != expected) {
            throw invalid(message);
        }
        return controls.pop();
    }

    private static void patch(List<Instruction> code, int index, int target) {
        Instruction old = code.get(index);
        code.set(index, new Instruction(old.op(), target, old.text()));
    }

    private static Instruction device(DeviceCommand command) {
        return Instruction.withOperand(Op.DEVICE, command.ordinal());
    }

    private static boolean isBuiltin(String word) {
        return Arrays.asList(WORDS.split(" ")).contains(word)
                || List.of(":", ";", "if", "else", "then", "begin", "until", "again", "exit").contains(word);
    }

    private static List<String> tokenize(String source) {
        String withoutParenthesizedComments = source.replaceAll("(?s)\\([^)]*\\)", " ");
        StringBuilder cleaned = new StringBuilder();
        for (String line : withoutParenthesizedComments.split("\\R", -1)) {
            int comment = line.indexOf('\\');
            cleaned.append(comment < 0 ? line : line.substring(0, comment)).append(' ');
        }
        String trimmed = cleaned.toString().trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        List<String> result = Arrays.stream(trimmed.split("\\s+"))
                .map(ForthEngine::normalized)
                .toList();
        if (result.size() > MAX_CODE_SIZE * 2) {
            throw invalid("Source contains too many tokens");
        }
        return result;
    }

    private static String normalized(String token) {
        return token.toLowerCase(Locale.ROOT);
    }

    private static Integer parseNumber(String token) {
        try {
            return token.startsWith("0x") || token.startsWith("-0x")
                    ? Integer.decode(token)
                    : Integer.valueOf(token);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid FORTH source: " + message);
    }

    private enum Op {
        PUSH,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO,
        DUPLICATE,
        DROP,
        SWAP,
        OVER,
        FETCH,
        STORE,
        OUTPUT_NUMBER,
        EMIT,
        WORDS,
        FREE,
        TICKS,
        JUMP,
        JUMP_IF_ZERO,
        CALL,
        RETURN,
        DEVICE,
        HALT
    }

    private enum ControlKind {
        IF,
        ELSE,
        BEGIN
    }

    private record Control(ControlKind kind, int index) {
    }

    private record Instruction(Op op, int operand, String text) {
        private static Instruction simple(Op op) {
            return new Instruction(op, 0, null);
        }

        private static Instruction withOperand(Op op, int operand) {
            return new Instruction(op, operand, null);
        }
    }

    @FunctionalInterface
    private interface IntOperation {
        int apply(int left, int right);
    }
}
