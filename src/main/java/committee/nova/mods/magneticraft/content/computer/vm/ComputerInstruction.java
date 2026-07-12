package committee.nova.mods.magneticraft.content.computer.vm;

import java.util.Objects;

/**
 * One fixed-width instruction. Operand meaning is defined by {@link ComputerOpcode}.
 */
public record ComputerInstruction(ComputerOpcode opcode, int operandA, int operandB) {
    public ComputerInstruction {
        Objects.requireNonNull(opcode, "opcode");
    }
}
