package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorAction;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorControlMode;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** C2S PWR action bound to the sender's currently open controller menu. */
public record NuclearReactorActionMessage(BlockPos position, NuclearReactorAction action) {
    private static final int MAX_CORE_COORDINATE = 8;

    public NuclearReactorActionMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(action, "action");
    }

    public static void encode(NuclearReactorActionMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.position);
        buffer.writeEnum(message.action.type());
        buffer.writeVarInt(message.action.group() == null ? -1 : message.action.group().ordinal());
        buffer.writeVarInt(message.action.mode() == null ? -1 : message.action.mode().ordinal());
        buffer.writeVarInt(message.action.coordinate() == null ? -1 : message.action.coordinate().x());
        buffer.writeVarInt(message.action.coordinate() == null ? -1 : message.action.coordinate().z());
        buffer.writeVarInt(message.action.value());
        buffer.writeBoolean(message.action.confirmed());
    }

    public static NuclearReactorActionMessage decode(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        NuclearReactorAction.Type type = requiredEnum(
                NuclearReactorAction.Type.values(), buffer.readVarInt(), "action type");
        int groupOrdinal = buffer.readVarInt();
        int modeOrdinal = buffer.readVarInt();
        int x = buffer.readVarInt();
        int z = buffer.readVarInt();
        int value = buffer.readVarInt();
        boolean confirmed = buffer.readBoolean();
        ReactorRodGroup group = nullableEnum(ReactorRodGroup.values(), groupOrdinal, "rod group");
        ReactorControlMode mode = nullableEnum(ReactorControlMode.values(), modeOrdinal, "control mode");
        if ((x == -1) != (z == -1) || x < -1 || z < -1
                || x > MAX_CORE_COORDINATE || z > MAX_CORE_COORDINATE) {
            throw new DecoderException("Invalid reactor core coordinate " + x + "," + z);
        }
        if (value < 0 || value > 1000) {
            throw new DecoderException("Invalid reactor action value " + value);
        }
        ReactorColumnCoordinate coordinate = x == -1 ? null : new ReactorColumnCoordinate(x, z);
        try {
            return new NuclearReactorActionMessage(position,
                    new NuclearReactorAction(type, group, mode, coordinate, value, confirmed));
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid reactor action", exception);
        }
    }

    public static void handle(
            NuclearReactorActionMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> applyIfValid(sender, message));
        context.setPacketHandled(true);
    }

    static boolean applyIfValid(ServerPlayer sender, NuclearReactorActionMessage message) {
        if (sender == null || message == null || sender.level().isClientSide
                || !sender.getAbilities().mayBuild
                || !sender.level().getWorldBorder().isWithinBounds(message.position)
                || !sender.level().hasChunk(message.position.getX() >> 4, message.position.getZ() >> 4)
                || sender.distanceToSqr(message.position.getX() + 0.5D,
                message.position.getY() + 0.5D, message.position.getZ() + 0.5D) > 64.0D
                || !(sender.containerMenu instanceof NuclearReactorMenu menu)
                || !menu.position().equals(message.position)
                || !menu.stillValid(sender)) {
            return false;
        }
        return menu.applyAction(sender, message.action);
    }

    private static <T> T requiredEnum(T[] values, int ordinal, String field) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Invalid reactor " + field + " " + ordinal);
        }
        return values[ordinal];
    }

    private static <T> T nullableEnum(T[] values, int ordinal, String field) {
        if (ordinal == -1) {
            return null;
        }
        return requiredEnum(values, ordinal, field);
    }
}
