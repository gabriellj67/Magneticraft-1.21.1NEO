package committee.nova.mods.magneticraft.content.machine.battery;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Battery inventory and energy synchronization contract.
 */
public final class BatteryMenu extends AbstractMachineMenu {
    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public BatteryMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos(), null);
    }

    public BatteryMenu(int containerId, Inventory playerInventory, BatteryBlockEntity battery) {
        this(containerId, playerInventory, battery.getBlockPos(), battery);
    }

    private BatteryMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            BatteryBlockEntity knownBattery
    ) {
        super(ModMenus.BATTERY.get(), containerId);
        this.position = position.immutable();

        BatteryBlockEntity battery = knownBattery;
        if (battery == null) {
            BlockEntity candidate = playerInventory.player.level().getBlockEntity(position);
            if (candidate instanceof BatteryBlockEntity loadedBattery) {
                battery = loadedBattery;
            }
        }

        IItemHandler handler = battery == null ? new ItemStackHandler(2) : battery.inventory().menuHandler();
        data = knownBattery == null ? new SimpleContainerData(BatteryBlockEntity.MENU_DATA_COUNT) : knownBattery.data();
        access = battery == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(playerInventory.player.level(), position);

        addSlot(filteredCellSlot(handler, 0, 53, 35));
        addSlot(filteredCellSlot(handler, 1, 107, 35));
        addDataSlots(data);
        finishMachineSlots(playerInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModMachineBlocks.BATTERY.get());
    }

    public BlockPos position() {
        return position;
    }

    public int energyStored() {
        return Int32ContainerData.read(data, 0);
    }

    public int energyCapacity() {
        return Int32ContainerData.read(data, 1);
    }

    private static SlotItemHandler filteredCellSlot(IItemHandler handler, int slot, int x, int y) {
        return new SlotItemHandler(handler, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BatteryBlockEntity.isValidCell(slot, stack);
            }
        };
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        if (slots.get(0).mayPlace(stack) && moveItemStackTo(stack, 0, 1, false)) {
            return true;
        }
        return slots.get(1).mayPlace(stack) && moveItemStackTo(stack, 1, 2, false);
    }
}
