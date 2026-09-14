package committee.nova.mods.magneticraft.content.computer.vm;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Loader-independent integer VM with fixed storage and a hard per-tick budget.
 */
public final class BoundedComputerVm {
    public static final int MAX_PROGRAM_LENGTH = 256;
    public static final int MAX_INSTRUCTIONS_PER_TICK = 64;
    public static final int REGISTER_COUNT = 8;
    public static final int RAM_SIZE = 64;

    private List<ComputerInstruction> program = List.of();
    private final int[] registers = new int[REGISTER_COUNT];
    private final int[] ram = new int[RAM_SIZE];
    private int programCounter;
    private boolean running;
    private VmFault fault = VmFault.NONE;
    private int lastResult;

    public void replaceProgram(List<ComputerInstruction> replacement) {
        program = checkedProgram(replacement);
        Arrays.fill(registers, 0);
        Arrays.fill(ram, 0);
        programCounter = 0;
        running = !program.isEmpty();
        fault = VmFault.NONE;
        lastResult = 0;
    }

    public void reset() {
        Arrays.fill(registers, 0);
        Arrays.fill(ram, 0);
        programCounter = 0;
        running = !program.isEmpty();
        fault = VmFault.NONE;
        lastResult = 0;
    }

    /**
     * Restores an already validated save snapshot. Invalid snapshots enter a stable safe fault.
     */
    public boolean restore(
            List<ComputerInstruction> restoredProgram,
            int restoredProgramCounter,
            int[] restoredRegisters,
            int[] restoredRam,
            boolean restoredRunning,
            VmFault restoredFault,
            int restoredLastResult
    ) {
        List<ComputerInstruction> checked;
        try {
            checked = checkedProgram(restoredProgram);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            enterInvalidSnapshot();
            return false;
        }
        if (restoredRegisters == null
                || restoredRegisters.length != REGISTER_COUNT
                || restoredRam == null
                || restoredRam.length != RAM_SIZE
                || restoredFault == null
                || restoredProgramCounter < 0
                || restoredProgramCounter > checked.size()
                || (restoredRunning && (restoredFault != VmFault.NONE || restoredProgramCounter >= checked.size()))) {
            enterInvalidSnapshot();
            return false;
        }

        program = checked;
        System.arraycopy(restoredRegisters, 0, registers, 0, REGISTER_COUNT);
        System.arraycopy(restoredRam, 0, ram, 0, RAM_SIZE);
        programCounter = restoredProgramCounter;
        fault = restoredFault;
        running = restoredRunning && restoredFault == VmFault.NONE;
        lastResult = restoredLastResult;
        return true;
    }

    public int executeTick(ComputerDevice device) {
        return executeTick(device, MAX_INSTRUCTIONS_PER_TICK);
    }

    public int executeTick(ComputerDevice device, int requestedBudget) {
        Objects.requireNonNull(device, "device");
        int budget = Math.min(Math.max(requestedBudget, 0), MAX_INSTRUCTIONS_PER_TICK);
        int executed = 0;
        while (running && fault == VmFault.NONE && executed < budget) {
            if (programCounter < 0 || programCounter >= program.size()) {
                latchFault(VmFault.INVALID_PROGRAM_COUNTER);
                break;
            }
            ComputerInstruction instruction = program.get(programCounter);
            executed++;
            if (executeInstruction(instruction, device)) {
                break;
            }
            if (programCounter == program.size()) {
                running = false;
            }
        }
        return executed;
    }

    private boolean executeInstruction(ComputerInstruction instruction, ComputerDevice device) {
        int a = instruction.operandA();
        int b = instruction.operandB();
        switch (instruction.opcode()) {
            case NOP -> programCounter++;
            case HALT -> {
                programCounter++;
                running = false;
                return true;
            }
            case SET -> {
                if (!validRegister(a)) {
                    return true;
                }
                registers[a] = b;
                programCounter++;
            }
            case ADD -> {
                if (!validRegisterPair(a, b)) {
                    return true;
                }
                registers[a] += registers[b];
                programCounter++;
            }
            case SUBTRACT -> {
                if (!validRegisterPair(a, b)) {
                    return true;
                }
                registers[a] -= registers[b];
                programCounter++;
            }
            case MULTIPLY -> {
                if (!validRegisterPair(a, b)) {
                    return true;
                }
                registers[a] *= registers[b];
                programCounter++;
            }
            case DIVIDE -> {
                if (!validRegisterPair(a, b)) {
                    return true;
                }
                if (registers[b] == 0) {
                    latchFault(VmFault.DIVISION_BY_ZERO);
                    return true;
                }
                registers[a] /= registers[b];
                programCounter++;
            }
            case MODULO -> {
                if (!validRegisterPair(a, b)) {
                    return true;
                }
                if (registers[b] == 0) {
                    latchFault(VmFault.DIVISION_BY_ZERO);
                    return true;
                }
                registers[a] %= registers[b];
                programCounter++;
            }
            case LOAD -> {
                if (!validRegister(a) || !validMemoryAddress(b)) {
                    return true;
                }
                registers[a] = ram[b];
                programCounter++;
            }
            case STORE -> {
                if (!validMemoryAddress(a) || !validRegister(b)) {
                    return true;
                }
                ram[a] = registers[b];
                programCounter++;
            }
            case JUMP -> {
                if (!validJumpTarget(a)) {
                    return true;
                }
                programCounter = a;
            }
            case JUMP_IF_ZERO -> {
                if (!validRegister(a)) {
                    return true;
                }
                if (registers[a] == 0) {
                    if (!validJumpTarget(b)) {
                        return true;
                    }
                    programCounter = b;
                } else {
                    programCounter++;
                }
            }
            case MOVE, MINE, SET_REDSTONE -> {
                if (!validRegister(b)) {
                    return true;
                }
                ComputerDevice.DeviceResult result = device.execute(instruction.opcode(), a);
                if (result.status() == ComputerDevice.Status.FAULT) {
                    latchFault(result.fault());
                    return true;
                }
                if (result.status() == ComputerDevice.Status.WAIT) {
                    return true;
                }
                registers[b] = result.value();
                lastResult = result.value();
                programCounter++;
                return result.yieldAfter();
            }
        }
        return false;
    }

    private boolean validRegisterPair(int first, int second) {
        return validRegister(first) && validRegister(second);
    }

    private boolean validRegister(int index) {
        if (index >= 0 && index < registers.length) {
            return true;
        }
        latchFault(VmFault.INVALID_REGISTER);
        return false;
    }

    private boolean validMemoryAddress(int address) {
        if (address >= 0 && address < ram.length) {
            return true;
        }
        latchFault(VmFault.INVALID_MEMORY_ADDRESS);
        return false;
    }

    private boolean validJumpTarget(int target) {
        if (target >= 0 && target < program.size()) {
            return true;
        }
        latchFault(VmFault.INVALID_PROGRAM_COUNTER);
        return false;
    }

    private void enterInvalidSnapshot() {
        program = List.of();
        Arrays.fill(registers, 0);
        Arrays.fill(ram, 0);
        programCounter = 0;
        running = false;
        fault = VmFault.INVALID_SNAPSHOT;
        lastResult = 0;
    }

    private void latchFault(VmFault newFault) {
        if (fault == VmFault.NONE) {
            fault = Objects.requireNonNull(newFault, "newFault");
        }
        running = false;
    }

    public static List<ComputerInstruction> validatedProgramCopy(List<ComputerInstruction> candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.size() > MAX_PROGRAM_LENGTH) {
            throw new IllegalArgumentException("Program exceeds " + MAX_PROGRAM_LENGTH + " instructions");
        }
        for (ComputerInstruction instruction : candidate) {
            Objects.requireNonNull(instruction, "instruction");
            validateInstruction(instruction, candidate.size());
        }
        return List.copyOf(candidate);
    }

    private static List<ComputerInstruction> checkedProgram(List<ComputerInstruction> candidate) {
        return validatedProgramCopy(candidate);
    }

    private static void validateInstruction(ComputerInstruction instruction, int programSize) {
        int a = instruction.operandA();
        int b = instruction.operandB();
        boolean valid = switch (instruction.opcode()) {
            case NOP, HALT -> true;
            case SET -> isRegisterIndex(a);
            case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO -> isRegisterIndex(a) && isRegisterIndex(b);
            case LOAD -> isRegisterIndex(a) && isMemoryAddress(b);
            case STORE -> isMemoryAddress(a) && isRegisterIndex(b);
            case JUMP -> isProgramTarget(a, programSize);
            case JUMP_IF_ZERO -> isRegisterIndex(a) && isProgramTarget(b, programSize);
            case MOVE, MINE -> isRelativeDirection(a) && isRegisterIndex(b);
            case SET_REDSTONE -> a >= 0 && a <= 15 && isRegisterIndex(b);
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid operands for " + instruction.opcode());
        }
    }

    private static boolean isRegisterIndex(int index) {
        return index >= 0 && index < REGISTER_COUNT;
    }

    private static boolean isMemoryAddress(int address) {
        return address >= 0 && address < RAM_SIZE;
    }

    private static boolean isProgramTarget(int target, int programSize) {
        return target >= 0 && target < programSize;
    }

    private static boolean isRelativeDirection(int direction) {
        return direction >= 0 && direction <= 5;
    }

    public List<ComputerInstruction> program() {
        return program;
    }

    public int programCounter() {
        return programCounter;
    }

    public boolean running() {
        return running;
    }

    public VmFault fault() {
        return fault;
    }

    public int lastResult() {
        return lastResult;
    }

    public int register(int index) {
        if (index < 0 || index >= registers.length) {
            throw new IndexOutOfBoundsException(index);
        }
        return registers[index];
    }

    public int memory(int address) {
        if (address < 0 || address >= ram.length) {
            throw new IndexOutOfBoundsException(address);
        }
        return ram[address];
    }

    public int[] copyRegisters() {
        return registers.clone();
    }

    public int[] copyRam() {
        return ram.clone();
    }
}
