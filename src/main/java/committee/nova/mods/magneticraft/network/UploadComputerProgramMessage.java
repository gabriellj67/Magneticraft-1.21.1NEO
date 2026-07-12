package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.computer.ProgrammableBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * C2S intent that atomically replaces one nearby owned computer program.
 */
public record UploadComputerProgramMessage(
        BlockPos position,
        long expectedRevision,
        List<ComputerInstruction> program
) {
    public UploadComputerProgramMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        if (expectedRevision < 0L) {
            throw new IllegalArgumentException("Program revision must be non-negative");
        }
        program = BoundedComputerVm.validatedProgramCopy(program);
    }

    public static void encode(UploadComputerProgramMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.position);
        buffer.writeVarLong(message.expectedRevision);
        buffer.writeVarInt(message.program.size());
        for (ComputerInstruction instruction : message.program) {
            buffer.writeVarInt(instruction.opcode().networkId());
            buffer.writeVarInt(instruction.operandA());
            buffer.writeVarInt(instruction.operandB());
        }
    }

    public static UploadComputerProgramMessage decode(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        long expectedRevision = buffer.readVarLong();
        int instructionCount = buffer.readVarInt();
        if (expectedRevision < 0L) {
            throw new DecoderException("Negative computer program revision");
        }
        if (instructionCount < 0 || instructionCount > BoundedComputerVm.MAX_PROGRAM_LENGTH) {
            throw new DecoderException("Computer program exceeds instruction limit");
        }
        List<ComputerInstruction> program = new ArrayList<>(instructionCount);
        for (int index = 0; index < instructionCount; index++) {
            int opcodeId = buffer.readVarInt();
            ComputerOpcode opcode = ComputerOpcode.fromNetworkId(opcodeId)
                    .orElseThrow(() -> new DecoderException("Unknown computer opcode " + opcodeId));
            program.add(new ComputerInstruction(opcode, buffer.readVarInt(), buffer.readVarInt()));
        }
        try {
            return new UploadComputerProgramMessage(position, expectedRevision, program);
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid computer program operands", exception);
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

    private static boolean applyFromOpenMenu(ServerPlayer sender, UploadComputerProgramMessage message) {
        if (sender == null
                || !(sender.containerMenu instanceof ProgrammableMenu menu)
                || !menu.position().equals(message.position)
                || menu.revision() != message.expectedRevision
                || !menu.stillValid(sender)) {
            return false;
        }
        return applyIfValid(sender, message);
    }

    public static boolean applyIfValid(Player sender, UploadComputerProgramMessage message) {
        if (sender == null
                || message == null
                || sender.level().isClientSide
                || !sender.getAbilities().mayBuild
                || !sender.level().getWorldBorder().isWithinBounds(message.position)
                || !sender.level().hasChunk(message.position.getX() >> 4, message.position.getZ() >> 4)
                || sender.distanceToSqr(
                        message.position.getX() + 0.5D,
                        message.position.getY() + 0.5D,
                        message.position.getZ() + 0.5D
                ) > 64.0D) {
            return false;
        }
        if (!(sender.level().getBlockEntity(message.position) instanceof ProgrammableBlockEntity programmable)
                || !programmable.canManage(sender)
                || programmable.programRevision() != message.expectedRevision) {
            return false;
        }
        return programmable.tryReplaceProgram(message.expectedRevision, message.program);
    }
}
