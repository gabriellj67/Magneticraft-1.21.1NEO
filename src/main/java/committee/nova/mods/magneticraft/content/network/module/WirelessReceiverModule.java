package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.longdistance.WirelessEnergyReceiverHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/** Registers a loaded wireless receiver without persisting runtime objects. */
public final class WirelessReceiverModule implements MachineModule {
    private final ResourceLocation id;
    private final WirelessEnergyReceiverHost host;
    private LongDistanceElectricityService service;

    public WirelessReceiverModule(ResourceLocation id, WirelessEnergyReceiverHost host) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void onLoad() {
        if (host.level() instanceof ServerLevel level) {
            service = LongDistanceElectricityService.get(level);
            service.registerReceiver(host);
        }
    }

    @Override
    public void onUnload() {
        if (service != null) {
            service.unregisterReceiver(host);
            service = null;
        }
    }
}
