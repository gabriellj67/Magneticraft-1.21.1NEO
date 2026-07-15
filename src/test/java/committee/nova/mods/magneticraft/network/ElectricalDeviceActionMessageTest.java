package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceAction;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElectricalDeviceActionMessageTest {
    @Test
    void boundedActionRoundTrips() {
        ElectricalDeviceActionMessage message = new ElectricalDeviceActionMessage(
                new BlockPos(-7, 42, 19),
                ElectricalDeviceAction.CYCLE_REDSTONE_MODE
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ElectricalDeviceActionMessage.encode(message, buffer);
            assertEquals(message, ElectricalDeviceActionMessage.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void decoderRejectsUnknownAction() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeVarInt(ElectricalDeviceAction.values().length);
            assertThrows(DecoderException.class, () -> ElectricalDeviceActionMessage.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
