package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

/** C2S script replacement bound to one open, replay-protected programmable menu. */
public record UploadComputerProgramMessage(
        BlockPos position,
        long expectedRevision,
        long sessionToken,
        int sequence,
        ScriptProgram program
) implements CustomPacketPayload {
    public static final Type<UploadComputerProgramMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Magneticraft.MOD_ID, "upload_computer_program"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadComputerProgramMessage> STREAM_CODEC = StreamCodec.of(
            UploadComputerProgramMessage::encode,
            UploadComputerProgramMessage::decode
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, UploadComputerProgramMessage message) {
        buffer.writeBlockPos(message.position);
        buffer.writeVarLong(message.expectedRevision);
        buffer.writeLong(message.sessionToken);
        buffer.writeVarInt(message.sequence);
        buffer.writeUtf(message.program.language().serializedName(), 16);
        buffer.writeUtf(message.program.source(), ScriptRuntime.MAX_SOURCE_BYTES);
    }

    private static UploadComputerProgramMessage decode(RegistryFriendlyByteBuf buffer) {
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
            IPayloadContext context
    ) {
        context.enqueueWork(() -> applyFromOpenMenu((ServerPlayer) context.player(), message));
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
