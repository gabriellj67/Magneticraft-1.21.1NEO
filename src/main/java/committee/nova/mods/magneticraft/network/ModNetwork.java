package committee.nova.mods.magneticraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Narrow protocol for server-authoritative machine-menu actions.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "5";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(
                SetGhostFilterMessage.TYPE,
                SetGhostFilterMessage.STREAM_CODEC,
                SetGhostFilterMessage::handle
        );
        registrar.playToServer(
                UploadComputerProgramMessage.TYPE,
                UploadComputerProgramMessage.STREAM_CODEC,
                UploadComputerProgramMessage::handle
        );
        registrar.playToClient(
                SyncVoltageTiersMessage.TYPE,
                SyncVoltageTiersMessage.STREAM_CODEC,
                SyncVoltageTiersMessage::handle
        );
        registrar.playToServer(
                ElectricalDeviceActionMessage.TYPE,
                ElectricalDeviceActionMessage.STREAM_CODEC,
                ElectricalDeviceActionMessage::handle
        );
        registrar.playToServer(
                NuclearReactorActionMessage.TYPE,
                NuclearReactorActionMessage.STREAM_CODEC,
                NuclearReactorActionMessage::handle
        );
    }

    public static void setGhostFilter(SetGhostFilterMessage message) {
        PacketDistributor.sendToServer(message);
    }

    public static void uploadComputerProgram(UploadComputerProgramMessage message) {
        PacketDistributor.sendToServer(message);
    }

    public static void controlElectricalDevice(ElectricalDeviceActionMessage message) {
        PacketDistributor.sendToServer(message);
    }

    public static void syncVoltageTiers(ServerPlayer player, SyncVoltageTiersMessage message) {
        PacketDistributor.sendToPlayer(player, message);
    }

    public static void controlNuclearReactor(NuclearReactorActionMessage message) {
        PacketDistributor.sendToServer(message);
    }
}
