package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptRuntimeTest {
    @Test
    void forthPreservesReleasedArithmeticWordsAndDefinitions() {
        ScriptRuntime runtime = runtime(ScriptLanguage.FORTH, ": square dup * ; 2 5 + . 9 square . words");

        runToCompletion(runtime, ComputerDeviceBus.NONE);

        assertEquals(VmFault.NONE, runtime.fault());
        assertTrue(runtime.output().startsWith("7 81 "));
        assertTrue(runtime.output().contains("front"));
        assertTrue(runtime.output().contains("mine"));
    }

    @Test
    void forthDeadLoopSuspendsAtThePerTickBudget() {
        ScriptRuntime runtime = runtime(ScriptLanguage.FORTH, "begin again");

        assertEquals(ScriptRuntime.MAX_INSTRUCTIONS_PER_TICK, runtime.executeTick(ComputerDeviceBus.NONE));
        assertTrue(runtime.running());
        assertEquals(VmFault.NONE, runtime.fault());
    }

    @Test
    void forthStackFailuresLatchWithoutEscapingTheRuntime() {
        ScriptRuntime underflow = runtime(ScriptLanguage.FORTH, "+");
        underflow.executeTick(ComputerDeviceBus.NONE);
        assertEquals(VmFault.DATA_STACK_UNDERFLOW, underflow.fault());
        assertFalse(underflow.running());

        String overflowSource = "1 ".repeat(ForthEngine.DATA_STACK_SIZE + 1);
        ScriptRuntime overflow = runtime(ScriptLanguage.FORTH, overflowSource);
        overflow.executeTick(ComputerDeviceBus.NONE);
        overflow.executeTick(ComputerDeviceBus.NONE);
        assertEquals(VmFault.DATA_STACK_OVERFLOW, overflow.fault());
        assertFalse(overflow.running());
    }

    @Test
    void forthWaitingDeviceRequestResumesFromAValidatedSnapshot() {
        ScriptRuntime original = runtime(ScriptLanguage.FORTH, "front .");
        assertEquals(1, original.executeTick((command, argument) -> ComputerDeviceBus.DeviceResult.waiting()));
        assertEquals(0, original.programCounter());

        ScriptRuntime restored = new ScriptRuntime();
        assertTrue(restored.restore(original.save()));
        AtomicInteger calls = new AtomicInteger();
        runToCompletion(restored, (command, argument) -> {
            calls.incrementAndGet();
            assertEquals(ComputerDeviceBus.DeviceCommand.MOVE_FRONT, command);
            assertEquals(0, argument);
            return ComputerDeviceBus.DeviceResult.complete(1);
        });

        assertEquals(1, calls.get());
        assertEquals("1 ", restored.output());
        assertEquals(VmFault.NONE, restored.fault());
    }

    @Test
    void forthRobotWordsPreserveHistoricalActionSignals() {
        ScriptRuntime runtime = runtime(
                ScriptLanguage.FORTH,
                "front back left right up down mine scan"
        );
        List<ComputerDeviceBus.DeviceCommand> commands = new ArrayList<>();

        runToCompletion(runtime, (command, argument) -> {
            commands.add(command);
            return ComputerDeviceBus.DeviceResult.complete(1);
        });

        assertEquals(List.of(
                ComputerDeviceBus.DeviceCommand.MOVE_FRONT,
                ComputerDeviceBus.DeviceCommand.MOVE_BACK,
                ComputerDeviceBus.DeviceCommand.ROTATE_LEFT,
                ComputerDeviceBus.DeviceCommand.ROTATE_RIGHT,
                ComputerDeviceBus.DeviceCommand.ROTATE_UP,
                ComputerDeviceBus.DeviceCommand.ROTATE_DOWN,
                ComputerDeviceBus.DeviceCommand.MINE_FRONT,
                ComputerDeviceBus.DeviceCommand.SCAN_FRONT
        ), commands);
    }

    @Test
    void lispPreservesReleasedDefinitionsListsAndQuotedSymbols() {
        ScriptRuntime runtime = runtime(
                ScriptLanguage.LISP,
                "(define x 5) (defun say-5 () (print 5)) (say-5) "
                        + "(print (+ x 2)) (print (first '(9 8))) (print 'magneticraft)"
        );

        runToCompletion(runtime, ComputerDeviceBus.NONE);

        assertEquals(VmFault.NONE, runtime.fault());
        assertTrue(runtime.output().contains("5"));
        assertTrue(runtime.output().contains("7"));
        assertTrue(runtime.output().contains("9"));
        assertTrue(runtime.output().contains("magneticraft"));
    }

    @Test
    void lispRunawayRecursionFailsWithinTheTickBudget() {
        ScriptRuntime runtime = runtime(ScriptLanguage.LISP, "(defun loop () (loop)) (loop)");

        runtime.executeTick(ComputerDeviceBus.NONE);
        runtime.executeTick(ComputerDeviceBus.NONE);

        assertEquals(VmFault.RETURN_STACK_OVERFLOW, runtime.fault());
        assertFalse(runtime.running());
    }

    @Test
    void lispRejectsGlobalsBeyondItsMemoryBudget() {
        StringBuilder source = new StringBuilder();
        for (int index = 0; index <= LispEngine.MAX_GLOBALS; index++) {
            source.append("(define value-").append(index).append(' ').append(index).append(") ");
        }
        ScriptRuntime runtime = runtime(ScriptLanguage.LISP, source.toString());

        runToCompletion(runtime, ComputerDeviceBus.NONE);

        assertEquals(VmFault.MEMORY_EXHAUSTED, runtime.fault());
    }

    @Test
    void lispGlobalsRoundTripWithoutReevaluatingCompletedForms() {
        ScriptRuntime original = runtime(ScriptLanguage.LISP, "(define answer 42) (print answer)");
        original.executeTick(ComputerDeviceBus.NONE);

        ScriptRuntime restored = new ScriptRuntime();
        assertTrue(restored.restore(original.save()));
        runToCompletion(restored, ComputerDeviceBus.NONE);

        assertEquals("42", restored.output().trim());
        assertEquals(2, restored.programCounter());
    }

    @Test
    void lispRobotFunctionsPreserveHistoricalActionSignals() {
        ScriptRuntime runtime = runtime(
                ScriptLanguage.LISP,
                "(front) (back) (left) (right) (up) (down) (mine) (scan)"
        );
        List<ComputerDeviceBus.DeviceCommand> commands = new ArrayList<>();

        runToCompletion(runtime, (command, argument) -> {
            commands.add(command);
            return ComputerDeviceBus.DeviceResult.complete(1);
        });

        assertEquals(List.of(
                ComputerDeviceBus.DeviceCommand.MOVE_FRONT,
                ComputerDeviceBus.DeviceCommand.MOVE_BACK,
                ComputerDeviceBus.DeviceCommand.ROTATE_LEFT,
                ComputerDeviceBus.DeviceCommand.ROTATE_RIGHT,
                ComputerDeviceBus.DeviceCommand.ROTATE_UP,
                ComputerDeviceBus.DeviceCommand.ROTATE_DOWN,
                ComputerDeviceBus.DeviceCommand.MINE_FRONT,
                ComputerDeviceBus.DeviceCommand.SCAN_FRONT
        ), commands);
    }

    @Test
    void shellUsesOnlyTheBoundedVirtualDisk() {
        ScriptRuntime runtime = runtime(
                ScriptLanguage.SHELL,
                "format\nmkdir docs\nwrite docs/readme hello world\ncat docs/readme\nls docs\nfree"
        );

        runToCompletion(runtime, ComputerDeviceBus.NONE);

        assertEquals(VmFault.NONE, runtime.fault());
        assertTrue(runtime.output().contains("hello world"));
        assertTrue(runtime.output().contains("readme"));
        assertTrue(runtime.output().contains("bytes free"));
        VirtualDisk disk = runtime.virtualDisk().orElseThrow();
        assertEquals("hello world", disk.read("/", "docs/readme").orElseThrow());
    }

    @Test
    void shellRejectsLegacyOutboundNetworkCommands() {
        assertThrows(IllegalArgumentException.class, () -> runtime(ScriptLanguage.SHELL, "pastebin get abc"));
        assertThrows(IllegalArgumentException.class, () -> runtime(ScriptLanguage.SHELL, "update"));
    }

    @Test
    void shellQuarryRetriesWaitAndAdvancesOnce() {
        ScriptRuntime runtime = runtime(ScriptLanguage.SHELL, "quarry 10");
        List<Integer> arguments = new ArrayList<>();

        assertEquals(1, runtime.executeTick((command, argument) -> {
            arguments.add(argument);
            return ComputerDeviceBus.DeviceResult.waiting();
        }));
        assertTrue(runtime.running());
        assertEquals(0, runtime.programCounter());

        assertEquals(1, runtime.executeTick((command, argument) -> {
            arguments.add(argument);
            return ComputerDeviceBus.DeviceResult.complete(1);
        }));
        assertFalse(runtime.running());
        assertEquals(List.of(10, 10), arguments);
        assertTrue(runtime.output().contains("Quarry finished"));
    }

    @Test
    void malformedOrFutureSnapshotsFailClosed() {
        ScriptRuntime runtime = runtime(ScriptLanguage.FORTH, "1 .");
        CompoundTag snapshot = runtime.save();
        snapshot.putInt("schema_version", Integer.MAX_VALUE);

        ScriptRuntime restored = new ScriptRuntime();
        assertFalse(restored.restore(snapshot));
        assertFalse(restored.hasProgram());
        assertEquals(VmFault.INVALID_SNAPSHOT, restored.fault());
        assertEquals(0, restored.executeTick(ComputerDeviceBus.NONE));
    }

    @Test
    void sourcePayloadIsUtf8BoundedAndRejectsNul() {
        assertEquals(3, new ScriptProgram(ScriptLanguage.FORTH, "中").encodedSize());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScriptProgram(ScriptLanguage.FORTH, "a".repeat(ScriptRuntime.MAX_SOURCE_BYTES + 1))
        );
        assertThrows(IllegalArgumentException.class, () -> new ScriptProgram(ScriptLanguage.LISP, "a\u0000b"));
    }

    private static ScriptRuntime runtime(ScriptLanguage language, String source) {
        ScriptRuntime runtime = new ScriptRuntime();
        runtime.replaceProgram(new ScriptProgram(language, source));
        return runtime;
    }

    private static void runToCompletion(ScriptRuntime runtime, ComputerDeviceBus bus) {
        int ticks = 0;
        while (runtime.running() && ticks++ < 1_024) {
            runtime.executeTick(bus);
        }
        assertFalse(runtime.running(), "script did not complete within the test limit");
    }
}
