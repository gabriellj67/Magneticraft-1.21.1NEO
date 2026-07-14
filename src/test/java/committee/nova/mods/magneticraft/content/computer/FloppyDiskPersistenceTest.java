package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloppyDiskPersistenceTest {
    private static final List<ComputerInstruction> PROGRAM = List.of(
            new ComputerInstruction(ComputerOpcode.SET, 0, 42),
            new ComputerInstruction(ComputerOpcode.HALT, 0, 0)
    );

    @Test
    void currentSchemaRoundTripPreservesLegacyCandidateProgram() {
        CompoundTag tag = new CompoundTag();

        FloppyDiskPersistence.writeLegacy(tag, PROGRAM);
        FloppyDiskPersistence.State state = FloppyDiskPersistence.read(tag).orElseThrow();

        assertEquals(2, tag.getInt("schema_version"));
        assertEquals(PROGRAM, state.legacyProgram().orElseThrow());
        assertFalse(state.readOnly());
    }

    @Test
    void schemaOneProgramMigratesWithoutChangingItsInstructions() {
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putInt("schema_version", 1);
        ProgramNbt.writeProgram(legacyTag, "program", PROGRAM);

        FloppyDiskPersistence.State migrated = FloppyDiskPersistence.read(legacyTag).orElseThrow();

        assertEquals(PROGRAM, migrated.legacyProgram().orElseThrow());
        assertEquals("user", migrated.preset());
    }

    @Test
    void shellProgramAndVirtualDiskRoundTripTogether() {
        VirtualDisk disk = new VirtualDisk();
        disk.format();
        assertTrue(disk.write("/", "startup.sh", "quarry 10"));
        ScriptProgram script = new ScriptProgram(ScriptLanguage.SHELL, "cat startup.sh");
        CompoundTag tag = new CompoundTag();

        FloppyDiskPersistence.writeScript(tag, script, Optional.of(disk));
        FloppyDiskPersistence.State restored = FloppyDiskPersistence.read(tag).orElseThrow();

        assertEquals(script, restored.scriptProgram().orElseThrow());
        assertEquals("quarry 10", restored.disk().orElseThrow().read("/", "startup.sh").orElseThrow());
    }

    @Test
    void presetMediaIsReadOnlyAndCanRepresentInertHistoricalTools() {
        CompoundTag forth = new CompoundTag();
        FloppyDiskPersistence.writePreset(forth, "forth", ScriptLanguage.FORTH);
        FloppyDiskPersistence.State executable = FloppyDiskPersistence.read(forth).orElseThrow();
        assertTrue(executable.readOnly());
        assertEquals(ScriptLanguage.FORTH, executable.scriptProgram().orElseThrow().language());

        CompoundTag editor = new CompoundTag();
        FloppyDiskPersistence.writePreset(editor, "editor", null);
        FloppyDiskPersistence.State inert = FloppyDiskPersistence.read(editor).orElseThrow();
        assertTrue(inert.readOnly());
        assertFalse(inert.executable());
    }

    @Test
    void malformedAndFutureSchemasFailClosed() {
        CompoundTag missingVersion = new CompoundTag();
        ProgramNbt.writeProgram(missingVersion, "program", PROGRAM);
        assertFalse(FloppyDiskPersistence.read(missingVersion).orElseThrow().executable());

        CompoundTag futureTag = new CompoundTag();
        FloppyDiskPersistence.writeLegacy(futureTag, PROGRAM);
        futureTag.putInt("schema_version", Integer.MAX_VALUE);
        assertTrue(FloppyDiskPersistence.read(futureTag).isEmpty());

        CompoundTag corruptScript = new CompoundTag();
        FloppyDiskPersistence.writeScript(
                corruptScript,
                new ScriptProgram(ScriptLanguage.LISP, "(print 5)"),
                Optional.empty()
        );
        corruptScript.getCompound("script").putString("language", "unknown");
        assertTrue(FloppyDiskPersistence.read(corruptScript).isEmpty());
    }
}
