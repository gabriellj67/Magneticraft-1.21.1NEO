package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.WirelessReceiverModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.longdistance.WirelessEnergyReceiverHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class WirelessEnergyReceiverBlockEntity extends NetworkComponentBlockEntity
        implements WirelessEnergyReceiverHost {
    private final ElectricalNetworkModule electricity;

    public WirelessEnergyReceiverBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.WIRELESS_ENERGY_RECEIVER.get(), position, state);
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.MACHINE,
                side -> side == backSide()
        ));
        addModule(new WirelessReceiverModule(Magneticraft.id("wireless_receiver"), this));
    }

    @Override
    public ElectricalNetworkModule electricity() {
        return electricity;
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
                "message.magneticraft.voltage",
                String.format(java.util.Locale.ROOT, "%.2f", electricity.node().voltage())
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
