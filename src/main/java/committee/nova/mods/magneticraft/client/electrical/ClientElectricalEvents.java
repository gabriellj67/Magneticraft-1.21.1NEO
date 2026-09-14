package committee.nova.mods.magneticraft.client.electrical;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModelRefreshQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Clears server-specific display data when the client leaves a connection. */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientElectricalEvents {
    private ClientElectricalEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientVoltageTierRegistry.reset();
        MachineModelRefreshQueue.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        for (MachineModelRefreshQueue.Request request : MachineModelRefreshQueue.tickAndDrain()) {
            if (level == null || !level.dimension().location().equals(request.dimension())) {
                continue;
            }
            if (level.getBlockEntity(request.position()) instanceof MachineBlockEntity machine) {
                machine.requestModelDataUpdate();
            }
            BlockState state = level.getBlockState(request.position());
            level.sendBlockUpdated(
                    request.position(),
                    state,
                    state,
                    Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE
            );
        }
    }
}
