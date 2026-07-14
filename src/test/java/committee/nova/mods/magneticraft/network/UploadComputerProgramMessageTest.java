package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadComputerProgramMessageTest {
    @Test
    void boundedUploadRoundTripsSessionSequenceAndUtf8Source() {
        UploadComputerProgramMessage message = new UploadComputerProgramMessage(
                new BlockPos(1, 2, 3),
                7L,
                Long.MIN_VALUE,
                4,
                new ScriptProgram(ScriptLanguage.LISP, "(print '你好)")
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        UploadComputerProgramMessage.encode(message, buffer);

        assertEquals(message, UploadComputerProgramMessage.decode(buffer));
        buffer.release();
    }

    @Test
    void constructorRejectsOversizedSourceAndNegativeSequence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UploadComputerProgramMessage(
                        BlockPos.ZERO,
                        0L,
                        1L,
                        0,
                        new ScriptProgram(ScriptLanguage.FORTH, "x".repeat(ScriptRuntime.MAX_SOURCE_BYTES + 1))
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new UploadComputerProgramMessage(
                        BlockPos.ZERO,
                        0L,
                        1L,
                        -1,
                        new ScriptProgram(ScriptLanguage.FORTH, "")
                )
        );
    }

    @Test
    void decoderRejectsUnknownLanguageBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(BlockPos.ZERO);
        buffer.writeVarLong(0L);
        buffer.writeLong(1L);
        buffer.writeVarInt(0);
        buffer.writeUtf("unknown", 16);
        buffer.writeUtf("", ScriptRuntime.MAX_SOURCE_BYTES);

        assertThrows(DecoderException.class, () -> UploadComputerProgramMessage.decode(buffer));
        buffer.release();
    }
}
