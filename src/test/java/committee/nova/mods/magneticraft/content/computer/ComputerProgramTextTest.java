package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComputerProgramTextTest {
    @Test
    void parsesCaseInsensitiveProgramAndSkipsBlankEditorRows() {
        assertEquals(
                List.of(
                        new ComputerInstruction(ComputerOpcode.SET, 0, 7),
                        new ComputerInstruction(ComputerOpcode.HALT, 0, 0)
                ),
                ComputerProgramText.parseLines(List.of(" SET 0 7 ", "", "halt"))
        );
    }

    @Test
    void formattingUsesOnlyDeclaredOperands() {
        assertEquals("nop", ComputerProgramText.format(new ComputerInstruction(ComputerOpcode.NOP, 99, 99)));
        assertEquals("jump 2", ComputerProgramText.format(new ComputerInstruction(ComputerOpcode.JUMP, 2, 99)));
        assertEquals("set 1 -5", ComputerProgramText.format(new ComputerInstruction(ComputerOpcode.SET, 1, -5)));
    }

    @Test
    void rejectsUnknownOpcodesArityAndInvalidVmOperands() {
        assertThrows(IllegalArgumentException.class, () -> ComputerProgramText.parseLines(List.of("explode")));
        assertThrows(IllegalArgumentException.class, () -> ComputerProgramText.parseLines(List.of("set 0")));
        assertThrows(IllegalArgumentException.class, () -> ComputerProgramText.parseLines(List.of("set 8 1")));
    }
}
