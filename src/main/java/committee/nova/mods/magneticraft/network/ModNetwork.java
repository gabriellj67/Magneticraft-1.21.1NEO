package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Narrow protocol for server-authoritative machine-menu actions.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(Magneticraft.id("machine"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static boolean registered;

    private ModNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(
                0,
                SetGhostFilterMessage.class,
                SetGhostFilterMessage::encode,
                SetGhostFilterMessage::decode,
                SetGhostFilterMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        registered = true;
    }

    public static void setGhostFilter(SetGhostFilterMessage message) {
        CHANNEL.sendToServer(message);
    }
}
