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
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class ElectricPoleBlockEntity extends NetworkComponentBlockEntity
        implements LongDistanceEndpointHost, LongDistanceWireHost {
    private final boolean transformer;
    private final ElectricalNetworkModule electricity;
    private final LongDistanceEndpointModule longDistance;

    public ElectricPoleBlockEntity(BlockPos position, BlockState state) {
        super(
                state.getBlock() instanceof ElectricPoleBlock pole && pole.isTransformer()
                        ? ModBlockEntities.ELECTRIC_POLE_TRANSFORMER.get()
                        : ModBlockEntities.ELECTRIC_POLE.get(),
                position,
                state
        );
        transformer = state.getBlock() instanceof ElectricPoleBlock pole && pole.isTransformer();
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(transformer ? 0.5D : 0.25D, 125.0D, 0.001D),
                side -> false
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
        return transformer
                ? Set.of(LongDistancePort.POLE, LongDistancePort.CONNECTOR)
                : Set.of(LongDistancePort.POLE);
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        return Component.translatable(
                "message.magneticraft.long_distance_connections",
                longDistance.connectionCount()
        );
    }
}
