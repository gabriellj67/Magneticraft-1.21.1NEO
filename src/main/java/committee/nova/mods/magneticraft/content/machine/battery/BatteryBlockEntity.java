package committee.nova.mods.magneticraft.content.machine.battery;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalEnergyBridgeModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
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
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * One-million-FE battery with two portable-cell transfer slots.
 */
public final class BatteryBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int CAPACITY = 1_000_000;
    public static final int ITEM_TRANSFER_RATE = 500;
    private static final int NETWORK_TRANSFER_RATE = 640;

    private final ItemInventoryModule inventory;
    private final EnergyStorageModule energy;
    private final ElectricalNetworkModule electricity;
    private final ContainerData data;

    public BatteryBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"),
                this,
                2,
                BatteryBlockEntity::isValidCell,
                side -> new ItemInventoryModule.SlotAccess(new int[]{0, 1}, new int[]{0, 1})
        ));
        energy = addModule(new EnergyStorageModule(
                Magneticraft.id("energy_storage"),
                this,
                CAPACITY,
                NETWORK_TRANSFER_RATE,
                NETWORK_TRANSFER_RATE,
                this::canAccessEnergy
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(0.5D, 125.0D, 0.001D),
                0.001D,
                8.0D,
                this::canAccessEnergy
        ));
        addModule(new ElectricalEnergyBridgeModule(
                Magneticraft.id("electricity_bridge"),
                this,
                electricity,
                energy,
                90.0D,
                90.0D,
                NETWORK_TRANSFER_RATE,
                true
        ));
        data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energy.getEnergyStored();
                    case 1 -> energy.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    energy.setEnergyStored(value);
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, BatteryBlockEntity battery) {
        battery.tickModules();
        int charged = battery.chargeItem(battery.inventory.getStackInSlot(0));
        int discharged = battery.dischargeItem(battery.inventory.getStackInSlot(1));
        if (charged > 0 || discharged > 0) {
            battery.markChanged();
        }
    }

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public EnergyStorageModule energy() {
        return energy;
    }

    public ElectricalNetworkModule electricity() {
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
        return Component.translatable("container.magneticraft.battery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryMenu(containerId, playerInventory, this);
    }

    private int chargeItem(ItemStack stack) {
        if (stack.isEmpty() || energy.getEnergyStored() <= 0) {
            return 0;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY).map(itemEnergy -> {
            int offered = energy.extractEnergy(ITEM_TRANSFER_RATE, true);
            int accepted = itemEnergy.receiveEnergy(offered, false);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
            }
            return accepted;
        }).orElse(0);
    }

    private int dischargeItem(ItemStack stack) {
        if (stack.isEmpty() || energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            return 0;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY).map(itemEnergy -> {
            int requested = energy.receiveEnergy(ITEM_TRANSFER_RATE, true);
            int extracted = itemEnergy.extractEnergy(requested, false);
            if (extracted > 0) {
                energy.receiveEnergy(extracted, false);
            }
            return extracted;
        }).orElse(0);
    }

    private boolean canAccessEnergy(@Nullable Direction side) {
        if (side == null || side.getAxis() == Direction.Axis.Y) {
            return true;
        }
        return getBlockState().hasProperty(BatteryBlock.FACING)
                && side == getBlockState().getValue(BatteryBlock.FACING).getOpposite();
    }

    private static boolean isValidCell(int slot, ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(storage -> switch (slot) {
            case 0 -> storage.canReceive();
            case 1 -> storage.canExtract();
            default -> false;
        }).orElse(false);
    }
}
