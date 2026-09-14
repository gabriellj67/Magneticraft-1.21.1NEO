package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Small text boundary for the programmable-machine editor.
 */
public final class ComputerProgramText {
    private ComputerProgramText() {
    }

    public static List<ComputerInstruction> parseLines(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("Program lines must not be null");
        }
        List<ComputerInstruction> result = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            int sourceLine = lineIndex;
            String raw = lines.get(lineIndex);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] tokens = raw.trim().split("\\s+");
            ComputerOpcode opcode = Arrays.stream(ComputerOpcode.values())
                    .filter(candidate -> candidate.serializedName().equals(tokens[0].toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .orElseThrow(() -> invalidLine(sourceLine, "unknown opcode " + tokens[0]));
            if (tokens.length != opcode.operandCount() + 1) {
                throw invalidLine(lineIndex, "expected " + opcode.operandCount() + " operands");
            }
            int operandA = opcode.operandCount() > 0 ? parseOperand(tokens[1], lineIndex) : 0;
            int operandB = opcode.operandCount() > 1 ? parseOperand(tokens[2], lineIndex) : 0;
            result.add(new ComputerInstruction(opcode, operandA, operandB));
        }
        try {
            return BoundedComputerVm.validatedProgramCopy(result);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid program: " + exception.getMessage(), exception);
        }
    }

    public static String format(ComputerInstruction instruction) {
        return switch (instruction.opcode().operandCount()) {
            case 0 -> instruction.opcode().serializedName();
            case 1 -> instruction.opcode().serializedName() + " " + instruction.operandA();
            default -> instruction.opcode().serializedName()
                    + " " + instruction.operandA()
                    + " " + instruction.operandB();
        };
    }

    private static int parseOperand(String token, int lineIndex) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException exception) {
            throw invalidLine(lineIndex, "invalid integer " + token);
        }
    }

    private static IllegalArgumentException invalidLine(int lineIndex, String detail) {
        return new IllegalArgumentException("Line " + (lineIndex + 1) + ": " + detail);
    }
}
