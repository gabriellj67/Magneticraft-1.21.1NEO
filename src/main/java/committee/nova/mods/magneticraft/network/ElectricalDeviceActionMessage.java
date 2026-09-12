package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceAction;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

/** C2S control intent bound to the sender's currently open electrical-device menu. */
public record ElectricalDeviceActionMessage(
        BlockPos position,
        ElectricalDeviceAction action
) implements CustomPacketPayload {
    public static final Type<ElectricalDeviceActionMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Magneticraft.MOD_ID, "electrical_device_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElectricalDeviceActionMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ElectricalDeviceActionMessage::position,
            ByteBufCodecs.VAR_INT.map(ElectricalDeviceAction::decode, ElectricalDeviceAction::ordinal),
            ElectricalDeviceActionMessage::action,
            ElectricalDeviceActionMessage::new
    );

    public ElectricalDeviceActionMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(action, "action");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            ElectricalDeviceActionMessage message,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> applyIfValid((ServerPlayer) context.player(), message));
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
