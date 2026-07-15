package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceAction;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** C2S control intent bound to the sender's currently open electrical-device menu. */
public record ElectricalDeviceActionMessage(BlockPos position, ElectricalDeviceAction action) {
    public ElectricalDeviceActionMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(action, "action");
    }

    public static void encode(ElectricalDeviceActionMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.position);
        buffer.writeVarInt(message.action.ordinal());
    }

    public static ElectricalDeviceActionMessage decode(FriendlyByteBuf buffer) {
        return new ElectricalDeviceActionMessage(
                buffer.readBlockPos(),
                ElectricalDeviceAction.decode(buffer.readVarInt())
        );
    }

    public static void handle(
            ElectricalDeviceActionMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> applyIfValid(sender, message));
        context.setPacketHandled(true);
    }

    static boolean applyIfValid(ServerPlayer sender, ElectricalDeviceActionMessage message) {
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
        ) > 64.0D
                || !(sender.containerMenu instanceof ElectricalDeviceMenu menu)
                || !menu.position().equals(message.position)
                || !menu.stillValid(sender)) {
            return false;
        }
        return menu.applyAction(sender, message.action);
    }
}
