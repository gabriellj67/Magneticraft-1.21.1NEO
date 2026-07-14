package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockMembershipSavedDataTest {
    private static final BlockPos FIRST_CONTROLLER = new BlockPos(1, 2, 3);
    private static final BlockPos SECOND_CONTROLLER = new BlockPos(9, 2, 3);
    private static final BlockPos FIRST_MEMBER = new BlockPos(2, 2, 3);
    private static final BlockPos SECOND_MEMBER = new BlockPos(3, 2, 3);

    @Test
    void currentSchemaRoundTripPreservesControllerAndDefinition() {
        MultiblockMembershipSavedData data = new MultiblockMembershipSavedData();
        assertTrue(data.claim(
                FIRST_CONTROLLER,
                MultiblockDefinition.GRINDER.id(),
                List.of(FIRST_MEMBER, SECOND_MEMBER)
        ));

        CompoundTag encoded = data.save(new CompoundTag());
        MultiblockMembershipSavedData restored = MultiblockMembershipSavedData.load(encoded);

        assertEquals(1, encoded.getInt("schema_version"));
        assertEquals(2, restored.size());
        assertEquals(
                new MultiblockMembershipSavedData.Membership(
                        FIRST_CONTROLLER, MultiblockDefinition.GRINDER.id()),
                restored.membershipAt(FIRST_MEMBER).orElseThrow()
        );
    }

    @Test
    void replacingProjectionRemovesOldMembersWithoutStealingAnotherOwner() {
        MultiblockMembershipSavedData data = new MultiblockMembershipSavedData();
        assertTrue(data.claim(
                FIRST_CONTROLLER,
                MultiblockDefinition.GRINDER.id(),
                List.of(FIRST_MEMBER, SECOND_MEMBER)
        ));
        assertTrue(data.claim(
                FIRST_CONTROLLER,
                MultiblockDefinition.GRINDER.id(),
                List.of(SECOND_MEMBER)
        ));

        assertTrue(data.membershipAt(FIRST_MEMBER).isEmpty());
        assertEquals(FIRST_CONTROLLER, data.membershipAt(SECOND_MEMBER).orElseThrow().controller());
        assertFalse(data.claim(
                SECOND_CONTROLLER,
                MultiblockDefinition.SIEVE.id(),
                List.of(SECOND_MEMBER)
        ));
        assertEquals(FIRST_CONTROLLER, data.membershipAt(SECOND_MEMBER).orElseThrow().controller());
    }

    @Test
    void releaseRequiresTheRecordedOwnerAndUnsupportedSchemaFailsClosed() {
        MultiblockMembershipSavedData data = new MultiblockMembershipSavedData();
        assertTrue(data.claim(
                FIRST_CONTROLLER,
                MultiblockDefinition.GRINDER.id(),
                List.of(FIRST_MEMBER)
        ));
        assertFalse(data.releaseMember(FIRST_MEMBER, SECOND_CONTROLLER));
        assertTrue(data.releaseMember(FIRST_MEMBER, FIRST_CONTROLLER));
        assertEquals(0, data.size());

        CompoundTag unsupported = new CompoundTag();
        unsupported.putInt("schema_version", 2);
        unsupported.put("memberships", new net.minecraft.nbt.ListTag());
        assertEquals(0, MultiblockMembershipSavedData.load(unsupported).size());
    }
}
