package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** C2S script replacement bound to one open, replay-protected programmable menu. */
public record UploadComputerProgramMessage(
        BlockPos position,
        long expectedRevision,
        long sessionToken,
        int sequence,
        ScriptProgram program
) {
    public UploadComputerProgramMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(program, "program");
        if (expectedRevision < 0L) {
            throw new IllegalArgumentException("Program revision must be non-negative");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("Upload sequence must be non-negative");
        }
    }

    public static void encode(UploadComputerProgramMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.position);
        buffer.writeVarLong(message.expectedRevision);
        buffer.writeLong(message.sessionToken);
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.program.language().serializedName(), 16);
        buffer.writeUtf(message.program.source(), ScriptRuntime.MAX_SOURCE_BYTES);
    }

    public static UploadComputerProgramMessage decode(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        long expectedRevision = buffer.readVarLong();
        long sessionToken = buffer.readLong();
        int sequence = buffer.readVarInt();
        if (expectedRevision < 0L || sequence < 0) {
            throw new DecoderException("Negative computer upload revision or sequence");
        }
        ScriptLanguage language = ScriptLanguage.parse(buffer.readUtf(16))
                .orElseThrow(() -> new DecoderException("Unknown computer language"));
        try {
            return new UploadComputerProgramMessage(
                    position,
                    expectedRevision,
                    sessionToken,
                    sequence,
                    new ScriptProgram(language, buffer.readUtf(ScriptRuntime.MAX_SOURCE_BYTES))
            );
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid computer script", exception);
        }
    }

    public static void handle(
            UploadComputerProgramMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> applyFromOpenMenu(sender, message));
        context.setPacketHandled(true);
    }

    static boolean applyFromOpenMenu(ServerPlayer sender, UploadComputerProgramMessage message) {
        return sender != null
                && message != null
                && sender.containerMenu instanceof ProgrammableMenu menu
                && menu.applyUpload(
                sender,
                message.position,
                message.expectedRevision,
                message.sessionToken,
                message.sequence,
                message.program
        );
    }
}
