package committee.nova.mods.magneticraft.system.network.longdistance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireSelectionPayloadTest {
    private static final WireSelectionPayload.Selection SELECTION = new WireSelectionPayload.Selection(
            new ResourceLocation("minecraft", "overworld"),
            new BlockPos(12, 64, -8)
    );

    @Test
    void currentSchemaRoundTripsInsideOwnedSubtag() {
        CompoundTag owner = new CompoundTag();
        owner.putString("unrelated", "preserved");

        WireSelectionPayload.write(owner, SELECTION);

        assertFalse(owner.contains("schema_version"));
        assertFalse(owner.contains("dimension"));
        assertTrue(owner.contains("wire_selection", Tag.TAG_COMPOUND));
        CompoundTag payload = owner.getCompound("wire_selection");
        assertEquals(WireSelectionPayload.SCHEMA_VERSION, payload.getInt("schema_version"));
        assertEquals("minecraft:overworld", payload.getString("dimension"));
        assertEquals(12, payload.getInt("x"));
        assertEquals(64, payload.getInt("y"));
        assertEquals(-8, payload.getInt("z"));
        assertEquals(SELECTION, WireSelectionPayload.read(owner).orElseThrow());
        assertEquals("preserved", owner.getString("unrelated"));
    }

    @Test
    void legacyAndFutureSchemasAreRejected() {
        CompoundTag legacy = encodedSelection();
        legacy.getCompound("wire_selection").remove("schema_version");
        assertTrue(WireSelectionPayload.read(legacy).isEmpty());

        CompoundTag future = encodedSelection();
        future.getCompound("wire_selection").putInt(
                "schema_version",
                WireSelectionPayload.SCHEMA_VERSION + 1
        );
        assertTrue(WireSelectionPayload.read(future).isEmpty());
    }

    @Test
    void malformedSelectionsAreRejected() {
        CompoundTag wrongOwnerType = new CompoundTag();
        wrongOwnerType.putString("wire_selection", "invalid");
        assertTrue(WireSelectionPayload.read(wrongOwnerType).isEmpty());

        CompoundTag invalidDimension = encodedSelection();
        invalidDimension.getCompound("wire_selection").putString("dimension", "Invalid Dimension");
        assertTrue(WireSelectionPayload.read(invalidDimension).isEmpty());

        CompoundTag missingCoordinate = encodedSelection();
        missingCoordinate.getCompound("wire_selection").remove("z");
        assertTrue(WireSelectionPayload.read(missingCoordinate).isEmpty());

        CompoundTag wrongCoordinateType = encodedSelection();
        wrongCoordinateType.getCompound("wire_selection").putString("x", "12");
        assertTrue(WireSelectionPayload.read(wrongCoordinateType).isEmpty());
    }

    @Test
    void clearRemovesOnlyTheOwnedSelection() {
        CompoundTag owner = encodedSelection();
        owner.putInt("unrelated", 42);

        WireSelectionPayload.clear(owner);

        assertFalse(owner.contains("wire_selection"));
        assertEquals(42, owner.getInt("unrelated"));
        assertTrue(WireSelectionPayload.read(owner).isEmpty());
    }

    private static CompoundTag encodedSelection() {
        CompoundTag owner = new CompoundTag();
        WireSelectionPayload.write(owner, SELECTION);
        return owner;
    }
}
