package committee.nova.mods.magneticraft.system.network.electric.item;

import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TieredElectricalItemDataTest {
    private static final ResourceLocation RATING = ResourceLocation.fromNamespaceAndPath("magneticraft", "standard_8c");
    private static final ResourceLocation TRANSFORMER =
            ResourceLocation.fromNamespaceAndPath("magneticraft", "low_to_medium");
    private static final TieredElectricalItemData COMPLETE = new TieredElectricalItemData(
            VoltageTierIds.MEDIUM,
            Optional.of(RATING),
            Optional.of(TRANSFORMER)
    );

    @Test
    void itemTagRoundTripPreservesIdentityWithoutPersistingNodeEnergy() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        COMPLETE.write(stack);

        assertEquals(COMPLETE, TieredElectricalItemData.read(stack).orElseThrow());
        CompoundTag payload = stack.getTagElement(TieredElectricalItemData.OWNER_TAG);
        assertEquals(TieredElectricalItemData.SCHEMA_VERSION, payload.getInt("schema_version"));
        assertFalse(payload.contains("node_joules"));
        assertFalse(payload.contains("energy"));
        assertEquals(
                "tier=magneticraft:medium_voltage;rating=magneticraft:standard_8c;"
                        + "transformer=magneticraft:low_to_medium",
                COMPLETE.subtypeKey()
        );
    }

    @Test
    void jsonAndNetworkRoundTripsPreserveOptionalIdentifiers() {
        assertEquals(COMPLETE, TieredElectricalItemData.fromJson(COMPLETE.toJson()));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            COMPLETE.writeNetwork(buffer);
            assertEquals(COMPLETE, TieredElectricalItemData.readNetwork(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void missingFutureAndMalformedPayloadsFailClosed() {
        assertTrue(TieredElectricalItemData.fromTag(new CompoundTag()).isEmpty());

        CompoundTag future = COMPLETE.toTag();
        future.putInt("schema_version", TieredElectricalItemData.SCHEMA_VERSION + 1);
        assertTrue(TieredElectricalItemData.fromTag(future).isEmpty());

        CompoundTag malformedTier = COMPLETE.toTag();
        malformedTier.putString("tier_id", "Invalid Tier");
        assertTrue(TieredElectricalItemData.fromTag(malformedTier).isEmpty());

        CompoundTag malformedOptional = COMPLETE.toTag();
        malformedOptional.putInt("rating_id", 8);
        assertTrue(TieredElectricalItemData.fromTag(malformedOptional).isEmpty());

        ItemStack plain = new ItemStack(Items.IRON_INGOT);
        assertTrue(TieredElectricalItemData.read(plain).isEmpty());
    }
}
