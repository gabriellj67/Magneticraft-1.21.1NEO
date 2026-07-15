package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceWireHost;
import committee.nova.mods.magneticraft.content.network.module.TransformerCouplerModule;
import committee.nova.mods.magneticraft.content.network.module.TransformerElectricalHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistancePort;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.List;

public final class ElectricPoleBlockEntity extends NetworkComponentBlockEntity
        implements LongDistanceEndpointHost, LongDistanceWireHost, TransformerElectricalHost {
    public static final ResourceLocation INPUT_TERMINAL = Magneticraft.id("transformer_input");
    public static final ResourceLocation OUTPUT_TERMINAL = Magneticraft.id("transformer_output");
    private final boolean transformer;
    private final ElectricalNetworkModule electricity;
    private final ElectricalNetworkModule transformedElectricity;
    private final TransformerCouplerModule transformerCoupler;
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
        electricity = transformer
                ? addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_input"),
                this,
                ElectricalNetworkModule.LOW_VOLTAGE,
                INPUT_TERMINAL,
                ElectricalNodeKind.CONDUCTOR,
                side -> false
        ))
                : addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.CONDUCTOR,
                side -> false
        ));
        transformedElectricity = transformer
                ? addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_output"),
                this,
                Magneticraft.id("medium_voltage"),
                OUTPUT_TERMINAL,
                ElectricalNodeKind.CONDUCTOR,
                side -> false
        ))
                : null;
        electricity.useOverheadDamageProfile();
        if (transformedElectricity != null) {
            transformedElectricity.useOverheadDamageProfile();
        }
        transformerCoupler = transformer
                ? addModule(new TransformerCouplerModule(
                Magneticraft.id("transformer_coupler"),
                this,
                electricity,
                transformedElectricity,
                TransformerProfileIds.LV_TO_MV
        ))
                : null;
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
    public ElectricalNetworkModule electricity(LongDistancePort port) {
        if (!transformer) {
            return LongDistanceEndpointHost.super.electricity(port);
        }
        return switch (port) {
            case CONNECTOR -> electricity;
            case POLE -> transformedElectricity;
        };
    }

    @Override
    public java.util.Optional<committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpoint>
    longDistanceEndpointForInteraction(Direction clickedFace) {
        if (!transformer) {
            return LongDistanceEndpointHost.super.longDistanceEndpointForInteraction(clickedFace);
        }
        return longDistanceEndpoint(clickedFace.getAxis() == Direction.Axis.Y
                ? LongDistancePort.POLE
                : LongDistancePort.CONNECTOR);
    }

    @Override
    public TransformerCouplerModule transformerCoupler() {
        if (transformerCoupler == null) {
            throw new IllegalStateException("Ordinary electric pole has no transformer coupler");
        }
        return transformerCoupler;
    }

    @Override
    public LongDistanceEndpointModule longDistance() {
        return longDistance;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return renderBounds(worldPosition, longDistance.clientConnections());
    }

    static AABB renderBounds(BlockPos position, List<LongDistanceEndpointModule.WireView> connections) {
        AABB bounds = new AABB(
                position.getX() - 0.875D,
                position.getY() - 4.0D,
                position.getZ() - 0.875D,
                position.getX() + 1.875D,
                position.getY() + 1.0D,
                position.getZ() + 1.875D
        );
        for (LongDistanceEndpointModule.WireView connection : connections) {
            BlockPos remote = connection.remotePosition();
            AABB wire = new AABB(
                    Math.min(position.getX(), remote.getX()) + 0.25D,
                    Math.min(position.getY(), remote.getY()) - 1.75D,
                    Math.min(position.getZ(), remote.getZ()) + 0.25D,
                    Math.max(position.getX(), remote.getX()) + 0.75D,
                    Math.max(position.getY(), remote.getY()) + 0.75D,
                    Math.max(position.getZ(), remote.getZ()) + 0.75D
            );
            bounds = new AABB(
                    Math.min(bounds.minX, wire.minX),
                    Math.min(bounds.minY, wire.minY),
                    Math.min(bounds.minZ, wire.minZ),
                    Math.max(bounds.maxX, wire.maxX),
                    Math.max(bounds.maxY, wire.maxY),
                    Math.max(bounds.maxZ, wire.maxZ)
            );
        }
        return bounds;
    }

    @Override
    public Set<LongDistancePort> longDistancePorts() {
        return transformer
                ? Set.of(LongDistancePort.POLE, LongDistancePort.CONNECTOR)
                : Set.of(LongDistancePort.POLE);
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
        if (transformerCoupler != null) {
            if (secondaryAction) {
                transformerCoupler.tryReverse();
            } else {
                transformerCoupler.cycleRedstoneMode();
            }
            return Component.translatable(
                    "message.magneticraft.transformer_status",
                    transformerCoupler.profileId(),
                    transformerCoupler.reversed(),
                    transformerCoupler.redstoneMode().name()
            );
        }
        return Component.translatable(
                "message.magneticraft.long_distance_connections",
                longDistance.connectionCount()
        );
    }
}
