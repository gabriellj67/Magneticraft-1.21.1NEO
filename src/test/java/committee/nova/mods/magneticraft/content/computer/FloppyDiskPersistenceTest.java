package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloppyDiskPersistenceTest {
    private static final List<ComputerInstruction> PROGRAM = List.of(
            new ComputerInstruction(ComputerOpcode.SET, 0, 42),
            new ComputerInstruction(ComputerOpcode.HALT, 0, 0)
    );

    @Test
    void currentSchemaRoundTripPreservesProgram() {
        CompoundTag tag = new CompoundTag();

        FloppyDiskPersistence.write(tag, PROGRAM);

        assertEquals(1, tag.getInt("schema_version"));
        assertEquals(PROGRAM, FloppyDiskPersistence.read(tag).orElseThrow());
    }

    @Test
    void missingAndUnsupportedSchemasResetWithoutReadingProgram() {
        CompoundTag legacyTag = new CompoundTag();
        ProgramNbt.writeProgram(legacyTag, "program", PROGRAM);
        assertEquals(List.of(), FloppyDiskPersistence.read(legacyTag).orElseThrow());

        CompoundTag futureTag = new CompoundTag();
        FloppyDiskPersistence.write(futureTag, PROGRAM);
        futureTag.putInt("schema_version", 2);
        assertEquals(List.of(), FloppyDiskPersistence.read(futureTag).orElseThrow());
    }
}
