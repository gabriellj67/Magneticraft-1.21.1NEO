package committee.nova.mods.magneticraft.content.machine.electricfurnace;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
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
 * Electric-furnace slots, progress and energy synchronization.
 */
public final class ElectricFurnaceMenu extends AbstractMachineMenu {
    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readBlockPos(), null);
    }

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, ElectricFurnaceBlockEntity furnace) {
        this(containerId, playerInventory, furnace.getBlockPos(), furnace);
    }

    private ElectricFurnaceMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            ElectricFurnaceBlockEntity knownFurnace
    ) {
        super(ModMenus.ELECTRIC_FURNACE.get(), containerId);
        this.position = position.immutable();

        ElectricFurnaceBlockEntity furnace = knownFurnace;
        if (furnace == null) {
            BlockEntity candidate = playerInventory.player.level().getBlockEntity(position);
            if (candidate instanceof ElectricFurnaceBlockEntity loadedFurnace) {
                furnace = loadedFurnace;
            }
        }

        IItemHandler handler = furnace == null ? new ItemStackHandler(2) : furnace.inventory().menuHandler();
        data = furnace == null ? new SimpleContainerData(4) : furnace.data();
        access = furnace == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(playerInventory.player.level(), position);

        addSlot(new SlotItemHandler(handler, 0, 56, 35));
        addSlot(new SlotItemHandler(handler, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addDataSlots(data);
        finishMachineSlots(playerInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModMachineBlocks.ELECTRIC_FURNACE.get());
    }

    public BlockPos position() {
        return position;
    }

    public int energyStored() {
        return data.get(0);
    }

    public int energyCapacity() {
        return data.get(1);
    }

    public int progress() {
        return data.get(2);
    }

    public int totalProgress() {
        return data.get(3);
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return slots.get(0).mayPlace(stack) && moveItemStackTo(stack, 0, 1, false);
    }
}
