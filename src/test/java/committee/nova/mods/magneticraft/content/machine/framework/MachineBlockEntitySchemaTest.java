package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineBlockEntitySchemaTest {
    @Test
    void currentSchemaRoundTripIsAccepted() {
        CompoundTag tag = new CompoundTag();

        MachineBlockEntity.writeSchemaVersion(tag, MachineBlockEntity.INITIAL_SCHEMA_VERSION);

        assertEquals(1, tag.getInt("schema_version"));
        assertTrue(MachineBlockEntity.hasSchema(tag, MachineBlockEntity.INITIAL_SCHEMA_VERSION));
    }

    @Test
    void missingWrongTypeAndUnsupportedSchemasAreRejected() {
        assertFalse(MachineBlockEntity.hasSchema(new CompoundTag(), 1));

        CompoundTag wrongType = new CompoundTag();
        wrongType.put("schema_version", StringTag.valueOf("1"));
        assertFalse(MachineBlockEntity.hasSchema(wrongType, 1));

        CompoundTag future = new CompoundTag();
        future.putInt("schema_version", 2);
        assertFalse(MachineBlockEntity.hasSchema(future, 1));
    }
}
