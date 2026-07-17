package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Narrow protocol for server-authoritative machine-menu actions.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "5";
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
        CHANNEL.registerMessage(
                1,
                UploadComputerProgramMessage.class,
                UploadComputerProgramMessage::encode,
                UploadComputerProgramMessage::decode,
                UploadComputerProgramMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                2,
                SyncVoltageTiersMessage.class,
                SyncVoltageTiersMessage::encode,
                SyncVoltageTiersMessage::decode,
                SyncVoltageTiersMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                3,
                ElectricalDeviceActionMessage.class,
                ElectricalDeviceActionMessage::encode,
                ElectricalDeviceActionMessage::decode,
                ElectricalDeviceActionMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                4,
                NuclearReactorActionMessage.class,
                NuclearReactorActionMessage::encode,
                NuclearReactorActionMessage::decode,
                NuclearReactorActionMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        registered = true;
    }

    public static void setGhostFilter(SetGhostFilterMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void uploadComputerProgram(UploadComputerProgramMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void controlElectricalDevice(ElectricalDeviceActionMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void syncVoltageTiers(ServerPlayer player, SyncVoltageTiersMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void controlNuclearReactor(NuclearReactorActionMessage message) {
        CHANNEL.sendToServer(message);
    }
}
