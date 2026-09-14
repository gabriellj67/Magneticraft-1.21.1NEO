package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class SpentFuelPoolMenu extends AbstractMachineMenu {
    public static final int IMAGE_WIDTH = 300;
    public static final int IMAGE_HEIGHT = 205;
    public static final int PLAYER_TOP = 123;
    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public SpentFuelPoolMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null);
    }

    public SpentFuelPoolMenu(int id, Inventory inventory, SpentFuelPoolControllerBlockEntity controller) {
        this(id, inventory, controller.getBlockPos(), controller);
    }

    private SpentFuelPoolMenu(int id, Inventory playerInventory, BlockPos position,
                              SpentFuelPoolControllerBlockEntity controller) {
        super(ModMenus.SPENT_FUEL_POOL.get(), id);
        this.position = position.immutable();
        access = ContainerLevelAccess.create(playerInventory.player.level(), position);
        data = controller == null ? new SimpleContainerData(SpentFuelPoolControllerBlockEntity.MENU_DATA_COUNT)
                : controller.menuData();
        addDataSlots(data);
        IItemHandler items = controller == null
                ? new ItemStackHandler(SpentFuelPoolControllerBlockEntity.SLOTS)
                : controller.inventory().menuHandler();
        for (int slot = 0; slot < SpentFuelPoolControllerBlockEntity.SLOTS; slot++) {
            int x = 26 + slot % 4 * 18;
            int y = 34 + slot / 4 * 18;
            addSlot(new SlotItemHandler(items, slot, x, y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return controller == null || controller.isSpentFuel(stack);
                }
                @Override public boolean mayPickup(Player player) {
                    return controller == null || controller.transferable(getItem());
                }
            });
        }
        finishMachineSlots(playerInventory, 26, PLAYER_TOP);
    }

    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        return access.evaluate((level, pos) ->
                level.getBlockEntity(pos) instanceof SpentFuelPoolControllerBlockEntity controller
                        && controller.formed() && controller.canManage(player), false);
    }

    @Override protected boolean movePlayerStackToMachine(ItemStack stack) {
        return moveItemStackTo(stack, 0, SpentFuelPoolControllerBlockEntity.SLOTS, false);
    }

    public BlockPos position() { return position; }
    public boolean formed() { return value(0) != 0; }
    public Direction facing() { return Direction.from2DDataValue(value(1)); }
    public int width() { return value(2); }
    public int length() { return value(3); }
    public int height() { return value(4); }
    public boolean portPresent() { return value(5) != 0; }
    public boolean cooling() { return value(6) != 0; }
    public int fuelCount() { return value(7); }
    public int safeAssemblies() { return value(8); }
    public int waterBlocks() { return value(9); }
    public int releasedHeat() { return value(10); }
    public double portTemperatureKelvin() { return value(11) / 10.0D; }
    public double doseRateMillisievertsPerHour() { return value(12) / 1000.0D; }
    public int transferableAssemblies() { return value(13); }
    public SpentFuelHandlingStatus handlingStatus() {
        return SpentFuelHandlingStatus.from(
                fuelCount(), transferableAssemblies(), safeAssemblies(), cooling());
    }
    private int value(int logical) { return Int32ContainerData.read(data, logical); }
}
