package committee.nova.mods.magneticraft.content.machine.windturbine;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wind turbine controller that generates directly into its native electrical node.
 */
public final class WindTurbineBlockEntity extends NetworkComponentBlockEntity {
    private final ElectricalPowerModule energy;
    private final ElectricalNetworkModule electricity;
    private final WindTurbineModule wind;

    public WindTurbineBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE.get(), position, state);
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.MACHINE,
                this::canConnectElectricity
        ));
        energy = addModule(new ElectricalPowerModule(
                Magneticraft.id("energy_storage"),
                Magneticraft.id("wind_turbine"),
                this,
                electricity,
                ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false,
                false
        ));
        wind = addModule(new WindTurbineModule(
                Magneticraft.id("wind_turbine"),
                this,
                energy,
                this::facing
        ));
    }

    public ElectricalPowerModule energy() {
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
        return true;
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
