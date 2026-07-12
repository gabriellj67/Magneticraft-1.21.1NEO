package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramNbtTest {
    @Test
    void programRoundTripPreservesStableOpcodeAndOperands() {
        List<ComputerInstruction> program = List.of(
                new ComputerInstruction(ComputerOpcode.SET, 0, 42),
                new ComputerInstruction(ComputerOpcode.STORE, 7, 0),
                new ComputerInstruction(ComputerOpcode.HALT, 0, 0)
        );
        CompoundTag root = new CompoundTag();

        ProgramNbt.writeProgram(root, "program", program);

        assertEquals(program, ProgramNbt.readProgram(root, "program").orElseThrow());
    }

    @Test
    void malformedOpcodeAndElementTypeFailClosed() {
        CompoundTag unknownOpcodeRoot = new CompoundTag();
        ListTag unknownOpcodeProgram = new ListTag();
        CompoundTag instruction = new CompoundTag();
        instruction.putInt("opcode", Integer.MAX_VALUE);
        unknownOpcodeProgram.add(instruction);
        unknownOpcodeRoot.put("program", unknownOpcodeProgram);
        assertTrue(ProgramNbt.readProgram(unknownOpcodeRoot, "program").isEmpty());

        CompoundTag wrongElementRoot = new CompoundTag();
        ListTag wrongElements = new ListTag();
        wrongElements.add(IntTag.valueOf(1));
        wrongElementRoot.put("program", wrongElements);
        assertTrue(ProgramNbt.readProgram(wrongElementRoot, "program").isEmpty());
    }
}
