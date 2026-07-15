package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncVoltageTiersMessageTest {
    @AfterEach
    void resetClientSnapshot() {
        ClientVoltageTierRegistry.reset();
    }

    @Test
    void boundedMessageRoundTripsWithoutChangingFields() {
        SyncVoltageTiersMessage expected = new SyncVoltageTiersMessage(7L, List.of(
                display("low_voltage", 60.0, 120.0, 125.0, 0xD98245),
                display("medium_voltage", 240.0, 480.0, 500.0, 0xE5C84B)
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncVoltageTiersMessage.encode(expected, buffer);
            assertEquals(expected, SyncVoltageTiersMessage.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void decoderRejectsCountAndNumericBoundariesBeforeAllocatingEntries() {
        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        try {
            oversized.writeVarLong(1L);
            oversized.writeVarInt(VoltageTier.MAX_SYNCED_TIERS + 1);
            assertThrows(DecoderException.class, () -> SyncVoltageTiersMessage.decode(oversized));
        } finally {
            oversized.release();
        }

        assertThrows(IllegalArgumentException.class, () -> new SyncVoltageTiersMessage(0L, List.of(
                display("low_voltage", 60.0, 120.0, 125.0, 0xD98245)
        )));
        assertThrows(IllegalArgumentException.class, () -> new SyncVoltageTiersMessage(1L, List.of(
                new VoltageTierDisplay(
                        ResourceLocation.fromNamespaceAndPath("magneticraft", "bad"),
                        "voltage_tier.magneticraft.bad",
                        Double.NaN,
                        120.0,
                        125.0,
                        0
                )
        )));

        ArrayList<VoltageTierDisplay> tooMany = new ArrayList<>();
        for (int index = 0; index <= VoltageTier.MAX_SYNCED_TIERS; index++) {
            tooMany.add(display("tier_" + index, 1.0, 2.0, 3.0, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new SyncVoltageTiersMessage(1L, tooMany));
    }

    @Test
    void clientCacheIsImmutableAndRejectsStaleGenerations() {
        VoltageTierDisplay low = display("low_voltage", 60.0, 120.0, 125.0, 0xD98245);
        VoltageTierDisplay medium = display("medium_voltage", 240.0, 480.0, 500.0, 0xE5C84B);

        assertTrue(ClientVoltageTierRegistry.apply(4L, List.of(low)));
        assertFalse(ClientVoltageTierRegistry.apply(3L, List.of(medium)));
        assertEquals(low, ClientVoltageTierRegistry.current().tier(low.id()).orElseThrow());
        assertThrows(
                UnsupportedOperationException.class,
                () -> ClientVoltageTierRegistry.current().tiers().put(medium.id(), medium)
        );
    }

    private static VoltageTierDisplay display(
            String path,
            double minimum,
            double nominal,
            double maximum,
            int color
    ) {
        return new VoltageTierDisplay(
                ResourceLocation.fromNamespaceAndPath("magneticraft", path),
                "voltage_tier.magneticraft." + path,
                minimum,
                nominal,
                maximum,
                color
        );
    }
}
