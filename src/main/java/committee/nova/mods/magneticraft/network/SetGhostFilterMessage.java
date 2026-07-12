package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostFilterMenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S intent for setting one non-consuming item-filter sample.
 */
public record SetGhostFilterMessage(BlockPos position, int slot, ItemStack sample) {
    public SetGhostFilterMessage {
        position = position.immutable();
        sample = normalize(sample);
    }

    public static void encode(SetGhostFilterMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.position);
        buffer.writeVarInt(message.slot);
        buffer.writeItem(message.sample);
    }

    public static SetGhostFilterMessage decode(FriendlyByteBuf buffer) {
        return new SetGhostFilterMessage(buffer.readBlockPos(), buffer.readVarInt(), buffer.readItem());
    }

    public static void handle(SetGhostFilterMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> applyIfValid(sender, message));
        context.setPacketHandled(true);
    }

    public static boolean applyIfValid(Player sender, SetGhostFilterMessage message) {
        if (sender == null || !(sender.containerMenu instanceof GhostFilterMenuAccess menu)) {
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
