package committee.nova.mods.magneticraft.content.machine.windturbine;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindTurbineMenuTest {
    @Test
    void missingExtendedOpeningDataFallsBackWithoutCrashing() {
        assertEquals(BlockPos.ZERO, WindTurbineMenu.readPosition(null));

        FriendlyByteBuf emptyBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertEquals(BlockPos.ZERO, WindTurbineMenu.readPosition(emptyBuffer));
        } finally {
            emptyBuffer.release();
        }
    }

    @Test
    void extendedOpeningDataPreservesControllerPosition() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BlockPos expected = new BlockPos(12, 64, -9);
        try {
            buffer.writeBlockPos(expected);
            assertEquals(expected, WindTurbineMenu.readPosition(buffer));
        } finally {
            buffer.release();
        }
    }
}
