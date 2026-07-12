package committee.nova.mods.magneticraft.content.computer.vm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedComputerVmTest {
    @Test
    void opcodeIdsAndGuideMetadataAreStableAndUnique() {
        HashSet<Integer> ids = new HashSet<>();
        for (ComputerOpcode opcode : ComputerOpcode.values()) {
            assertTrue(ids.add(opcode.networkId()), "Duplicate opcode id " + opcode.networkId());
            assertEquals(opcode, ComputerOpcode.fromNetworkId(opcode.networkId()).orElseThrow());
            assertTrue(opcode.operandCount() >= 0 && opcode.operandCount() <= 2);
            assertEquals(opcode.name().toLowerCase(java.util.Locale.ROOT), opcode.serializedName());
            assertTrue(opcode.descriptionTranslationKey().startsWith("guide.magneticraft.computer.opcode."));
        }
    }

    @Test
    void programAndTickLimitsAreHardBounds() {
        BoundedComputerVm vm = new BoundedComputerVm();
        List<ComputerInstruction> tooLarge = new ArrayList<>();
        for (int index = 0; index <= BoundedComputerVm.MAX_PROGRAM_LENGTH; index++) {
            tooLarge.add(instruction(ComputerOpcode.NOP, 0, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> vm.replaceProgram(tooLarge));

        List<ComputerInstruction> bounded = tooLarge.subList(0, BoundedComputerVm.MAX_PROGRAM_LENGTH);
        vm.replaceProgram(bounded);
        assertEquals(BoundedComputerVm.MAX_INSTRUCTIONS_PER_TICK, vm.executeTick(ComputerDevice.NONE, Integer.MAX_VALUE));
        assertEquals(BoundedComputerVm.MAX_INSTRUCTIONS_PER_TICK, vm.programCounter());
        assertTrue(vm.running());
    }

    @Test
    void invalidOperandsAreRejectedBeforeProgramMutation() {
        BoundedComputerVm vm = new BoundedComputerVm();
        List<ComputerInstruction> original = List.of(instruction(ComputerOpcode.HALT, 0, 0));
        vm.replaceProgram(original);

        assertThrows(
                IllegalArgumentException.class,
                () -> vm.replaceProgram(List.of(instruction(ComputerOpcode.SET, BoundedComputerVm.REGISTER_COUNT, 1)))
        );
        assertEquals(original, vm.program());
        assertEquals(0, vm.programCounter());
        assertTrue(vm.running());
    }

    @Test
    void arithmeticMemoryAndBranchingAreDeterministic() {
        BoundedComputerVm vm = new BoundedComputerVm();
        vm.replaceProgram(List.of(
                instruction(ComputerOpcode.SET, 0, 21),
                instruction(ComputerOpcode.SET, 1, 2),
                instruction(ComputerOpcode.MULTIPLY, 0, 1),
                instruction(ComputerOpcode.STORE, 5, 0),
                instruction(ComputerOpcode.LOAD, 2, 5),
                instruction(ComputerOpcode.SUBTRACT, 2, 0),
                instruction(ComputerOpcode.JUMP_IF_ZERO, 2, 8),
                instruction(ComputerOpcode.SET, 3, -1),
                instruction(ComputerOpcode.HALT, 0, 0)
        ));

        assertEquals(8, vm.executeTick(ComputerDevice.NONE));
        assertEquals(42, vm.register(0));
        assertEquals(42, vm.memory(5));
        assertEquals(0, vm.register(2));
        assertEquals(0, vm.register(3));
        assertFalse(vm.running());
        assertEquals(VmFault.NONE, vm.fault());
    }

    @Test
    void faultsLatchUntilProgramReplacement() {
        BoundedComputerVm vm = new BoundedComputerVm();
        vm.replaceProgram(List.of(
                instruction(ComputerOpcode.SET, 0, 7),
                instruction(ComputerOpcode.SET, 1, 0),
                instruction(ComputerOpcode.DIVIDE, 0, 1),
                instruction(ComputerOpcode.HALT, 0, 0)
        ));

        assertEquals(3, vm.executeTick(ComputerDevice.NONE));
        assertEquals(VmFault.DIVISION_BY_ZERO, vm.fault());
        assertEquals(7, vm.register(0));
        assertEquals(0, vm.executeTick(ComputerDevice.NONE));
        assertEquals(VmFault.DIVISION_BY_ZERO, vm.fault());

        vm.replaceProgram(List.of(instruction(ComputerOpcode.HALT, 0, 0)));
        assertEquals(VmFault.NONE, vm.fault());
        assertTrue(vm.running());
    }

    @Test
    void deviceInstructionsCanYieldWithoutEscapingTheBudget() {
        BoundedComputerVm vm = new BoundedComputerVm();
        vm.replaceProgram(List.of(
                instruction(ComputerOpcode.MINE, 0, 3),
                instruction(ComputerOpcode.SET, 0, 99),
                instruction(ComputerOpcode.HALT, 0, 0)
        ));

        int executed = vm.executeTick((opcode, operand) -> ComputerDevice.DeviceResult.completeAndYield(1));
        assertEquals(1, executed);
        assertEquals(1, vm.register(3));
        assertEquals(1, vm.programCounter());
        assertTrue(vm.running());

        assertEquals(2, vm.executeTick(ComputerDevice.NONE));
        assertEquals(99, vm.register(0));
        assertFalse(vm.running());
    }

    @Test
    void waitingDeviceInstructionDoesNotAdvanceOrFault() {
        BoundedComputerVm vm = new BoundedComputerVm();
        vm.replaceProgram(List.of(
                instruction(ComputerOpcode.MOVE, 0, 1),
                instruction(ComputerOpcode.HALT, 0, 0)
        ));

        assertEquals(1, vm.executeTick((opcode, operand) -> ComputerDevice.DeviceResult.waiting()));
        assertEquals(0, vm.programCounter());
        assertEquals(VmFault.NONE, vm.fault());
        assertTrue(vm.running());

        assertEquals(1, vm.executeTick((opcode, operand) -> ComputerDevice.DeviceResult.completeAndYield(1)));
        assertEquals(1, vm.programCounter());
        assertEquals(1, vm.register(1));
    }

    @Test
    void malformedSnapshotFailsClosed() {
        BoundedComputerVm vm = new BoundedComputerVm();
        boolean restored = vm.restore(
                List.of(instruction(ComputerOpcode.NOP, 0, 0)),
                0,
                new int[1],
                new int[BoundedComputerVm.RAM_SIZE],
                true,
                VmFault.NONE,
                0
        );

        assertFalse(restored);
        assertFalse(vm.running());
        assertEquals(VmFault.INVALID_SNAPSHOT, vm.fault());
        assertEquals(0, vm.executeTick(ComputerDevice.NONE));
    }

    private static ComputerInstruction instruction(ComputerOpcode opcode, int operandA, int operandB) {
        return new ComputerInstruction(opcode, operandA, operandB);
    }
}
