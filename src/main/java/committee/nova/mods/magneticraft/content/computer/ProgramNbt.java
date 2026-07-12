package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared, bounded NBT representation used by block entities and floppy disks.
 */
final class ProgramNbt {
    private static final String OPCODE_TAG = "opcode";
    private static final String OPERAND_A_TAG = "operand_a";
    private static final String OPERAND_B_TAG = "operand_b";

    private ProgramNbt() {
    }

    static void writeProgram(CompoundTag target, String key, List<ComputerInstruction> program) {
        ListTag instructions = new ListTag();
        for (ComputerInstruction instruction : BoundedComputerVm.validatedProgramCopy(program)) {
            CompoundTag encoded = new CompoundTag();
            encoded.putInt(OPCODE_TAG, instruction.opcode().networkId());
            encoded.putInt(OPERAND_A_TAG, instruction.operandA());
            encoded.putInt(OPERAND_B_TAG, instruction.operandB());
            instructions.add(encoded);
        }
        target.put(key, instructions);
    }

    static Optional<List<ComputerInstruction>> readProgram(CompoundTag source, String key) {
        if (!source.contains(key)) {
            return Optional.of(List.of());
        }
        Tag rawProgram = source.get(key);
        if (!(rawProgram instanceof ListTag encoded)) {
            return Optional.empty();
        }
        if (encoded.size() > BoundedComputerVm.MAX_PROGRAM_LENGTH) {
            return Optional.empty();
        }
        List<ComputerInstruction> instructions = new ArrayList<>(encoded.size());
        for (Tag element : encoded) {
            if (!(element instanceof CompoundTag instructionTag)) {
                return Optional.empty();
            }
            if (!instructionTag.contains(OPCODE_TAG, Tag.TAG_INT)
                    || !instructionTag.contains(OPERAND_A_TAG, Tag.TAG_INT)
                    || !instructionTag.contains(OPERAND_B_TAG, Tag.TAG_INT)) {
                return Optional.empty();
            }
            Optional<ComputerOpcode> opcode = ComputerOpcode.fromNetworkId(instructionTag.getInt(OPCODE_TAG));
            if (opcode.isEmpty()) {
                return Optional.empty();
            }
            instructions.add(new ComputerInstruction(
                    opcode.get(),
                    instructionTag.getInt(OPERAND_A_TAG),
                    instructionTag.getInt(OPERAND_B_TAG)
            ));
        }
        try {
            return Optional.of(BoundedComputerVm.validatedProgramCopy(instructions));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Optional.empty();
        }
    }
}
