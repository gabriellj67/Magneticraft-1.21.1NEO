package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceWireHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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
                new ElectricalNode(0.25D, 125.0D, 0.001D),
                side -> side == backSide()
        ));
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

    @Override
    protected void tickComponent() {
        if (level instanceof ServerLevel serverLevel
                && ElectricEnergyExporter.export(serverLevel, worldPosition, outwardFacing(), electricity.node()) > 0) {
            markChanged();
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
