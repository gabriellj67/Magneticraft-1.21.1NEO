package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostFilterMenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

/**
 * C2S intent for setting one non-consuming item-filter sample.
 */
public record SetGhostFilterMessage(BlockPos position, int slot, ItemStack sample) implements CustomPacketPayload {
    public static final Type<SetGhostFilterMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Magneticraft.MOD_ID, "set_ghost_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGhostFilterMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetGhostFilterMessage::position,
            ByteBufCodecs.VAR_INT, SetGhostFilterMessage::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, SetGhostFilterMessage::sample,
            SetGhostFilterMessage::new
    );

    public SetGhostFilterMessage {
        position = Objects.requireNonNull(position, "position").immutable();
        sample = normalize(sample);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetGhostFilterMessage message, IPayloadContext context) {
        context.enqueueWork(() -> applyIfValid(context.player(), message));
    }

    public static boolean applyIfValid(Player sender, SetGhostFilterMessage message) {
        if (sender == null
                || message == null
                || sender.level().isClientSide
                || !sender.level().getWorldBorder().isWithinBounds(message.position)
                || !(sender.containerMenu instanceof GhostFilterMenuAccess menu)) {
            return false;
        }
        if (!menu.machinePosition().equals(message.position)
                || message.slot < 0
                || message.slot >= menu.ghostFilterCount()
                || !sender.getAbilities().mayBuild
                || !sender.level().hasChunk(message.position.getX() >> 4, message.position.getZ() >> 4)
                || sender.distanceToSqr(
                        message.position.getX() + 0.5D,
                        message.position.getY() + 0.5D,
                        message.position.getZ() + 0.5D
                ) > 64.0D
                || !menu.stillValid(sender)) {
            return false;
        }
        menu.setGhostFilter(message.slot, message.sample);
        return true;
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
