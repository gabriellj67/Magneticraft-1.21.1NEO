package committee.nova.mods.magneticraft.content.machine.windturbine;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Wind turbine controller that generates directly into its native electrical node.
 */
public final class WindTurbineBlockEntity extends NetworkComponentBlockEntity implements MenuProvider {
    public static final int MENU_LOGICAL_DATA_COUNT = 6;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    private final ElectricalPowerModule energy;
    private final ElectricalNetworkModule electricity;
    private final ItemInventoryModule inventory;
    private final WindTurbineModule wind;
    private final ContainerData menuData;

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
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("rotor_inventory"),
                this,
                1,
                (slot, stack) -> slot == 0 && WindTurbineRotorItem.tier(stack) != null,
                side -> new ItemInventoryModule.SlotAccess(new int[]{0}, new int[]{0})
        ));
        wind = addModule(new WindTurbineModule(
                Magneticraft.id("wind_turbine"),
                this,
                energy,
                this::facing,
                this::installedRotorTier
        ));
        menuData = Int32ContainerData.readOnly(
                energy::storedWholeJoules,
                energy::ratedCapacityWholeJoules,
                () -> (int) Math.round(wind.currentWind() * 1_000.0D),
                () -> (int) Math.round(wind.openSpace() * 1_000.0D),
                () -> (int) Math.round(wind.productionJoulesPerTick() * 10.0D),
                () -> wind.rotorTier() == null ? 0 : wind.rotorTier().ordinal() + 1
        );
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

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public Direction facing() {
        return getBlockState().getValue(WindTurbineBlock.FACING);
    }

    @Override
    protected void tickComponent() {
        syncClientState(wind.clientStateHash());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WindTurbineMenu(containerId, playerInventory, this);
    }

    @Override
    public void dropContents(net.minecraft.world.level.Level level) {
        for (int slot = 0; slot < inventory.slots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                inventory.setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        int radius = WindTurbineRotorTier.LARGE.bladeRadius() + 2;
        return new AABB(worldPosition).inflate(radius, radius, radius);
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

    @Nullable
    private WindTurbineRotorTier installedRotorTier() {
        return WindTurbineRotorItem.tier(inventory.getStackInSlot(0));
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
