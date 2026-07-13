package committee.nova.mods.magneticraft.content.machine.windturbine;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalEnergyBridgeModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wind turbine controller with an internal buffer and native electrical output.
 */
public final class WindTurbineBlockEntity extends NetworkComponentBlockEntity {
    public static final int ENERGY_CAPACITY_JOULES = 80_000;
    public static final int MAX_TRANSFER_JOULES_PER_TICK = 200;

    private static final double NODE_CAPACITANCE = 1.0D;
    private static final double NODE_MAX_VOLTAGE = 125.0D;
    private static final double NODE_RESISTANCE = 0.001D;
    private static final double BRIDGE_CHARGE_THRESHOLD_VOLTS = 120.0D;
    private static final double BRIDGE_DISCHARGE_THRESHOLD_VOLTS = 115.0D;

    private final EnergyStorageModule energy;
    private final ElectricalNetworkModule electricity;
    private final WindTurbineModule wind;

    public WindTurbineBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE.get(), position, state);
        energy = addModule(new EnergyStorageModule(
                Magneticraft.id("energy_storage"),
                this,
                ENERGY_CAPACITY_JOULES,
                MAX_TRANSFER_JOULES_PER_TICK,
                MAX_TRANSFER_JOULES_PER_TICK,
                side -> false,
                false,
                false
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(NODE_CAPACITANCE, NODE_MAX_VOLTAGE, NODE_RESISTANCE),
                this::canConnectElectricity
        ));
        addModule(new ElectricalEnergyBridgeModule(
                Magneticraft.id("electricity_bridge"),
                electricity,
                energy,
                BRIDGE_CHARGE_THRESHOLD_VOLTS,
                BRIDGE_DISCHARGE_THRESHOLD_VOLTS,
                MAX_TRANSFER_JOULES_PER_TICK
        ));
        wind = addModule(new WindTurbineModule(
                Magneticraft.id("wind_turbine"),
                this,
                electricity,
                this::facing
        ));
    }

    public EnergyStorageModule energy() {
        return energy;
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    public WindTurbineModule wind() {
        return wind;
    }

    public Direction facing() {
        return getBlockState().getValue(WindTurbineBlock.FACING);
    }

    @Override
    protected void tickComponent() {
        syncClientState(wind.clientStateHash());
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (!canConnectElectricity(side)) {
            return connectionMessage(side, false);
        }
        electricity.toggleSide(side);
        return connectionMessage(side, electricity.isSideEnabled(side));
    }

    private boolean canConnectElectricity(Direction side) {
        return side == facing().getOpposite();
    }

    private static Component connectionMessage(Direction side, boolean enabled) {
        return Component.translatable(
                enabled
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }
}
