package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Optional;

/** Current-format codec for the state owned by one floppy disk. */
final class FloppyDiskPersistence {
    static final String SCHEMA_VERSION_TAG = "schema_version";
    static final int SCHEMA_VERSION = 1;
    private static final String PROGRAM_TAG = "program";

    private FloppyDiskPersistence() {
    }

    static void write(CompoundTag tag, List<ComputerInstruction> program) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        ProgramNbt.writeProgram(tag, PROGRAM_TAG, program);
    }

    static Optional<List<ComputerInstruction>> read(CompoundTag tag) {
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION) {
            return Optional.of(List.of());
        }
        return ProgramNbt.readProgram(tag, PROGRAM_TAG);
    }
}
