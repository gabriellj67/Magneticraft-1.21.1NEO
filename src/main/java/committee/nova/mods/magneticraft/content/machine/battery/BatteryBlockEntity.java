package committee.nova.mods.magneticraft.content.machine.battery;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

/**
 * Tiered native-joule battery with two explicit portable-FE conversion slots.
 */
public final class BatteryBlockEntity extends MachineBlockEntity implements MenuProvider, TieredElectricalHost {
    public static final int CAPACITY = 1_000_000;
    public static final int ITEM_TRANSFER_RATE = 500;
    public static final int MENU_DATA_COUNT = 4;

    private final ItemInventoryModule inventory;
    private final ElectricalPowerModule energy;
    private final ElectricalNetworkModule electricity;
    private final ContainerData data;

    public BatteryBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"),
                this,
                2,
                (slot, stack) -> true,
                side -> new ItemInventoryModule.SlotAccess(new int[]{0, 1}, new int[]{0, 1})
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.MACHINE,
                this::canAccessEnergy
        ));
        energy = addModule(new ElectricalPowerModule(
                Magneticraft.id("energy_storage"),
                Magneticraft.id("battery_box"),
                this,
                electricity,
                ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false,
                true
        ));
        data = Int32ContainerData.readOnly(energy::storedWholeJoules, energy::ratedCapacityWholeJoules);
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, BatteryBlockEntity battery) {
        battery.tickModules();
        int charged = battery.electricalFaulted() ? 0 : battery.chargeItem(battery.inventory.getStackInSlot(0));
        int discharged = battery.electricalFaulted() ? 0 : battery.dischargeItem(battery.inventory.getStackInSlot(1));
        if (charged > 0 || discharged > 0) {
            battery.markChanged();
        }
        battery.finishServerTick();
    }

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public ElectricalPowerModule energy() {
        return energy;
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Override
    public ElectricalNetworkModule tieredElectricalModule() {
        return electricity;
    }

    public ContainerData data() {
        return data;
    }

    public void dropContents(Level level) {
        for (int slot = 0; slot < inventory.slots(); slot++) {
            ItemStack stack = inventory.extractInternal(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack
                );
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magneticraft.battery_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryMenu(containerId, playerInventory, this);
    }

    private int chargeItem(ItemStack stack) {
        if (stack.isEmpty() || energy.storedWholeJoules() <= 0) {
            return 0;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY).map(itemEnergy -> {
            int offered = (int) Math.floor(energy.withdrawJoules(ITEM_TRANSFER_RATE, true));
            int accepted = itemEnergy.receiveEnergy(offered, true);
            int inserted = itemEnergy.receiveEnergy(accepted, false);
            if (inserted > 0) {
                energy.withdrawJoules(inserted, false);
            }
            return inserted;
        }).orElse(0);
    }

    private int dischargeItem(ItemStack stack) {
        if (stack.isEmpty() || energy.storedJoules() >= energy.ratedCapacityJoules()) {
            return 0;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY).map(itemEnergy -> {
            int requested = (int) Math.floor(energy.storeJoules(ITEM_TRANSFER_RATE, true));
            int available = itemEnergy.extractEnergy(requested, true);
            int extracted = itemEnergy.extractEnergy(available, false);
            int insertedWhole = 0;
            if (extracted > 0) {
                double inserted = energy.storeJoules(extracted, false);
                insertedWhole = (int) Math.floor(inserted);
                int remainder = extracted - insertedWhole;
                if (remainder > 0) {
                    itemEnergy.receiveEnergy(remainder, false);
                }
            }
            return insertedWhole;
        }).orElse(0);
    }

    private boolean canAccessEnergy(@Nullable Direction side) {
        if (side == null || side.getAxis() == Direction.Axis.Y) {
            return true;
        }
        return getBlockState().hasProperty(BatteryBlock.FACING)
                && side == getBlockState().getValue(BatteryBlock.FACING).getOpposite();
    }

    static boolean isValidCell(int slot, ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(storage -> switch (slot) {
            case 0 -> storage.canReceive();
            case 1 -> storage.canExtract();
            default -> false;
        }).orElse(false);
    }
}
