package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceConnection;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runtime registration and bounded client wire snapshot for one endpoint. */
public final class LongDistanceEndpointModule implements MachineModule {
    private static final String CONNECTIONS_TAG = "connections";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String PORT_TAG = "port";
    private static final int MAX_CLIENT_CONNECTIONS = 64;

    private final ResourceLocation id;
    private final LongDistanceEndpointHost host;
    private List<WireView> clientConnections = List.of();
    private LongDistanceElectricityService service;

    public LongDistanceEndpointModule(ResourceLocation id, LongDistanceEndpointHost host) {
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
            service.register(host);
        }
    }

    @Override
    public void onUnload() {
        if (service != null) {
            service.unregister(host);
            service = null;
        }
    }

    @Override
    public void serverTick() {
        if (service != null && host.level() instanceof ServerLevel level) {
            PhysicalNetworkService.manager(level).tick(level.getGameTime());
        }
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        if (service == null) {
            return;
        }
        ListTag connections = new ListTag();
        List<LongDistanceConnection> all = service.connectionsAt(host.position());
        for (int index = 0; index < Math.min(all.size(), MAX_CLIENT_CONNECTIONS); index++) {
            LongDistanceConnection connection = all.get(index);
            boolean first = connection.first().position().equals(host.position());
            BlockPos remote = first ? connection.second().position() : connection.first().position();
            LongDistancePort port = first ? connection.first().port() : connection.second().port();
            CompoundTag connectionTag = new CompoundTag();
            connectionTag.putInt(X_TAG, remote.getX());
            connectionTag.putInt(Y_TAG, remote.getY());
            connectionTag.putInt(Z_TAG, remote.getZ());
            connectionTag.putString(PORT_TAG, port.serializedName());
            connections.add(connectionTag);
        }
        tag.put(CONNECTIONS_TAG, connections);
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        if (!tag.contains(CONNECTIONS_TAG, Tag.TAG_LIST)) {
            clientConnections = List.of();
            return;
        }
        ListTag connections = tag.getList(CONNECTIONS_TAG, Tag.TAG_COMPOUND);
        List<WireView> loaded = new ArrayList<>(Math.min(connections.size(), MAX_CLIENT_CONNECTIONS));
        for (int index = 0; index < Math.min(connections.size(), MAX_CLIENT_CONNECTIONS); index++) {
            CompoundTag connection = connections.getCompound(index);
            LongDistancePort.byName(connection.getString(PORT_TAG)).ifPresent(port -> loaded.add(new WireView(
                    new BlockPos(connection.getInt(X_TAG), connection.getInt(Y_TAG), connection.getInt(Z_TAG)),
                    port
            )));
        }
        clientConnections = List.copyOf(loaded);
    }

    public List<WireView> clientConnections() {
        return clientConnections;
    }

    public int connectionCount() {
        return service == null
                ? clientConnections.size()
                : service.connectionsAt(host.position()).size();
    }

    public record WireView(BlockPos remotePosition, LongDistancePort port) {
        public WireView {
            remotePosition = remotePosition.immutable();
        }
    }
}
