package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceWireHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

public final class ElectricConnectorBlockEntity extends NetworkComponentBlockEntity
        implements LongDistanceEndpointHost, LongDistanceWireHost {
    private final ElectricalNetworkModule electricity;
    private final LongDistanceEndpointModule longDistance;

    public ElectricConnectorBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.ELECTRIC_CONNECTOR.get(), position, state);
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.CONDUCTOR,
                side -> side == backSide() || side == outwardFacing()
        ));
        electricity.useOverheadDamageProfile();
        longDistance = addModule(new LongDistanceEndpointModule(
                Magneticraft.id("long_distance_endpoint"),
                this
        ));
    }

    @Override
    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Override
    public LongDistanceEndpointModule longDistance() {
        return longDistance;
    }

    @Override
    public Set<LongDistancePort> longDistancePorts() {
        return Set.of(LongDistancePort.CONNECTOR);
    }

    public static AABB renderBounds(BlockPos position, List<LongDistanceEndpointModule.WireView> connections) {
        double minX = position.getX();
        double minY = position.getY();
        double minZ = position.getZ();
        double maxX = position.getX() + 1.0D;
        double maxY = position.getY() + 1.0D;
        double maxZ = position.getZ() + 1.0D;
        for (LongDistanceEndpointModule.WireView connection : connections) {
            BlockPos remote = connection.remotePosition();
            minX = Math.min(minX, remote.getX());
            minY = Math.min(minY, Math.min(position.getY(), remote.getY()) - 2.0D);
            minZ = Math.min(minZ, remote.getZ());
            maxX = Math.max(maxX, remote.getX() + 1.0D);
            maxY = Math.max(maxY, remote.getY() + 1.0D);
            maxZ = Math.max(maxZ, remote.getZ() + 1.0D);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.25D);
    }

    @Override
    protected void tickComponent() {
        if (level instanceof ServerLevel serverLevel
                && electricity.electricalProfileBound()
                && ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(electricity.tierId()))
                .map(tier -> ElectricEnergyExporter.export(
                        serverLevel,
                        worldPosition,
                        outwardFacing(),
                        electricity.node(),
                        tier,
                        (int) Math.floor(tier.connectorConversionJoulesPerTick())
                ))
                .orElse(0) > 0) {
            markChanged();
        }
    }

    @Override
    protected void tickElectricalFault() {
        if (level instanceof ServerLevel serverLevel) {
            committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService
                    .get(serverLevel)
                    .removeConnectionsAt(worldPosition);
        }
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        return Component.translatable(
                "message.magneticraft.long_distance_connections",
                longDistance.connectionCount()
        );
    }

    private Direction outwardFacing() {
        return getBlockState().hasProperty(WallMountedElectricBlock.FACING)
                ? getBlockState().getValue(WallMountedElectricBlock.FACING)
                : Direction.NORTH;
    }

    private Direction backSide() {
        return outwardFacing().getOpposite();
    }
}
