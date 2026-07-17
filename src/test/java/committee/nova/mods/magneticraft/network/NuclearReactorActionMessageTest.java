package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorAction;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NuclearReactorActionMessageTest {
    @Test
    void boundedControlActionRoundTrips() {
        NuclearReactorActionMessage message = new NuclearReactorActionMessage(
                new BlockPos(-12, 64, 9),
                new NuclearReactorAction(
                        NuclearReactorAction.Type.SET_ROD_GROUP,
                        ReactorRodGroup.C, null, null, 425, false));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NuclearReactorActionMessage.encode(message, buffer);
            assertEquals(message, NuclearReactorActionMessage.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void boundedFuelCoordinateRoundTrips() {
        NuclearReactorActionMessage message = new NuclearReactorActionMessage(
                BlockPos.ZERO,
                new NuclearReactorAction(
                        NuclearReactorAction.Type.LOAD_FUEL_FROM_HAND,
                        null, null, new ReactorColumnCoordinate(8, 8), 0, false));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NuclearReactorActionMessage.encode(message, buffer);
            assertEquals(message, NuclearReactorActionMessage.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void decoderRejectsOutOfBoundsCoordinatesAndValues() {
        FriendlyByteBuf coordinate = rawAction(9, 0, 0);
        FriendlyByteBuf value = rawAction(-1, -1, 1001);
        try {
            assertThrows(DecoderException.class, () -> NuclearReactorActionMessage.decode(coordinate));
            assertThrows(DecoderException.class, () -> NuclearReactorActionMessage.decode(value));
        } finally {
            coordinate.release();
            value.release();
        }
    }

    private static FriendlyByteBuf rawAction(int x, int z, int value) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(BlockPos.ZERO);
        buffer.writeVarInt(NuclearReactorAction.Type.SET_TARGET_POWER.ordinal());
        buffer.writeVarInt(-1);
        buffer.writeVarInt(-1);
        buffer.writeVarInt(x);
        buffer.writeVarInt(z);
        buffer.writeVarInt(value);
        buffer.writeBoolean(false);
        return buffer;
    }
}
