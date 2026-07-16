package committee.nova.mods.magneticraft.content.network.pressure;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.PressureNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PressureTankBlockEntity extends NetworkComponentBlockEntity implements MenuProvider {
    public static final double VOLUME_LITERS = 16.0D;
    public static final double MAX_PRESSURE_KPA = 20_000.0D;
    public static final int MENU_DATA_COUNT = 6;

    private final PressureNetworkModule pressure;
    private final ContainerData data;

    public PressureTankBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.PRESSURE_TANK.get(), position, state);
        pressure = addModule(new PressureNetworkModule(
                Magneticraft.id("pressure"),
                this,
                new PressureNode(VOLUME_LITERS, MAX_PRESSURE_KPA),
                BrassPressurePipeBlockEntity.CONDUCTANCE,
                BrassPressurePipeBlockEntity.MAX_GAS_PER_TICK,
                0.0D,
                side -> true,
                true
        ));
        data = Int32ContainerData.readOnly(
                () -> scaled(pressure.node().pressureKpa()),
                () -> scaled(pressure.node().gasKpaLiters()),
                () -> scaled(pressure.node().capacityKpaLiters())
        );
    }

    public PressureNetworkModule pressure() {
        return pressure;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    protected void tickComponent() {
        syncClientState(31 * pressure.node().gasId().hashCode()
                + Double.hashCode(pressure.node().gasKpaLiters()));
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (secondaryAction) {
            pressure.cycleRedstoneMode();
            return Component.translatable(
                    "message.magneticraft.redstone_mode",
                    Component.translatable("message.magneticraft.redstone_mode."
                            + pressure.redstoneMode().name().toLowerCase())
            );
        }
        pressure.toggleSide(side);
        return Component.translatable(
                pressure.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magneticraft.pressure_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PressureTankMenu(containerId, inventory, this);
    }

    private static int scaled(double value) {
        return (int) Math.round(Math.min(Integer.MAX_VALUE, Math.max(0.0D, value * 1_000.0D)));
    }
}
